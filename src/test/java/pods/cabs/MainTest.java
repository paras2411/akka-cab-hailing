package pods.cabs;

import akka.actor.Actor;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import junit.framework.TestCase;
import akka.actor.testkit.typed.javadsl.ActorTestKit;
import org.junit.Test;

import static pods.cabs.Main.*;

public class MainTest extends TestCase {

    static final ActorTestKit testKit = ActorTestKit.create();




    @Test
    public void testMainActorCreation() {

        TestProbe<Main.Started> probe = testKit.createTestProbe();
        ActorRef<Void> mainRef = testKit.spawn(Main.create(probe.getRef()), "main_initialized");

        Main.Started check = probe.receiveMessage();
//        assertEquals(check.message, "All good");
        assertSame(check.message, "All good");
        // #test-spawn
    }


}