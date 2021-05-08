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

public class MainTest extends TestCase {

    @ClassRule
    static final ActorTestKit testKit = ActorTestKit.create();

    @AfterClass
    public static void cleanup() {
        testKit.shutdownTestKit();
    }

    @Test
    public void testMainActorCreation() {

        TestProbe<Main.Started> probe = testKit.createTestProbe();
        ActorRef<Void> mainRef = testKit.spawn(Main.create(probe.getRef()), "main_initialized");

        Main.Started check = probe.receiveMessage();
        //assertEquals(check.message, "All good");
        assertSame(check.message, "All good");
        //System.out.println("1");
    }

    @Test
    public void testGetWallet() {
        TestProbe<Wallet.ResponseBalance> probe = testKit.createTestProbe();

        Globals.wallets.get("201").tell(new Wallet.Reset(probe.getRef()));
        Globals.wallets.get("201").tell(new Wallet.GetBalance(probe.getRef()));
        //balChk.tell(new Wallet.Reset(probe.getRef()));
        //balChk.tell(new Wallet.GetBalance(probe.getRef()));

        Wallet.ResponseBalance check = probe.receiveMessage();
        assertEquals(check.walletBalance, 10000);
        //System.out.println("2");
    }

    @Test
    public void testAddWallet(){
        TestProbe<Wallet.ResponseBalance> probe = testKit.createTestProbe();

        Globals.wallets.get("202").tell(new Wallet.Reset(probe.getRef()));
        //Add 1000
        Globals.wallets.get("202").tell(new Wallet.AddBalance(1000));
        Globals.wallets.get("202").tell(new Wallet.GetBalance(probe.getRef()));

//        balAdd.tell(new Wallet.Reset(probe.getRef()));
//        Add 1000
//        balAdd.tell(new Wallet.AddBalance(1000));
//        balAdd.tell(new Wallet.GetBalance(probe.getRef()));

        Wallet.ResponseBalance check = probe.receiveMessage();
        assertEquals(check.walletBalance, 11000);
        //System.out.println("3");
    }

    @Test
    public void testDeductWallet() {
        TestProbe<Wallet.ResponseBalance> probe = testKit.createTestProbe();

        Globals.wallets.get("203").tell(new Wallet.Reset(probe.getRef()));
        //Deduct 1000
        //Globals.wallets.get("203").tell(new Wallet.DeductBalance(1000, probe.getRef()));

//        balDed.tell(new Wallet.Reset(probe.getRef()));
//        Deduct 1000
//        balDed.tell(new Wallet.DeductBalance(1000, probe.getRef()));

        //Wallet.ResponseBalance check = probe.receiveMessage();
        //assertEquals(check.walletBalance, 9000);


        //Deduct 10001 -> Not Possible
        Globals.wallets.get("203").tell(new Wallet.DeductBalance(10001, probe.getRef()));
        //balDed.tell(new Wallet.DeductBalance(10001, probe.getRef()));
        //check = probe.receiveMessage();
        assertEquals(probe.receiveMessage().walletBalance, -1);
        //System.out.println("4");
    }

    @Test
    public void testResetWallet(){
        TestProbe<Wallet.ResponseBalance> probe = testKit.createTestProbe();

        Globals.wallets.get("201").tell(new Wallet.Reset(probe.getRef()));
        //System.out.println(probe.receiveMessage());

        assertEquals(probe.receiveMessage().walletBalance, 10000);
        //System.out.println("5");

        // not sending any message back
    }

    @Test
    public void testNumRideCab(){
        TestProbe<Cab.NumRidesResponse> probe = testKit.createTestProbe();
        ActorRef<Cab.CabCommands> numRides = testKit.spawn(Cab.create(), "cab_rides");

        numRides.tell(new Cab.NumRides(probe.getRef()));
        assertEquals(probe.receiveMessage().numRides, 0);
        //System.out.println("6");

        // increment function of NumRidesResponse is never used
        //how is it incrementing
    }

    @Test
    public void testResetCab(){
        TestProbe<Cab.NumRidesResponse> probe = testKit.createTestProbe();
        //ActorRef<Cab.CabCommands> cabRst = testKit.spawn(Cab.create(), "cab_reset");

        Globals.cabs.get("101").tell(new Cab.Reset(probe.getRef()));
        //cabRst.tell(new Cab.Reset(probe.getRef()));
        assertEquals(probe.receiveMessage().numRides, 0);
        //System.out.println("7");

        //not sending any message back
    }

    @Test
    public void testCab() {
        //sir's testcase

        ActorRef<Cab.CabCommands> cab101 = Globals.cabs.get("101");
        cab101.tell(new Cab.SignIn("101",10));
        ActorRef<RideService.RideCommands> rideService = Globals.rideService.get(0);

        TestProbe<RideService.RideResponse> probe = testKit.createTestProbe();
        rideService.tell(new RideService.RequestRide("201", 10, 100, probe.ref()));
        RideService.RideResponse resp = probe.receiveMessage();

        assertNotSame(resp.rideId , -1);
        cab101.tell(new Cab.RideEnded(resp.rideId));
        //System.out.println("8");
    }

    @Test
    public void testCab1() {
        ActorRef<Cab.CabCommands> cab101 = Globals.cabs.get("102");
        cab101.tell(new Cab.SignIn("102",20));
        ActorRef<RideService.RideCommands> rideService = Globals.rideService.get(1);

//        TestProbe<RideService.RideResponse> probe1 = testKit.createTestProbe();
//        rideService.tell(new RideService.RequestRide("201", 10, 100, probe1.ref()));
//        RideService.RideResponse resp = probe1.receiveMessage();
//
//        assertNotSame(resp.rideId , -1);
//        cab101.tell(new Cab.RideEnded(resp.rideId));
        //System.out.println("9");
    }

}
