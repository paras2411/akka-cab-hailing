package pods.cabs;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;

import java.util.HashMap;

public class RideService extends AbstractBehavior<RideService.Command>{

    HashMap<String, CabStatus> cabs;

    @Override
    public Receive<Command> createReceive() {

        return newReceiveBuilder()
                .onMessage(CabSignsIn.class, this::onCabSignsIn)
                .onMessage(CabSignsOut.class, this::onCabSignsOut)
                .onMessage(RequestRide.class, this::onRequestRide)
                .onMessage(UpdateCabStatus.class, this::onUpdateCabStatus)
                .onMessage(StoreCabStatus.class, this::onStoreCabStatus)
                .onMessage(replyCabStatus.class, this::onreplyCabStatus)
                .build();

    }

    public Behavior<Command> onreplyCabStatus(replyCabStatus command) {

        command.replyTo.tell(new GetCabStatus(cabs.get(command.cabId)));
        return this;
    }
    public static final class GetCabStatus {
        public final CabStatus status;
        public GetCabStatus(CabStatus status) {
            this.status = status;
        }
    }
    public static final class replyCabStatus implements Command {
        public final ActorRef<RideService.GetCabStatus> replyTo;
        public final String cabId;
        public replyCabStatus(ActorRef<RideService.GetCabStatus> replyTo, String cabId) {
            this.replyTo = replyTo;
            this.cabId = cabId;
        }
    }


    public Behavior<Command> onStoreCabStatus(StoreCabStatus command) {

        cabs.put(command.cabId, command.cabStatus);

        return this;
    }

    public Behavior<Command> onUpdateCabStatus(UpdateCabStatus command) {

        cabs.put(command.cabId, command.cabStatus);
        broadcastUpdate(command.cabId);

        return this;
    }
    public Behavior<Command> onRequestRide(RequestRide command) {

        ActorRef<FulfillRide.Command> fulFillActor = getContext().spawn(FulfillRide.create(
                cabs,
                command.sourceLoc,
                command.destinationLoc,
                command.custId,
                command.replyTo
        ), command.custId + command.sourceLoc + command.destinationLoc);

        fulFillActor.tell(new FulfillRide.RequestRide());

        return this;
    }

    public void broadcastUpdate(String cabId) {
        for(ActorRef<Command> ride: Globals.rideService) {
            ride.tell(new StoreCabStatus(cabId, cabs.get(cabId)));
        }
    }

    public Behavior<Command> onCabSignsIn(CabSignsIn command) {

        String cabId = command.cabId;
        CabStatus cabStatus = cabs.get(cabId);
        cabStatus.location = command.initialPos;
        cabStatus.interested = true;
        cabStatus.majorState = MajorState.SignedIn;
        cabStatus.minorState = MinorState.Available;
        broadcastUpdate(cabId);
        return this;
    }

    public Behavior<Command> onCabSignsOut(CabSignsOut command) {

        String cabId = command.cabId;
        CabStatus cabStatus = cabs.get(cabId);
        cabStatus.majorState = MajorState.SignedOut;
        cabStatus.minorState = MinorState.NotAvailable;
        broadcastUpdate(cabId);
        return this;
    }

    public RideService(ActorContext<Command> context) {
        super(context);
        cabs = new HashMap<>();
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(RideService::new);
    }

    interface Command {}

    public static class StoreCabStatus implements Command {
        public final String cabId;
        public final CabStatus cabStatus;

        public StoreCabStatus(String cabId, CabStatus cabStatus) {
            this.cabId = cabId;
            this.cabStatus = cabStatus;
        }
    }

    public static class UpdateCabStatus implements Command {

        public final String cabId;
        public final CabStatus cabStatus;

        public UpdateCabStatus(String cabId, CabStatus cabStatus) {
            this.cabId = cabId;
            this.cabStatus = cabStatus;
        }
    }

    public static class CabSignsIn implements Command {
        public final String cabId;
        public final int initialPos;

        public CabSignsIn(String cabId, int initialPos) {
            this.cabId = cabId;
            this.initialPos = initialPos;
        }
    }

    public static class CabSignsOut implements Command {
        public final String cabId;

        public CabSignsOut(String cabId) {
            this.cabId = cabId;
        }
    }

    public static class RequestRide implements Command {
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

    public static class RideResponse implements Command {

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


//@Test
//public void testSignIn() {
//
//    ActorRef<Cab.CabCommands> cab101 = Globals.cabs.get("102");
//    cab101.tell(new Cab.SignIn(20));
//    ActorRef<RideService.RideCommands> rideService = Globals.rideService.get(0);
//    TestProbe<Cab.GetCabStatus> probe1 = testKit.createTestProbe();
//    cab101.tell(new Cab.replyCabStatus(probe1.ref()));
//
//    Cab.GetCabStatus cabStatus = probe1.receiveMessage();
//    assertEquals(cabStatus.status.majorState, MajorState.SignedIn);
//
//    TestProbe<RideService.GetCabStatus> probe2 = testKit.createTestProbe();
//
//    rideService.tell(new RideService.replyCabStatus(probe2.ref(), "102"));
//    RideService.GetCabStatus status = probe2.receiveMessage();
//    assertEquals(status.status.majorState, MajorState.SignedIn);
//
//    TestProbe<RideService.RideResponse> probe3 = testKit.createTestProbe();
//    rideService.tell(new RideService.RequestRide("201", 10, 100, probe3.ref()));
//    RideService.RideResponse resp = probe3.receiveMessage(Duration.ofMinutes(1));
//
//    assertNotSame(resp.rideId , -1);
//
//    Globals.cabs.get(resp.cabId).tell(new Cab.RideEnded());
//    Globals.cabs.get(resp.cabId).tell(new Cab.replyCabStatus(probe1.ref()));
//
//    cabStatus = probe1.receiveMessage();
//    assertEquals(cabStatus.status.minorState, MinorState.Available);
//
//}