package pods.cabs;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class Main  {


    public static class Started {
        public final String message;
        public Started(String message) {
            this.message = message;
        }
    }



    public static Behavior<Void> create(ActorRef<Started> probe) {
        return Behaviors.setup(
                context -> {

                    // Instantiating the Global data
                    Globals.cabs = new HashMap<>();
                    Globals.wallets = new HashMap<>();
                    Globals.rideId = 0;
                    Globals.fulCab = new HashMap<>();

                    // reading the initial data and creating actors
                    File file = new File("src/main/java/pods/cabs/IDs.txt");
                    try {
                        Scanner scan = new Scanner(file);
                        int counter = 0;
                        List<String> customers = new ArrayList<String>();
                        while(scan.hasNextLine()) {
                            String cur = scan.nextLine();
                            if(cur.equals("****")) counter++;
                            else if(counter == 1) {
                                ActorRef<Cab.Command> cab = context.spawn(Cab.create(), cur);
                                Globals.cabs.put(cur, cab);
                            }
                            else if(counter == 2) {
                                customers.add(cur);
                            }
                            else {
                                Globals.initialBalance = Integer.parseInt(cur);
                            }
                        }
                        for(String customer: customers) {
                            ActorRef<Wallet.Command> wallet = context.spawn(Wallet.create(), customer);
                            Globals.wallets.put(customer, wallet);
                            wallet.tell(new Wallet.InitWallet(Globals.initialBalance));
                        }
                    }
                    catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    Globals.rideService = new ArrayList<>();
                    for(int i=0; i<10; i++){
                        ActorRef<RideService.Command> ride = context.spawn(RideService.create(), "ride" + i);
                        for(String cab: Globals.cabs.keySet()) {
                            ride.tell(new RideService.StoreCabStatus(cab, new CabStatus()));
                        }
                        Globals.rideService.add(ride);
                    }

                    if(Globals.cabs.size() == 4 && Globals.rideService.size() == 10 && Globals.wallets.size() == 3) {
                        probe.tell(new Started("All good"));
                    }
                    else {
                        probe.tell(new Started("Nope"));
                    }

                    return Behaviors.empty(); // don't want to receive any more messages

                    // We can return an empty behavior, because we are returning the user-guardian actor, and
                    // normally this actor need not receive any messages.
                });
    }

    public static void main(String[] args) {
//        final ActorSystem<Void> main = ActorSystem.create(Main.create(), "CabHailing"); // create "user guardian" actor
    }
}
