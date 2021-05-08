package pods.cabs;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;

import java.util.HashMap;

public class RideService extends AbstractBehavior<RideService.RideCommands>{

    HashMap<String, CabStatus> cabs;

    @Override
    public Receive<RideService.RideCommands> createReceive() {

        return newReceiveBuilder()
                .onMessage(CabSignsIn.class, this::onCabSignsIn)
                .onMessage(CabSignsOut.class, this::onCabSignsOut)
                .onMessage(RequestRide.class, this::onRequestRide)
                .onMessage(UpdateCabStatus.class, this::onUpdateCabStatus)
                .onMessage(StoreCabStatus.class, this::onStoreCabStatus)
                .build();

    }


    public Behavior<RideCommands> onStoreCabStatus(StoreCabStatus command) {

        cabs.put(command.cabId, command.cabStatus);

        return this;
    }

    public Behavior<RideCommands> onUpdateCabStatus(UpdateCabStatus command) {

        cabs.put(command.cabId, command.cabStatus);
        broadcastUpdate(command.cabId);

        return this;
    }
    public Behavior<RideCommands> onRequestRide(RequestRide command) {

        ActorRef<FulfillRide.Command> fulFillActor = getContext().spawn(FulfillRide.create(
                cabs,
                command.sourceLoc,
                command.destinationLoc,
                command.custId,
                command.replyTo
        ), command.custId);

        fulFillActor.tell(new FulfillRide.RequestRide());

        return this;
    }

    public void broadcastUpdate(String cabId) {
        for(ActorRef<RideService.RideCommands> ride: Globals.rideService) {
            ride.tell(new StoreCabStatus(cabId, cabs.get(cabId)));
        }
    }

    public Behavior<RideCommands> onCabSignsIn(CabSignsIn command) {

        String cabId = command.cabId;
        CabStatus cabStatus = cabs.get(cabId);
        cabStatus.location = command.initialPos;
        cabStatus.interested = true;
        cabStatus.majorState = MajorState.SignedIn;
        cabStatus.minorState = MinorState.Available;
        broadcastUpdate(cabId);
        return this;
    }

    public Behavior<RideCommands> onCabSignsOut(CabSignsOut command) {

        String cabId = command.cabId;
        CabStatus cabStatus = cabs.get(cabId);
        cabStatus.majorState = MajorState.SignedOut;
        cabStatus.minorState = MinorState.NotAvailable;
        broadcastUpdate(cabId);
        return this;
    }

    public RideService(ActorContext<RideCommands> context) {
        super(context);
        cabs = new HashMap<>();
    }

    public static Behavior<RideCommands> create() {
        return Behaviors.setup(RideService::new);
    }

    interface RideCommands {}

    public static class StoreCabStatus implements RideCommands {
        public final String cabId;
        public final CabStatus cabStatus;

        public StoreCabStatus(String cabId, CabStatus cabStatus) {
            this.cabId = cabId;
            this.cabStatus = cabStatus;
        }
    }

    public static class UpdateCabStatus implements RideCommands {

        public final String cabId;
        public final CabStatus cabStatus;

        public UpdateCabStatus(String cabId, CabStatus cabStatus) {
            this.cabId = cabId;
            this.cabStatus = cabStatus;
        }
    }

    public static class CabSignsIn implements RideCommands {
        public final String cabId;
        public final int initialPos;

        public CabSignsIn(String cabId, int initialPos) {
            this.cabId = cabId;
            this.initialPos = initialPos;
        }
    }

    public static class CabSignsOut implements RideCommands {
        public final String cabId;

        public CabSignsOut(String cabId) {
            this.cabId = cabId;
        }
    }

    public static class RequestRide implements RideCommands {
        public final String custId;
        public final int sourceLoc;
        public final int destinationLoc;
        public final ActorRef<RideResponse> replyTo;

        public RequestRide(String custId, int sourceLoc, int destinationLoc, ActorRef<RideResponse> replyTo) {
            this.custId = custId;
            this.sourceLoc = sourceLoc;
            this.destinationLoc = destinationLoc;
            this.replyTo = replyTo;
        }
    }

    public static class RideResponse implements RideCommands {

        public final int rideId;
        public final String cabId;
        public final int fare;
        public final ActorRef<FulfillRide.Command> fRide;

        public RideResponse(int rideId, String cabId, int fare, ActorRef<FulfillRide.Command> fRide) {
            this.rideId = rideId;
            this.cabId = cabId;
            this.fare = fare;
            this.fRide = fRide;
        }
    }



}
