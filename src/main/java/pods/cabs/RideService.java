package pods.cabs;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;

public class RideService extends AbstractBehavior<RideService.RideCommands>{


    @Override
    public Receive<RideService.RideCommands> createReceive() {
        return null;
    }

    public RideService(ActorContext<RideCommands> context) {
        super(context);
    }

    public static Behavior<RideCommands> create() {
        return Behaviors.setup(RideService::new);
    }

    public static final class RideDetails {


    }
    interface RideCommands {}
}
