package pods.cabs;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;

public class Cab extends AbstractBehavior<Cab.Command> {

    public CabStatus cabStatus;
    public ActorRef<FulfillRide.Command> fRide;
    public int destinationLoc;
    public int numRides;
    public int rideID;

    /**
     * function which initializes the initial value of the Cab's and no. of rides
     */
    public void initialize() {
        this.cabStatus = new CabStatus();
        this.numRides = 0;
        this.destinationLoc = this.cabStatus.location;
    }

    /**
     * constructor and invoke initialize
     */
    public Cab(ActorContext<Command> context) {
        super(context);
        this.initialize();
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(Cab::new);
    }

    interface Command {}

    /**
     * InitializeCab class which implements Command interface
     * and constructor invokes the initialize function
     */
    public class InitializeCab implements Command {
        public InitializeCab() {
            initialize();
        }
    }

    /**
     * requestRide class which implements Command interface
     * and constructor set the source location and destination
     * and also take FulfillRide type actor as a parameter
     */
    public static final class RequestRide implements Command {
        public final int sourceLoc;
        public final int destinationLoc;
        public final FulfillRide.RideAssigned assign;

        public RequestRide(String cabId, int sourceLoc, int destinationLoc, FulfillRide.RideAssigned assign) {
            this.sourceLoc = sourceLoc;
            this.destinationLoc = destinationLoc;
            this.assign = assign;
        }
    }

    /**
     * rideStarted class which implements Command interface
     * and constructor assigns the rideId, cabId, source and destination location
     * and also take a FulfillRide actor to reply back
     */
    public static final class RideStarted implements Command {
        public final int rideId;
        public final int sourceLoc;
        public final int destinationLoc;
        public final ActorRef<FulfillRide.Command> replyTo;
        public int fulRideId;

        public RideStarted(int rideId, String cabId, int sourceLoc, int destinationLoc, int fulRideId, ActorRef<FulfillRide.Command> replyTo) {
            this.rideId = rideId;
            this.sourceLoc = sourceLoc;
            this.destinationLoc = destinationLoc;
            this.fulRideId = fulRideId;
            this.replyTo = replyTo;
        }
    }

    /**
     * RideCancelled class which implements Command interface
     * and constructor takes the cab id as the parameter
     */
    public static final class RideCancelled implements Command {
        public RideCancelled(String cabId) {
        }
    }

    /**
     * RideEnded class which implements Command interface
     *  and constructor take rideId as the input to end that ride
     */
    public static final class RideEnded implements Command {
        public final int rideId;
        public RideEnded(int rideId) {
            this.rideId = rideId;
        }
    }

    /**
     * SignIn class which implements Command interface
     * and constructor takes initial position to make its
     * initial position from where he will start taking the rides
     */
    public static final class SignIn implements Command {
        public final int initialPos;

        public SignIn(int initialPos) {
            this.initialPos = initialPos;
        }
    }

    public static final class SignOut implements Command {

        public SignOut() {

        }
    }

    public static final class NumRides implements Command {
        public final ActorRef<NumRidesResponse> replyTo;
        public NumRides(ActorRef<NumRidesResponse> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static final class Reset implements Command {
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
    public static final class replyCabStatus implements Command {
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
    public Receive<Command> createReceive() {
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

    public Behavior<Command> onGetCabStatus(replyCabStatus command) {

        command.replyTo.tell(new GetCabStatus(this.cabStatus));
        return this;
    }

    public Behavior<Command> onRideCancelled(RideCancelled command) {

        if(cabStatus.minorState == MinorState.Committed) {
            cabStatus.minorState = MinorState.Available;
            Globals.rideService.get(0).tell(new RideService.UpdateCabStatus(getContext().getSelf().path().name(), cabStatus));
        }
        return this;
    }

    public Behavior<Command> onRideStarted(RideStarted command) {

        if(cabStatus.minorState == MinorState.Committed) {

            cabStatus.location = command.sourceLoc;
            cabStatus.minorState = MinorState.GivingRide;
            this.destinationLoc = command.destinationLoc;
            Globals.rideService.get(0).tell(new RideService.UpdateCabStatus(getContext().getSelf().path().name(), cabStatus));
            command.fulRideId = command.rideId;
            Globals.rideId++;
            this.numRides++;
            this.fRide = command.replyTo;
        }

        return this;
    }

    public Behavior<Command> onRequestRide(RequestRide command) {

        if(command.sourceLoc < 0 || command.destinationLoc < 0) {
            return this;
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
            command.assign.alloted = rideAssigned;
        }

        return this;
    }

    public Behavior<Command> onSignIn(SignIn command) {

        cabStatus.majorState = MajorState.SignedIn;
        cabStatus.minorState = MinorState.Available;
        cabStatus.interested = true;
        cabStatus.location = command.initialPos;

        Globals.rideService.get(0).tell(new RideService.UpdateCabStatus(getContext().getSelf().path().name(), cabStatus));

        return this;
    }

    public Behavior<Command> onSignOut(SignOut command) {

        cabStatus.majorState = MajorState.SignedOut;
        cabStatus.minorState = MinorState.NotAvailable;

        Globals.rideService.get(0).tell(new RideService.UpdateCabStatus(getContext().getSelf().path().name(), cabStatus));

        return this;
    }

    public Behavior<Command> onRideEnded(RideEnded command) {

        if(cabStatus.minorState == MinorState.GivingRide) {
            cabStatus.minorState = MinorState.Available;
            cabStatus.location = this.destinationLoc;
            this.fRide.tell(new FulfillRide.RideEnded(cabStatus));
        }

        return this;
    }

    private Behavior<Command> onNumRidesRequest(NumRides command) {

        command.replyTo.tell(new NumRidesResponse(this.numRides));
        return this;
    }

    private Behavior<Command> onReset(Reset command) {

        for(String cabId: Globals.cabs.keySet()) {
            ActorRef<Command> cab = Globals.cabs.get(cabId);
            cab.tell(new RideEnded(1));
            cab.tell(new InitializeCab());
        }
        command.replyTo.tell(new NumRidesResponse(this.numRides));
        return this;
    }

}
