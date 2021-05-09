package pods.cabs;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;

import javax.swing.*;

public class Cab extends AbstractBehavior<Cab.CabCommands> {

    public CabStatus cabStatus;
    public ActorRef<FulfillRide.Command> fRide;
    public int destinationLoc;
    public int numRides;

    public void initialize() {
        this.cabStatus = new CabStatus();
        this.numRides = 0;
        this.destinationLoc = this.cabStatus.location;
    }

    public Cab(ActorContext<CabCommands> context) {
        super(context);
        this.initialize();
    }

    public static Behavior<CabCommands> create() {
        return Behaviors.setup(Cab::new);
    }

    interface CabCommands {}

    public class InitializeCab implements CabCommands {
        public InitializeCab() {
            initialize();
        }
    }

    public static final class RequestRide implements CabCommands {
        public final int sourceLoc;
        public final int destinationLoc;
        public final ActorRef<FulfillRide.Command> replyTo;

        public RequestRide(String cabId, int sourceLoc, int destinationLoc, ActorRef<FulfillRide.Command> replyTo) {
            this.sourceLoc = sourceLoc;
            this.destinationLoc = destinationLoc;
            this.replyTo = replyTo;
        }
    }

    public static final class RideStarted implements CabCommands {
        public final int rideId;
        public final int sourceLoc;
        public final int destinationLoc;
        public final ActorRef<FulfillRide.Command> replyTo;

        public RideStarted(int rideId, String cabId, int sourceLoc, int destinationLoc, ActorRef<FulfillRide.Command> replyTo) {
            this.rideId = rideId;
            this.sourceLoc = sourceLoc;
            this.destinationLoc = destinationLoc;
            this.replyTo = replyTo;
        }
    }

    public static final class RideCancelled implements CabCommands {
        public RideCancelled(String cabId) {
        }
    }

    public static final class RideEnded implements CabCommands {
        public final int rideId;
        public RideEnded(int rideId) {
            this.rideId = rideId;
        }
    }

    public static final class SignIn implements CabCommands {
        public final int initialPos;

        public SignIn(int initialPos) {
            this.initialPos = initialPos;
        }
    }

    public static final class SignOut implements CabCommands {

        public SignOut() {

        }
    }

    public static final class NumRides implements  CabCommands {
        public final ActorRef<NumRidesResponse> replyTo;
        public NumRides(ActorRef<NumRidesResponse> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static final class Reset implements CabCommands {
        public final ActorRef<NumRidesResponse> replyTo;
        public Reset(ActorRef<NumRidesResponse> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static final class GetCabStatus {
        public final CabStatus status;
        public GetCabStatus(CabStatus status) {
            this.status = status;
        }
    }
    public static final class replyCabStatus implements CabCommands {
        public final ActorRef<GetCabStatus> replyTo;
        public replyCabStatus(ActorRef<GetCabStatus> replyTo) {
            this.replyTo = replyTo;
        }
    }



    public static final class NumRidesResponse {
        public int numRides;
        public NumRidesResponse(int numRides) {
            this.numRides = numRides;
        }
    }

    @Override
    public Receive<CabCommands> createReceive() {
        return newReceiveBuilder()
                .onMessage(NumRides.class, this::onNumRidesRequest)
                .onMessage(Reset.class, this::onReset)
                .onMessage(RideEnded.class, this::onRideEnded)
                .onMessage(SignIn.class, this::onSignIn)
                .onMessage(RequestRide.class, this::onRequestRide)
                .onMessage(RideStarted.class, this::onRideStarted)
                .onMessage(SignOut.class, this::onSignOut)
                .onMessage(RideCancelled.class, this::onRideCancelled)
                .onMessage(replyCabStatus.class, this::onGetCabStatus)
                .build();
    }

    public Behavior<CabCommands> onGetCabStatus(replyCabStatus command) {

        command.replyTo.tell(new GetCabStatus(this.cabStatus));
        return this;
    }

    public Behavior<CabCommands> onRideCancelled(RideCancelled command) {

        if(cabStatus.minorState == MinorState.Committed) {
            cabStatus.minorState = MinorState.Available;
            Globals.rideService.get(0).tell(new RideService.UpdateCabStatus(getContext().getSelf().path().name(), cabStatus));
        }
        return this;
    }

    public Behavior<CabCommands> onRideStarted(RideStarted command) {

        if(cabStatus.minorState == MinorState.Committed) {

            cabStatus.location = command.sourceLoc;
            cabStatus.minorState = MinorState.GivingRide;
            this.destinationLoc = command.destinationLoc;
            Globals.rideService.get(0).tell(new RideService.UpdateCabStatus(getContext().getSelf().path().name(), cabStatus));
            command.replyTo.tell(new FulfillRide.CabStartedRide(command.rideId));
            Globals.rideId++;
            this.numRides++;
        }
        else {
            command.replyTo.tell(new FulfillRide.CabStartedRide(-1));
        }

        return this;
    }

    public Behavior<CabCommands> onRequestRide(RequestRide command) {

        if(command.sourceLoc < 0 || command.destinationLoc < 0) {
            command.replyTo.tell(new FulfillRide.RideAssigned(false, getContext().getSelf().path().name()));
        }
        else if(cabStatus.minorState == MinorState.Available) {
            boolean rideAssigned = false;
            if(cabStatus.interested) {
                rideAssigned = true;
                cabStatus.interested = false;
                cabStatus.minorState = MinorState.Committed;
            }
            else {
                cabStatus.interested = true;
            }
            Globals.rideService.get(0).tell(new RideService.UpdateCabStatus(getContext().getSelf().path().name(), cabStatus));
            command.replyTo.tell(new FulfillRide.RideAssigned(rideAssigned, getContext().getSelf().path().name()));

        }


        return this;
    }

    public Behavior<CabCommands> onSignIn(SignIn command) {

        cabStatus.majorState = MajorState.SignedIn;
        cabStatus.minorState = MinorState.Available;
        cabStatus.interested = true;
        cabStatus.location = command.initialPos;

        Globals.rideService.get(0).tell(new RideService.UpdateCabStatus(getContext().getSelf().path().name(), cabStatus));

        return this;
    }

    public Behavior<CabCommands> onSignOut(SignOut command) {

        cabStatus.majorState = MajorState.SignedOut;
        cabStatus.minorState = MinorState.NotAvailable;

        Globals.rideService.get(0).tell(new RideService.UpdateCabStatus(getContext().getSelf().path().name(), cabStatus));

        return this;
    }

    public Behavior<CabCommands> onRideEnded(RideEnded command) {

        if(cabStatus.minorState == MinorState.GivingRide) {
            cabStatus.minorState = MinorState.Available;
            cabStatus.location = this.destinationLoc;
            this.fRide.tell(new FulfillRide.RideEnded(cabStatus));
        }

        return this;
    }

    private Behavior<CabCommands> onNumRidesRequest(NumRides command) {

        command.replyTo.tell(new NumRidesResponse(this.numRides));
        return this;
    }

    private Behavior<CabCommands> onReset(Reset command) {

        for(String cabId: Globals.cabs.keySet()) {
            ActorRef<Cab.CabCommands> cab = Globals.cabs.get(cabId);
            cab.tell(new InitializeCab());
        }
        command.replyTo.tell(new NumRidesResponse(this.numRides));
        return this;
    }

}
