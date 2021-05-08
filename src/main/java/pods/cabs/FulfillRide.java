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

    interface Command {}

    public static final class RideEnded implements Command {

    }

    public static final class RequestRide implements Command {

        public int rideId;

        public RequestRide() {
            rideId = -1;
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
        this.rideId = 0;
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
                .build();
    }

    public Behavior<Command> onRequestRide(RequestRide command) {

        // finding the nearest 3 cabs
        int[] distance = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
        String[] cabId = {"", "", ""};
        for(String cab: cabs.keySet()) {
            CabStatus status = cabs.get(cab);
            int far = Math.abs(status.location - sourceLoc);
            for(int i = 0; i < 3; i++) {
                if(far < distance[i]) {
                    swap(distance, cabId, cab, far, i);
                }
                else if(far == distance[i]) {
                    if(cabs.get(cabId[i]).minorState != MinorState.Available || !cabs.get(cabId[i]).interested) {
                        swap(distance, cabId, cab, far, i);
                        break;
                    }
                }
            }
        }

        for(int i = 0; i < 3; i++) {
            if(!cabId[i].equals("")) {
                Globals.cabs.get(cabId[i]).tell(new Cab.RequestRide(cabId[i], sourceLoc, destinationLoc, command, getContext().getSelf()));
                // everything inside this
            }
            if(command.rideId != -1) {
                break;
            }
        }

        if(command.rideId == -1) {
            return Behaviors.stopped();
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
