package pods.cabs;

import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import junit.framework.TestCase;
import akka.actor.testkit.typed.javadsl.ActorTestKit;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.time.Duration;

public class MainTest extends TestCase {

    @ClassRule
    static final ActorTestKit testKit = ActorTestKit.create();

    @AfterClass
    public static void cleanup() {
        testKit.shutdownTestKit();
    }

    /**
     * Testing the Main actor
     */
    @Test
    public void testMainActorCreation() {

        TestProbe<Main.Started> probe = testKit.createTestProbe();
        ActorRef<Void> mainRef = testKit.spawn(Main.create(probe.getRef()), "main_initialized");

        Main.Started check = probe.receiveMessage();
        assertSame(check.message, "All good");
    }

    /**
     * It tests the sign in command of cab actor
     */
    @Test
    public void testSignIn() {

        TestProbe<Cab.NumRidesResponse> resProbe = testKit.createTestProbe();
        Globals.cabs.get("101").tell(new Cab.Reset(resProbe.getRef()));
        Cab.NumRidesResponse respRes = resProbe.receiveMessage();
        assertEquals(respRes.numRides, 0);

        ActorRef<Cab.Command> cab102 = Globals.cabs.get("102");
        cab102.tell(new Cab.SignIn(20));
        ActorRef<RideService.Command> rideService = Globals.rideService.get(1);
        TestProbe<Cab.GetCabStatus> probe1 = testKit.createTestProbe();
        cab102.tell(new Cab.replyCabStatus(probe1.ref()));

        Cab.GetCabStatus cabStatus = probe1.receiveMessage();
        assertEquals(cabStatus.status.majorState, MajorState.SignedIn);

        TestProbe<RideService.GetCabStatus> probe2 = testKit.createTestProbe();

        rideService.tell(new RideService.replyCabStatus(probe2.ref(), "102"));
        RideService.GetCabStatus status = probe2.receiveMessage();
        assertEquals(status.status.majorState, MajorState.SignedIn);
    }

    /**
     * It tests the sign out command of cab actor as well as check the cache table of ride service
     */
    @Test
    public void testSignOut(){

        TestProbe<Cab.NumRidesResponse> resProbe = testKit.createTestProbe();
        Globals.cabs.get("101").tell(new Cab.Reset(resProbe.getRef()));
        Cab.NumRidesResponse respRes = resProbe.receiveMessage();
        assertEquals(respRes.numRides, 0);

        ActorRef<Cab.Command> cab103 = Globals.cabs.get("103");
        ActorRef<RideService.Command> rideService = Globals.rideService.get(2);
        cab103.tell(new Cab.SignOut());
        TestProbe<Cab.GetCabStatus> probe1 = testKit.createTestProbe();
        TestProbe<RideService.GetCabStatus> probe2 = testKit.createTestProbe();

        cab103.tell(new Cab.replyCabStatus(probe1.getRef()));
        assertEquals(probe1.receiveMessage().status.majorState, MajorState.SignedOut);

        rideService.tell(new RideService.replyCabStatus(probe2.getRef(),"103"));
        assertEquals(probe2.receiveMessage().status.majorState, MajorState.SignedOut);
    }

