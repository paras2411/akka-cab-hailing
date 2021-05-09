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
        //Add 1000
        Globals.wallets.get("202").tell(new Wallet.AddBalance(-1000));
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
    }

    @Test
    public void testResetCab(){
        TestProbe<Cab.NumRidesResponse> probe = testKit.createTestProbe();
        //ActorRef<Cab.CabCommands> cabRst = testKit.spawn(Cab.create(), "cab_reset");

        Globals.cabs.get("101").tell(new Cab.Reset(probe.getRef()));
        //cabRst.tell(new Cab.Reset(probe.getRef()));
        assertEquals(probe.receiveMessage().numRides, 0);
        //System.out.println("7");
    }

    @Test
    public void testCab() {
        //sir's testcase

        ActorRef<Cab.CabCommands> cab101 = Globals.cabs.get("101");
        cab101.tell(new Cab.SignIn(10));
        ActorRef<RideService.RideCommands> rideService = Globals.rideService.get(0);

        TestProbe<Cab.NumRidesResponse> probe1 = testKit.createTestProbe();
        ActorRef<Cab.CabCommands> numRides = testKit.spawn(Cab.create(), "cab_num_rides");

        TestProbe<RideService.RideResponse> probe = testKit.createTestProbe();
        rideService.tell(new RideService.RequestRide("201", 10, 100, probe.ref()));
        RideService.RideResponse resp = probe.receiveMessage();

        assertNotSame(resp.rideId , -1);

        // test #ride after allocating a ride
        numRides.tell(new Cab.NumRides(probe1.getRef()));
        assertEquals(probe1.receiveMessage().numRides, 1);

        cab101.tell(new Cab.RideEnded(resp.rideId));
        // test #ride after ending a ride
        numRides.tell(new Cab.NumRides(probe1.getRef()));
        assertEquals(probe1.receiveMessage().numRides, 1);

        //System.out.println("8");
    }

    @Test
    public void testSignIn() {
        ActorRef<Cab.CabCommands> cab102 = Globals.cabs.get("102");
        cab102.tell(new Cab.SignIn(20));
        ActorRef<RideService.RideCommands> rideService = Globals.rideService.get(1);
        TestProbe<Cab.GetCabStatus> probe1 = testKit.createTestProbe();
        cab102.tell(new Cab.replyCabStatus(probe1.ref()));

        Cab.GetCabStatus cabStatus = probe1.receiveMessage();
        assertEquals(cabStatus.status.majorState, MajorState.SignedIn);

        TestProbe<RideService.GetCabStatus> probe2 = testKit.createTestProbe();

        rideService.tell(new RideService.replyCabStatus(probe2.ref(), "102"));
        RideService.GetCabStatus status = probe2.receiveMessage();
        assertEquals(status.status.majorState, MajorState.SignedIn);
    }

    @Test
    public void testSignOut(){
        ActorRef<Cab.CabCommands> cab103 = Globals.cabs.get("103");
        ActorRef<RideService.RideCommands> rideService = Globals.rideService.get(2);
        cab103.tell(new Cab.SignOut());
        TestProbe<Cab.GetCabStatus> probe1 = testKit.createTestProbe();
        TestProbe<RideService.GetCabStatus> probe2 = testKit.createTestProbe();

        cab103.tell(new Cab.replyCabStatus(probe1.getRef()));
        assertEquals(probe1.receiveMessage().status.majorState, MajorState.SignedOut);

        rideService.tell(new RideService.replyCabStatus(probe2.getRef(),"103"));
        assertEquals(probe2.receiveMessage().status.majorState, MajorState.SignedOut);
    }

    @Test
    public void testNewCab(){
        //try{Thread.sleep(500);}catch(InterruptedException e){System.out.println(e);}
        ActorRef<Cab.CabCommands> cab105 = testKit.spawn(Cab.create(), "105");
        ActorRef<RideService.RideCommands> rideService = Globals.rideService.get(3);

        CabStatus var = new CabStatus();
        var.interested = false;
        var.location = 0;
        var.majorState = MajorState.SignedOut;
        var.minorState = MinorState.NotAvailable;

        TestProbe<Cab.GetCabStatus> probe1 = testKit.createTestProbe();
        TestProbe<RideService.GetCabStatus> probe2 = testKit.createTestProbe();

        cab105.tell(new Cab.replyCabStatus(probe1.getRef()));
        //assertEquals(probe1.receiveMessage().status, var);
        Cab.GetCabStatus status1 = probe1.receiveMessage();
        assertEquals(status1.status.majorState, var.majorState);
        assertEquals(status1.status.minorState, var.minorState);
        assertEquals(status1.status.interested, var.interested);
        assertEquals(status1.status.location, var.location);

        // What will be the cabId
    }

    @Test
    public void testAfterGivingRide (){
        ActorRef<Cab.CabCommands> cab104 = Globals.cabs.get("104");
        cab104.tell(new Cab.SignIn(50));
        ActorRef<RideService.RideCommands> rideService = Globals.rideService.get(4);

        ActorRef<Cab.CabCommands> stats = testKit.spawn(Cab.create(), "cab_status");

        TestProbe<Cab.GetCabStatus> probe1 = testKit.createTestProbe();
        TestProbe<RideService.RideResponse> probe2 = testKit.createTestProbe();
        rideService.tell(new RideService.RequestRide("203", 50, 60, probe2.ref()));
        RideService.RideResponse resp = probe2.receiveMessage();

        assertNotSame(resp.rideId , -1);

        cab104.tell(new Cab.RideEnded(resp.rideId)); //due to this

        cab104.tell(new Cab.replyCabStatus(probe1.getRef()));

        Cab.GetCabStatus status = probe1.receiveMessage();

        CabStatus var = new CabStatus();
        var.interested = false;
        var.location = 60;
        var.majorState = MajorState.SignedIn;
        var.minorState = MinorState.Available;

        assertEquals(status.status.majorState, var.majorState);
        assertEquals(status.status.minorState, var.minorState);
        assertEquals(status.status.interested, var.interested);
        assertEquals(status.status.location, var.location);
    }

    @Test
    public void fun(){

    }
}
