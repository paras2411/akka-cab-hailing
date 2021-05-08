package pods.cabs;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.HashMap;

public class FulfillRide extends AbstractBehavior<FulfillRide.Command> {

    HashMap<String, CabStatus> cabs;
    int sourceLoc;
    int destinationLoc;
    String custId;
    ActorRef<RideService.RideResponse> ride;
    int rideId;
    String cabAssigned;
    int rideFare;

    interface Command {}

    public static final class RideEnded implements Command {

        public CabStatus cabStatus;
        public RideEnded(CabStatus cabStatus) {
            this.cabStatus = cabStatus;
        }
    }

    public static final class RequestRide implements Command {

        public RequestRide() {

        }
    }

    public static final class RideAssigned implements Command {
        boolean alloted;
        String cabId;
        public RideAssigned(boolean alloted, String cabId) {
            this.alloted = alloted;
            this.cabId = cabId;
        }
    }

    public static final class CabStartedRide implements Command {
        int rideId;
        public CabStartedRide(int rideId) {
            this.rideId = rideId;
        }
    }

    public static final class RequestCab implements Command {

        public final String cabId;
        public RequestCab(String cabId) {
            this.cabId = cabId;
        }
    }

    public static class WrappedResponseBalance implements Command {
        final Wallet.ResponseBalance response;

        WrappedResponseBalance(Wallet.ResponseBalance response) {
            this.response = response;
        }
    }


    private FulfillRide(
            ActorContext<Command> context,
            HashMap<String, CabStatus> cabs,
            int sourceLoc,
            int destinationLoc,
            String custId,
            ActorRef<RideService.RideResponse> ride
    ) {
        super(context);
        this.cabs = cabs;
        this.sourceLoc = sourceLoc;
        this.destinationLoc = destinationLoc;
        this.custId = custId;
        this.ride = ride;
        this.rideId = -1;
    }

    public static Behavior<Command> create(
            HashMap<String, CabStatus> cabs,
            int sourceLoc,
            int destionationLoc,
            String custId,
            ActorRef<RideService.RideResponse> ride
    ) {
        return Behaviors.setup(context -> new FulfillRide(context, cabs, sourceLoc, destionationLoc, custId, ride));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(RequestRide.class, this::onRequestRide)
                .onMessage(WrappedResponseBalance.class, this::onResponseBalance)
                .onMessage(RequestCab.class, this::onRequestCab)
                .onMessage(RideAssigned.class, this::onRideAssigned)
                .onMessage(CabStartedRide.class, this::onCabStartedRide)
                .onMessage(RideEnded.class, this::onRideEnded)
                .build();
    }

    public Behavior<Command> onRideEnded(RideEnded command) {

        Globals.rideService.get(0).tell(new RideService.UpdateCabStatus(this.cabAssigned, command.cabStatus));
        return Behaviors.stopped();
    }

    public Behavior<Command> onCabStartedRide(CabStartedRide command) {

        this.rideId = command.rideId;
        return this;
    }

    public Behavior<Command> onRideAssigned(RideAssigned command) {

        if(command.alloted) {
            int fare = Math.abs(this.sourceLoc - this.destinationLoc) * 10 +
                    Math.abs(this.cabs.get(command.cabId).location - this.sourceLoc) * 10;
            ActorRef<Wallet.ResponseBalance> balRef = getContext().messageAdapter(Wallet.ResponseBalance.class,
                    WrappedResponseBalance::new);
            Globals.wallets.get(this.custId).tell(new Wallet.DeductBalance(fare, balRef));
        }
        return this;
    }

    public Behavior<Command> onResponseBalance(WrappedResponseBalance command) {

        if(command.response.deducted > 0) {
            Globals.cabs.get(command.response.cabId).tell(new Cab.RideStarted(
                    Globals.rideId + 1,
                    command.response.cabId,
                    this.sourceLoc,
                    this.destinationLoc,
                    getContext().getSelf())
            );
        }
        if(this.rideId == -1) {
            Globals.cabs.get(command.response.cabId).tell(new Cab.RideCancelled(command.response.cabId));
            Globals.wallets.get(this.custId).tell(new Wallet.AddBalance(command.response.deducted));
        }
        this.cabAssigned = command.response.cabId;
        this.rideFare = command.response.deducted;
        return this;
    }

    public Behavior<Command> onRequestCab(RequestCab command) {

        if(this.rideId != -1) return this;

        Globals.cabs.get(command.cabId).tell(new Cab.RequestRide(command.cabId, this.sourceLoc, this.destinationLoc, getContext().getSelf()));

        return this;
    }

    public Behavior<Command> onRequestRide(RequestRide command) {

        // finding the nearest 3 cabs
        int[] distance = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
        String[] cabId = {"", "", ""};
        for(String cab: this.cabs.keySet()) {
            CabStatus status = this.cabs.get(cab);
            int far = Math.abs(status.location - this.sourceLoc);
            for(int i = 0; i < 3; i++) {
                if(far < distance[i]) {
                    swap(distance, cabId, cab, far, i);
                }
                else if(far == distance[i]) {
                    if(this.cabs.get(cabId[i]).minorState != MinorState.Available || !this.cabs.get(cabId[i]).interested) {
                        swap(distance, cabId, cab, far, i);
                        break;
                    }
                }
            }
        }

        for(int i = 0; i < 3; i++) {
            if(!cabId[i].equals("")) {
                getContext().getSelf().tell(new RequestCab(cabId[i]));
            }
        }

        if(this.rideId == -1) {
            return Behaviors.stopped();
        }
        else {
            this.ride.tell(new RideService.RideResponse(this.rideId, this.cabAssigned, this.rideFare, getContext().getSelf()));
        }

        return this;
    }

    private void swap(int[] distance, String[] cabId, String cab, int far, int i) {
        for(int j = 2; j > i; j--) {
            distance[j] = distance[j - 1];
            cabId[j] = cabId[j - 1];
        }
        distance[i] = far;
        cabId[i] = cab;
    }


}
