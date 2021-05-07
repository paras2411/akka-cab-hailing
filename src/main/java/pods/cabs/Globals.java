package pods.cabs;

import akka.actor.typed.ActorRef;

import java.util.ArrayList;
import java.util.HashMap;

public class Globals {

    public static HashMap<String, ActorRef<Cab.CabCommands>> cabs;
    public static HashMap<String, ActorRef<Wallet.WalletCommands>> wallets;
    public static ArrayList<ActorRef<RideService.RideCommands>> rideService;
    public static Integer initialBalance;

    public Globals() {
        cabs = new HashMap<>();
        wallets = new HashMap<>();
        rideService = new ArrayList<>();
        initialBalance = 0;
    }
}
