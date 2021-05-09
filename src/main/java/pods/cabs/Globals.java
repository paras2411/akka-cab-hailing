package pods.cabs;

import akka.actor.typed.ActorRef;

import java.util.ArrayList;
import java.util.HashMap;

public class Globals {

    public static HashMap<String, ActorRef<Cab.Command>> cabs;
    public static HashMap<String, ActorRef<Wallet.Command>> wallets;
    public static ArrayList<ActorRef<RideService.Command>> rideService;
    public static Integer initialBalance;
    public static Integer rideId;
    public static HashMap<String, ActorRef<FulfillRide>> fulCab;

    public Globals() {
        cabs = new HashMap<>();
        wallets = new HashMap<>();
        rideService = new ArrayList<>();
        initialBalance = 0;
        rideId = 0;
        fulCab = new HashMap<>();
    }
}
