package pods.cabs;

import akka.actor.typed.ActorRef;

import java.util.ArrayList;
import java.util.HashMap;

public class Globals {

    HashMap<String, ActorRef<Cab.CabDetails>> cabs;
    HashMap<String, ActorRef<Wallet.WalletDetails>> wallets;
    ArrayList<ActorRef<RideService.RideDetails>> rideService;
}