    /**
     * In this test case, we are first signing one of the cab then requesting the ride such that it is assigned to the
     * cab. After that we are checking for the Ride ended as well. Also checking the alternate interested case, where
     * we are requesting ride which makes that cab interested for next request.
     */
    @Test
    public void testRequestRideAndRideEnded() {

        TestProbe<Cab.NumRidesResponse> resProbe = testKit.createTestProbe();
        Globals.cabs.get("101").tell(new Cab.Reset(resProbe.getRef()));
        Cab.NumRidesResponse respRes = resProbe.receiveMessage();
        assertEquals(respRes.numRides, 0);

        TestProbe<Wallet.ResponseBalance> resProbeWallet = testKit.createTestProbe();
        ActorRef<Wallet.Command> cust201 = Globals.wallets.get("201");
        cust201.tell(new Wallet.Reset(resProbeWallet.getRef()));
        Wallet.ResponseBalance amt = resProbeWallet.receiveMessage();
        assertEquals(amt.walletBalance, 10000);

        ActorRef<Cab.Command> cab101 = Globals.cabs.get("102");
        cab101.tell(new Cab.SignIn(20));
        ActorRef<RideService.Command> rideService = Globals.rideService.get(0);
        TestProbe<Cab.GetCabStatus> probe1 = testKit.createTestProbe();
        cab101.tell(new Cab.replyCabStatus(probe1.ref()));
        Cab.GetCabStatus cabStatus = probe1.receiveMessage();
        assertEquals(cabStatus.status.majorState, MajorState.SignedIn);

        TestProbe<RideService.GetCabStatus> probe2 = testKit.createTestProbe();
        rideService.tell(new RideService.replyCabStatus(probe2.ref(), "102"));
        RideService.GetCabStatus status = probe2.receiveMessage();
        assertEquals(status.status.majorState, MajorState.SignedIn);

        TestProbe<RideService.RideResponse> probe3 = testKit.createTestProbe();
        rideService.tell(new RideService.RequestRide("202", 10, 100, probe3.ref()));
        RideService.RideResponse resp = probe3.receiveMessage(Duration.ofMinutes(1));
        assertNotSame(resp.rideId , -1);

        String cabIds = resp.cabId;

        Globals.cabs.get(resp.cabId).tell(new Cab.RideEnded(resp.rideId));

        Globals.cabs.get(resp.cabId).tell(new Cab.replyCabStatus(probe1.ref()));
        cabStatus = probe1.receiveMessage();
        assertEquals(cabStatus.status.minorState, MinorState.Available);
        assertEquals(cabStatus.status.interested, Boolean.FALSE);

        rideService.tell(new RideService.RequestRide("202", 90, 101, probe3.ref()));
        resp = probe3.receiveMessage(Duration.ofMinutes(1));
        assertSame(resp.rideId , -1);

        Globals.cabs.get(cabIds).tell(new Cab.replyCabStatus(probe1.ref()));
        cabStatus = probe1.receiveMessage();
        assertEquals(cabStatus.status.minorState, MinorState.Available);
        assertEquals(cabStatus.status.interested, Boolean.TRUE);

    }


    /**
     * In this test case, we are adding amount and getting the balance after that.
     */
    @Test
    public void testAddAmount() {


        TestProbe<Wallet.ResponseBalance> probe1 = testKit.createTestProbe();
        ActorRef<Wallet.Command> cust201 = Globals.wallets.get("201");
        cust201.tell(new Wallet.Reset(probe1.getRef()));
        Wallet.ResponseBalance amt = probe1.receiveMessage();
        assertEquals(amt.walletBalance, 10000);

        TestProbe<Wallet.ResponseBalance> probe2 = testKit.createTestProbe();
        cust201.tell(new Wallet.AddBalance(100));
        cust201.tell(new Wallet.AddBalance(-100));
        cust201.tell(new Wallet.GetBalance(probe2.ref()));
        amt = probe2.receiveMessage();
        assertEquals(amt.walletBalance, 10100);

    }


    /**
     * It tests the deduct functionality of wallet actor
     */
    @Test
    public void testDeductAmount() {

        TestProbe<Wallet.ResponseBalance> probe1 = testKit.createTestProbe();
        ActorRef<Wallet.Command> cust201 = Globals.wallets.get("201");
        cust201.tell(new Wallet.Reset(probe1.getRef()));
        Wallet.ResponseBalance amt = probe1.receiveMessage();
        assertEquals(amt.walletBalance, 10000);

        Globals.wallets.get("202").tell(new Wallet.DeductBalance(1000, probe1.getRef()));

        Wallet.ResponseBalance check = probe1.receiveMessage();
        assertEquals(check.walletBalance, 9000);

        Globals.wallets.get("202").tell(new Wallet.DeductBalance(-1000, probe1.getRef()));

        check = probe1.receiveMessage();
        assertEquals(check.walletBalance, -1);

        Globals.wallets.get("202").tell(new Wallet.DeductBalance(10000, probe1.getRef()));

        check = probe1.receiveMessage();
        assertEquals(check.walletBalance, -1);

    }


    /**
     * This tests the flow of request ride and finally tests if the number of rides is increased for the cab if the
     * ride allocates to it.
     */
    @Test
    public void testNumRideCab(){

        TestProbe<Cab.NumRidesResponse> probe1 = testKit.createTestProbe();
        Globals.cabs.get("101").tell(new Cab.Reset(probe1.getRef()));
        Cab.NumRidesResponse resp = probe1.receiveMessage();
        assertEquals(resp.numRides, 0);

        TestProbe<Wallet.ResponseBalance> probe2 = testKit.createTestProbe();
        ActorRef<Wallet.Command> cust201 = Globals.wallets.get("201");
        cust201.tell(new Wallet.Reset(probe2.getRef()));
        Wallet.ResponseBalance amt = probe2.receiveMessage();
        assertEquals(amt.walletBalance, 10000);

        ActorRef<Cab.Command> cab101 = Globals.cabs.get("101");

        TestProbe<Cab.GetCabStatus> probe3 = testKit.createTestProbe();
        TestProbe<RideService.RideResponse> probe4 = testKit.createTestProbe();

        cab101.tell(new Cab.replyCabStatus(probe3.getRef()));
        assertEquals(probe3.receiveMessage().status.majorState, MajorState.SignedOut);

        cab101.tell(new Cab.SignIn(20));
        cab101.tell(new Cab.replyCabStatus(probe3.getRef()));
        Cab.GetCabStatus cabStatus = probe3.receiveMessage();
        assertEquals(cabStatus.status.majorState, MajorState.SignedIn);
        assertEquals(cabStatus.status.interested, Boolean.TRUE);

        ActorRef<RideService.Command> rideService = Globals.rideService.get(0);
        rideService.tell(new RideService.RequestRide("201", 20, 50, probe4.getRef()));
        RideService.RideResponse rideResp = probe4.receiveMessage(Duration.ofMinutes(1));
        assertNotSame(rideResp.rideId , -1);


        Globals.cabs.get("101").tell(new Cab.NumRides(probe1.getRef()));
        assertEquals(probe1.receiveMessage().numRides, 1);

    }

