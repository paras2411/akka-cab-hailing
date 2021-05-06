package pods.cabs;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;

public class Cab extends AbstractBehavior<Cab.CabCommands> {

    public MajorState majorState;
    public MinorState minorState;
    public Boolean interested;
    public Integer location;
    public Integer cabId;

    public Cab(ActorContext<CabCommands> context) {
        super(context);
        this.majorState = MajorState.SignedOut;
        this.minorState = MinorState.NotAvailable;
        this.interested = false;
        this.location = 0;
    }

    public static Behavior<CabCommands> create() {
        return Behaviors.setup(Cab::new);
    }

    interface CabCommands {}

    public static final class RideEnded {
        public final int rideId;
        public RideEnded(int rideId) {
            this.rideId = rideId;
        }
    }

    public static final class SignIn {

    }

    public static final class SignOut {

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
        return newReceiveBuilder().onMessage(CabCommands.class, this::onRequest).build();
    }

    private Behavior<CabCommands> onRequest(CabCommands rides) {

        //#greeter-send-message
//        rides.replyTo.tell(new NumRidesResponse(rides.));
        return this;
    }

}
