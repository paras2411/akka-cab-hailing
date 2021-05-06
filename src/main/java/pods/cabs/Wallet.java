package pods.cabs;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;

public class Wallet extends AbstractBehavior<Wallet.WalletCommands> {

    public static Integer walletAmount;



    public Wallet(ActorContext<WalletCommands> context) {
        super(context);
    }

    public static Behavior<WalletCommands> create() {
        return Behaviors.setup(Wallet::new);
    }

    public static final class WalletDetails {
//        public final String CabId;
//        public final ActorRef<Greeted> replyTo;

//        public Greet(String whom, ActorRef<Greeted> replyTo) {
//            this.whom = whom;
//            this.replyTo = replyTo;
//        }
    }
    interface WalletCommands {}

    public static final class InitWallet implements WalletCommands {
        public static int pp;
        public InitWallet(int amount) {
            walletAmount = amount;
            pp++;
        }
    }

    @Override
    public Receive<WalletCommands> createReceive() {
        return newReceiveBuilder().onMessage(InitWallet.class, this::onSayHello).build();
    }

    private Behavior<WalletCommands> onSayHello(InitWallet command) {
        //#create-actors
        //#create-actors
        System.out.println(command.pp);
        return this;
    }
}