    /**
     * This tests the flow of request ride and finally tests if the number of rides is increased for the cab if the
     * ride allocates to it.
     */
    @Test
    public void testWalletBalanceIsLessThanRideCost(){

        TestProbe<Cab.NumRidesResponse> probe1 = testKit.createTestProbe();
        Globals.cabs.get("101").tell(new Cab.Reset(probe1.getRef()));
        Cab.NumRidesResponse resp = probe1.receiveMessage();
        assertEquals(resp.numRides, 0);

        TestProbe<Wallet.ResponseBalance> probe2 = testKit.createTestProbe();
        ActorRef<Wallet.Command> cust201 = Globals.wallets.get("201");
        cust201.tell(new Wallet.Reset(probe2.getRef()));
        Wallet.ResponseBalance amt = probe2.receiveMessage();
        assertEquals(amt.walletBalance, 10000);

        ActorRef<Cab.Command> cab101 = Globals.cabs.get("101");

        TestProbe<Cab.GetCabStatus> probe3 = testKit.createTestProbe();
        TestProbe<RideService.RideResponse> probe4 = testKit.createTestProbe();

        cab101.tell(new Cab.replyCabStatus(probe3.getRef()));
        assertEquals(probe3.receiveMessage().status.majorState, MajorState.SignedOut);

        cab101.tell(new Cab.SignIn(20));
        cab101.tell(new Cab.replyCabStatus(probe3.getRef()));
        Cab.GetCabStatus cabStatus = probe3.receiveMessage();
        assertEquals(cabStatus.status.majorState, MajorState.SignedIn);
        assertEquals(cabStatus.status.interested, Boolean.TRUE);

        ActorRef<RideService.Command> rideService = Globals.rideService.get(0);
        rideService.tell(new RideService.RequestRide("202", 20, 60, probe4.getRef()));
        RideService.RideResponse rideResp = probe4.receiveMessage(Duration.ofMinutes(1));
        assertNotSame(rideResp.rideId , -1);


        Globals.cabs.get("101").tell(new Cab.NumRides(probe1.getRef()));
        assertEquals(probe1.receiveMessage().numRides, 1);

    }


    /**
     * It tests the status of cab when new cab actor is spawn
     */
    @Test
    public void testNewCab(){


        TestProbe<Cab.NumRidesResponse> resProbe = testKit.createTestProbe();
        Globals.cabs.get("101").tell(new Cab.Reset(resProbe.getRef()));
        Cab.NumRidesResponse respRes = resProbe.receiveMessage();
        assertEquals(respRes.numRides, 0);

        ActorRef<Cab.Command> cab105 = testKit.spawn(Cab.create(), "105");
        CabStatus status = new CabStatus();

        TestProbe<Cab.GetCabStatus> probe1 = testKit.createTestProbe();
        TestProbe<RideService.GetCabStatus> probe2 = testKit.createTestProbe();

        cab105.tell(new Cab.replyCabStatus(probe1.getRef()));
        Cab.GetCabStatus cabStatus = probe1.receiveMessage();
        assertSame(cabStatus.status.majorState, status.majorState);
        assertSame(cabStatus.status.minorState, status.minorState);
        assertSame(cabStatus.status.interested, status.interested);
        assertSame(cabStatus.status.location, status.location);

    }

