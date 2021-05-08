package pods.cabs;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;

public class Wallet extends AbstractBehavior<Wallet.WalletCommands> {

    int walletAmount;

    public Wallet(ActorContext<WalletCommands> context) {
        super(context);
    }

    public static Behavior<WalletCommands> create() {
        return Behaviors.setup(Wallet::new);
    }

    interface WalletCommands {}

    public static final class InitWallet implements WalletCommands {
        public final int amount;
        public InitWallet(int amount) {
            this.amount = amount;
        }
    }

    public static final class GetBalance implements WalletCommands {
        public final ActorRef<Wallet.ResponseBalance> replyTo;
        public GetBalance(ActorRef<Wallet.ResponseBalance> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static final class DeductBalance implements WalletCommands {
        public final int toDeduct;
        public final ActorRef<Wallet.ResponseBalance> replyTo;
        public DeductBalance(int toDeduct, ActorRef<Wallet.ResponseBalance> replyTo) {
            this.toDeduct = toDeduct;
            this.replyTo = replyTo;
        }
    }

    public static final class AddBalance implements WalletCommands {
        public final int toAdd;
        public AddBalance(int toAdd) {
            this.toAdd = toAdd;
        }
    }

    public static final class Reset implements WalletCommands {
        public final ActorRef<Wallet.ResponseBalance> replyTo;
        public Reset(ActorRef<Wallet.ResponseBalance> replyTo) {
            this.replyTo = replyTo;
        }
    }


    public static final class ResponseBalance {
        public final int walletBalance;
        public final int deducted;
        public final String cabId;
        public ResponseBalance(int walletBalance, int deducted, String cabId) {
            this.walletBalance = walletBalance;
            this.deducted = deducted;
            this.cabId = cabId;
        }
    }

    @Override
    public Receive<WalletCommands> createReceive() {
        return newReceiveBuilder()
                .onMessage(DeductBalance.class, this::onDeductBalance)
                .onMessage(AddBalance.class, this::onAddBalance)
                .onMessage(GetBalance.class, this::onGetBalance)
                .onMessage(Reset.class, this::onReset)
                .onMessage(InitWallet.class, this::onInitWallet)
                .build();
    }

    public Behavior<WalletCommands> onInitWallet(InitWallet command) {

        if(command.amount >= 0) this.walletAmount = command.amount;

        return this;
    }

    public Behavior<WalletCommands> onReset(Reset command) {

        for(String custID: Globals.wallets.keySet()) {
            ActorRef<Wallet.WalletCommands> wallet = Globals.wallets.get(custID);
            wallet.tell(new InitWallet(Globals.initialBalance));
        }

        return this;
    }

    private Behavior<WalletCommands> onGetBalance(GetBalance command) {

        command.replyTo.tell(new ResponseBalance(this.walletAmount, 0, ""));

        return this;
    }

    private Behavior<WalletCommands> onAddBalance(AddBalance command) {

        if(command.toAdd >= 0)
            this.walletAmount += command.toAdd;

        return this;
    }

    private Behavior<WalletCommands> onDeductBalance(DeductBalance command) {

        int responseAmount = -1;

        if(command.toDeduct >= 0 && command.toDeduct <= this.walletAmount) {
            this.walletAmount -= command.toDeduct;
            responseAmount = this.walletAmount;

            command.replyTo.tell(new ResponseBalance(responseAmount, command.toDeduct, ""));
        }

        command.replyTo.tell(new ResponseBalance(responseAmount, 0, ""));

        return this;
    }

}
