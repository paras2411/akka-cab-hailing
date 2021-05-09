package pods.cabs;

import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import junit.framework.TestCase;
import akka.actor.testkit.typed.javadsl.ActorTestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Scanner;
import junit.framework.TestCase;

public class RideServiceTest extends TestCase {
    @ClassRule
    static final ActorTestKit testKit = ActorTestKit.create();

    @AfterClass
    public static void cleanup() {
        testKit.shutdownTestKit();
    }

    @BeforeClass
    public void testMainActorCreation() {
        TestProbe<Main.Started> probe = testKit.createTestProbe();
        ActorRef<Void> mainRef = testKit.spawn(Main.create(probe.getRef()), "main_initialized");

        Main.Started check = probe.receiveMessage();
        //assertEquals(check.message, "All good");
        assertSame(check.message, "All good");
        //System.out.println("1");
    }

    @Test
    public void fun1(){
        ActorRef<Cab.CabCommands> cab101 = Globals.cabs.get("101");
        ActorRef<RideService.RideCommands> rideService = Globals.rideService.get(0);
        TestProbe<Cab.GetCabStatus> probe1 = testKit.createTestProbe();
        TestProbe<RideService.GetCabStatus> probe2 = testKit.createTestProbe();
        TestProbe<RideService.RideResponse> probe3 = testKit.createTestProbe();

        cab101.tell(new Cab.replyCabStatus(probe1.getRef()));
        assertEquals(probe1.receiveMessage().status.majorState, MajorState.SignedOut);

        cab101.tell(new Cab.SignIn(20));
        cab101.tell(new Cab.replyCabStatus(probe1.getRef()));
        assertEquals(probe1.receiveMessage().status.majorState, MajorState.SignedIn);

        rideService.tell(new RideService.RequestRide("201", 20, 60, probe3.getRef()));
        RideService.RideResponse resp = probe3.receiveMessage();
        assertNotSame(resp.rideId , -1);

        cab101.tell(new Cab.replyCabStatus(probe1.getRef()));
        Cab.GetCabStatus sts = probe1.receiveMessage();
        assertEquals(sts.status.minorState, MinorState.Committed);

        cab101.tell(new Cab.RideEnded(resp.rideId));
        cab101.tell(new Cab.replyCabStatus(probe1.ref()));
        Cab.GetCabStatus sts1 = probe1.receiveMessage();
        assertEquals(sts1.status.interested, Boolean.FALSE);
        assertEquals(sts1.status.minorState, MinorState.Available);

    }
}