    /**
     * First signin cab 102 then add 2000 in wallet 202
     * Now assign a ride from 10 to 1100 and then add -2000 and check balance
     */
    @Test
    public void testpv1(){

        TestProbe<Cab.NumRidesResponse> resProbe = testKit.createTestProbe();
        Globals.cabs.get("101").tell(new Cab.Reset(resProbe.getRef()));
        Cab.NumRidesResponse respRes = resProbe.receiveMessage();
        assertEquals(respRes.numRides, 0);

        TestProbe<Wallet.ResponseBalance> probe2 = testKit.createTestProbe();
        ActorRef<Wallet.Command> cust201 = Globals.wallets.get("201");
        cust201.tell(new Wallet.Reset(probe2.getRef()));
        Wallet.ResponseBalance amt = probe2.receiveMessage();
        assertEquals(amt.walletBalance, 10000);

        ActorRef<Cab.Command> cab102 = Globals.cabs.get("102");
        ActorRef<RideService.Command> rideService = Globals.rideService.get(1);
        TestProbe<Cab.GetCabStatus> probe1 = testKit.createTestProbe();
        TestProbe<Wallet.ResponseBalance> probe = testKit.createTestProbe();
        TestProbe<RideService.RideResponse> probe3 = testKit.createTestProbe();


        cab102.tell(new Cab.SignIn(0));
        cab102.tell(new Cab.replyCabStatus(probe1.getRef()));
        assertEquals(probe1.receiveMessage().status.majorState, MajorState.SignedIn);

        Globals.wallets.get("202").tell(new Wallet.AddBalance(2000));
        Globals.wallets.get("202").tell(new Wallet.GetBalance(probe.getRef()));
        assertEquals(probe.receiveMessage().walletBalance, 12000);

        rideService.tell(new RideService.RequestRide("202", 10, 1100, probe3.getRef()));
        RideService.RideResponse resp = probe3.receiveMessage(Duration.ofMinutes(1));
        assertNotSame(resp.rideId , -1);

        Globals.wallets.get("202").tell(new Wallet.AddBalance(-2000));
        Globals.wallets.get("202").tell(new Wallet.GetBalance(probe.getRef()));

        assertEquals(probe.receiveMessage().walletBalance, 1000);
        System.out.println("Paras");
    }


    /**
     * Testing the alternate ride request
     */
    @Test
    public void testpv2(){

        TestProbe<Cab.NumRidesResponse> resProbe = testKit.createTestProbe();
        Globals.cabs.get("101").tell(new Cab.Reset(resProbe.getRef()));
        Cab.NumRidesResponse respRes = resProbe.receiveMessage();
        assertEquals(respRes.numRides, 0);

        TestProbe<Wallet.ResponseBalance> probe2 = testKit.createTestProbe();
        ActorRef<Wallet.Command> cust201 = Globals.wallets.get("201");
        cust201.tell(new Wallet.Reset(probe2.getRef()));
        Wallet.ResponseBalance amt = probe2.receiveMessage();
        assertEquals(amt.walletBalance, 10000);

        ActorRef<Cab.Command> cab101 = Globals.cabs.get("101");
        TestProbe<Cab.GetCabStatus> probe1 = testKit.createTestProbe();
        ActorRef<Cab.Command> cab102 = Globals.cabs.get("102");
        ActorRef<RideService.Command> rideService = Globals.rideService.get(4);
        ActorRef<RideService.Command> rideService1 = Globals.rideService.get(5);
        TestProbe<RideService.RideResponse> probe4 = testKit.createTestProbe();

        cab101.tell(new Cab.SignIn(10));
        cab101.tell(new Cab.replyCabStatus(probe1.getRef()));
        assertEquals(probe1.receiveMessage().status.majorState, MajorState.SignedIn);

        cab102.tell(new Cab.SignIn(30));
        cab102.tell(new Cab.replyCabStatus(probe1.getRef()));
        assertEquals(probe1.receiveMessage().status.majorState, MajorState.SignedIn);

        rideService.tell(new RideService.RequestRide("201", 10, 100, probe4.getRef()));
        RideService.RideResponse resp1 = probe4.receiveMessage(Duration.ofMinutes(1));
        assertNotSame(resp1.rideId , -1);

        rideService1.tell(new RideService.RequestRide("202", 10, 110, probe4.getRef()));
        RideService.RideResponse resp2 = probe4.receiveMessage(Duration.ofMinutes(1));
        assertNotSame(resp2.rideId , -1);

        cab101.tell(new Cab.RideEnded(resp1.rideId));

        rideService.tell(new RideService.RequestRide("201", 100, 10, probe4.getRef()));
        resp1 = probe4.receiveMessage(Duration.ofMinutes(1));
        assertEquals(resp1.rideId , -1);

        rideService.tell(new RideService.RequestRide("201", 100, 10, probe4.getRef()));
        resp1 = probe4.receiveMessage(Duration.ofMinutes(1));
        assertNotSame(resp1.rideId , -1);
        System.out.println("lohani");

    }

}
