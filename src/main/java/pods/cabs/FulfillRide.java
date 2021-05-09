package pods.cabs;

import akka.actor.ActorSelection;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.pattern.Patterns;

import java.time.Duration;
import java.util.HashMap;
import java.util.regex.Pattern;

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
                .onMessage(RideEnded.class, this::onRideEnded)
                .build();
    }

    /**
     * This behavior is called when ride is ended from the cab actor
     * @param command to fulfill ride actor to end the ride and update the cab status to ride actors
     * @return behavior of the fulfill ride actor
     */
    public Behavior<Command> onRideEnded(RideEnded command) {

        Globals.rideService.get(0).tell(new RideService.UpdateCabStatus(this.cabAssigned, command.cabStatus));
        return Behaviors.stopped();
    }

    public Behavior<Command> onResponseBalance(WrappedResponseBalance command) {

        return this;
    }


    /**
     * It implements the logic of finding nearest 3 cabs and querying each cab for the ride
     * @param command to Fulfill ride actor for requesting ride
     * @return Fulfill ride behavior
     * @throws InterruptedException the exception is thrown if thread sleep fails
     */
    public Behavior<Command> onRequestRide(RequestRide command) throws InterruptedException {

        // finding the nearest 3 cabs
        int[] distance = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
        String[] cabId = {"", "", ""};
        for(String cab: this.cabs.keySet()) {
            CabStatus status = this.cabs.get(cab);
            int far = Math.abs(status.location - this.sourceLoc);
            for(int i = 0; i < 3; i++) {
                if(far < distance[i]) {
                    swap(distance, cabId, cab, far, i);
                    break;
                }
                else if(far == distance[i]) {
                    // If actor distance is same to previous cabs then select the cab which is available
                    if(this.cabs.get(cabId[i]).minorState != MinorState.Available || !this.cabs.get(cabId[i]).interested) {
                        swap(distance, cabId, cab, far, i);
                        break;
                    }
                }
            }
        }

        for(int i = 0; i < 3; i++) {
            if(!cabId[i].equals("")) {

                CabStatus status = this.cabs.get(cabId[i]);
                if(this.rideId == -1 && status.interested && status.minorState == MinorState.Available) {
                    RideAssigned assign = new RideAssigned(false, cabId[i]);
                    Globals.cabs.get(cabId[i]).tell(new Cab.RequestRide(cabId[i], this.sourceLoc, this.destinationLoc, assign));
                    Thread.sleep(2000);
                    // If some cab is assigned then continue
                    if(assign.alloted) {
                        int fare = Math.abs(this.sourceLoc - this.destinationLoc) * 10 +
                                Math.abs(this.cabs.get(cabId[i]).location - this.sourceLoc) * 10;
                        ActorRef<Wallet.ResponseBalance> balRef = getContext().messageAdapter(Wallet.ResponseBalance.class,
                                WrappedResponseBalance::new);
                        Wallet.DeductBalance deduct = new Wallet.DeductBalance(fare, balRef);
                        Globals.wallets.get(this.custId).tell(deduct);
                        Thread.sleep(2000);

                        // If money deducted
                        if(deduct.toDeduct == -1) {
                            Cab.RideStarted start = new Cab.RideStarted(
                                    Globals.rideId + 1,
                                    cabId[i],
                                    this.sourceLoc,
                                    this.destinationLoc,
                                    this.rideId,
                                    getContext().getSelf()
                            );
                            Globals.cabs.get(cabId[i]).tell(start);
                            Thread.sleep(2000);
                            if(start.fulRideId != -1) {
                                this.rideId = start.fulRideId;
                                this.cabAssigned = cabId[i];
                                this.rideFare = fare;
                            }
                            else {
                                // Add the money back if ride not started
                                Globals.wallets.get(this.custId).tell(new Wallet.AddBalance(fare));
                                Thread.sleep(2000);
                            }
                        }
                        if(this.rideId == -1) {
                            Globals.cabs.get(cabId[i]).tell(new Cab.RideCancelled(cabId[i]));
                            Thread.sleep(2000);
                        }

                    }
                }
                else {
                    if(status.minorState == MinorState.Available) {
                        status.interested = true;
                    }
                }
            }
        }
        Thread.sleep(2000);
        if(this.rideId == -1) {

            this.ride.tell(new RideService.RideResponse(this.rideId, this.cabAssigned, this.rideFare, getContext().getSelf()));
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
