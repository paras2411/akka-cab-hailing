package pods.cabs;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;

public class Cab extends AbstractBehavior<Cab.CabCommands> {

    public CabStatus cabStatus;

    public void initialize() {
        this.cabStatus = new CabStatus();
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
        public final String cabId;
        public final int sourceLoc;
        public final int destinationLoc;
        public final ActorRef<FulfillRide.Command> replyTo;
        public final FulfillRide.RequestRide cmd;

        public RequestRide(String cabId, int sourceLoc, int destinationLoc, FulfillRide.RequestRide cmd, ActorRef<FulfillRide.Command> replyTo) {
            this.cabId = cabId;
            this.sourceLoc = sourceLoc;
            this.destinationLoc = destinationLoc;
            this.replyTo = replyTo;
            this.cmd = cmd;
        }
    }

    public static final class RideStarted implements CabCommands {
        public final int rideId;
        public final String cabId;
        public RideStarted(int rideId, String cabId) {
            this.rideId = rideId;
            this.cabId = cabId;
        }
    }

    public static final class RideCancelled implements CabCommands {
        public final int rideId;
        public final String cabId;

        public RideCancelled(int rideId, String cabId) {
            this.rideId = rideId;
            this.cabId = cabId;
        }
    }

    public static final class RideEnded implements CabCommands {
        public final int rideId;
        public final String cabId;
        public RideEnded(int rideId, String cabId) {
            this.rideId = rideId;
            this.cabId = cabId;
        }
    }

    public static final class SignIn implements CabCommands {
        public final String cabId;
        public final int initialPos;

        public SignIn(String cabId, int initialPos) {
            this.cabId = cabId;
            this.initialPos = initialPos;
        }
    }

    public static final class SignOut implements CabCommands {

        public final String cabId;
        public SignOut(String cabId) {
            this.cabId = cabId;
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

    public static final class NumRidesResponse {
        public int numRides;
        public NumRidesResponse(int numRides) {
            this.numRides = numRides;
        }
        public void increment() {
            this.numRides += 1;
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
                .build();
    }

    public Behavior<CabCommands> onRideStarted(RideStarted command) {

        if(cabStatus.minorState == MinorState.Committed) {
//            cabStatus.location =
        }

        return this;
    }

    public Behavior<CabCommands> onRequestRide(RequestRide command) {

        if(command.sourceLoc < 0 || command.destinationLoc < 0) {
            command.replyTo.tell(command.cmd);
        }

        if(cabStatus.minorState == MinorState.Available) {
            if(cabStatus.interested) {
                command.cmd.rideId = Globals.rideId++;
                command.replyTo.tell(command.cmd);
                cabStatus.interested = false;
                cabStatus.minorState = MinorState.Committed;
            }
            else {
                cabStatus.interested = true;
            }
            Globals.rideService.get(0).tell(new RideService.UpdateCabStatus(command.cabId, cabStatus));
        }

        return this;
    }

    public Behavior<CabCommands> onSignIn(SignIn command) {

        cabStatus.majorState = MajorState.SignedIn;
        cabStatus.minorState = MinorState.Available;
        cabStatus.interested = true;
        cabStatus.location = command.initialPos;

        Globals.rideService.get(0).tell(new RideService.UpdateCabStatus(command.cabId, cabStatus));

        return this;
    }

    public Behavior<CabCommands> onSignOut(SignOut command) {

        cabStatus.majorState = MajorState.SignedOut;
        cabStatus.minorState = MinorState.NotAvailable;

        Globals.rideService.get(0).tell(new RideService.UpdateCabStatus(command.cabId, cabStatus));

        return this;
    }

    public Behavior<CabCommands> onRideEnded(RideEnded command) {

        return this;
    }

    private Behavior<CabCommands> onNumRidesRequest(NumRides cabs) {

        return this;
    }

    private Behavior<CabCommands> onReset(Reset cabs) {

        for(String cabId: Globals.cabs.keySet()) {
            ActorRef<Cab.CabCommands> cab = Globals.cabs.get(cabId);
            cab.tell(new InitializeCab());
        }
        return this;
    }

}
