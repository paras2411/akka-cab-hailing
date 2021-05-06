package pods.cabs;

import pods.cabs.Globals;
import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    static interface Started {}
    public static Behavior<Void> create() {
        return Behaviors.setup(
                context -> {
                    File file = new File("IDs.txt");
                    try {
                        Scanner scan = new Scanner(file);
                        int counter = 0;
                        while(scan.hasNextLine()) {
                            String cur = scan.nextLine();
                            if(cur.equals("****")) counter++;
                            else if(counter == 1) {
                                ActorRef<Cab.SessionEvent> cab = context.spawn(Cab.create(), "Cab"+cur);

                                //Cab c = new Cab(Integer.parseInt(cur), MajorState.SignedOut, MinorState.NotAvailable, false, 0);
                                //cabRepository.save(c);
                            }
                            else if(counter == 2) {
                                ActorRef<Wallet.SessionEvent> wallet = context.spawn(Wallet.create(), "Wallet"+cur);

                                //Cab c = new Cab(Integer.parseInt(cur), MajorState.SignedOut, MinorState.NotAvailable, false, 0);
                                //cabRepository.save(c);
                            }
                        }
                    }
                    catch (FileNotFoundException e) {
                        e.printStackTrace();
                        log.info("File IDs.txt not found");
                    }

                    Globals.rideService = new ArrayList<ActorRef<RideService.RideDetails>>();
                    for(int i=0; i<10; i++){
                        Globals.rideService.add(context.spawn(RideService.create(), "ride"+i));
                    }

                    //context.watch(gabbler1);
                    //context.watch(gabbler2);

                    //cab.tell(new ChatRoom.GetSession("ol’ Gabbler", gabbler1));
                    //cab.tell(new ChatRoom.GetSession("nu’ Gabbler", gabbler2));


                    return Behaviors.empty(); // don't want to receive any more messages

                    // We can return an empty behavior, because we are returning the user-guardian actor, and
                    // normally this actor need not receive any messages.
                });
    }

    public static void main(String[] args) {
        final ActorSystem<Main.Started> main = ActorSystem.create(Main.create(), "CabHailing"); // create "user guardian" actor
    }
}
