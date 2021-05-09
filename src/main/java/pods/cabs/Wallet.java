package pods.cabs;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;

public class Wallet extends AbstractBehavior<Wallet.Command> {

    int walletAmount;

    /**
     * Constructor
     */
    public Wallet(ActorContext<Command> context) {
        super(context);
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(Wallet::new);
    }

    interface Command {}

    /**
     * this is the initWallet class which implements the Command interface
     * and constructor takes an integer as amount and initializes the
     * costumer's wallet with that amount.
     */
    public static final class InitWallet implements Command {
        public final int amount;
        public InitWallet(int amount) {
            this.amount = amount;
        }
    }

    /**
     * this is the getBalance class which implements the Command interface
     * and constructor takes as Wallet actor to reply back
     */
    public static final class GetBalance implements Command {
        public final ActorRef<Wallet.ResponseBalance> replyTo;
        public GetBalance(ActorRef<Wallet.ResponseBalance> replyTo) {
            this.replyTo = replyTo;
        }
    }

    /**
     * this is the deductBalance class which implements the Command interface
     * and constructor takes an integer which is tho be deducted from costumer's
     * wallet as well as take an wallet actor to respond back.
     */
    public static final class DeductBalance implements Command {
        public int toDeduct;
        public final ActorRef<Wallet.ResponseBalance> replyTo;
        public DeductBalance(int toDeduct, ActorRef<Wallet.ResponseBalance> replyTo) {
            this.toDeduct = toDeduct;
            this.replyTo = replyTo;
        }
    }

    /**
     * this is the addBalance class which implements the Command interface
     * and constructor takes an integer which is to be added
     */
    public static final class AddBalance implements Command {
        public final int toAdd;
        public AddBalance(int toAdd) {
            this.toAdd = toAdd;
        }
    }

    /**
     * Reset class and implements the Command interface
     * Constructor takes a Wallet Actor and save it in the replyTo actor
     */
    public static final class Reset implements Command {
        public final ActorRef<Wallet.ResponseBalance> replyTo;
        public Reset(ActorRef<Wallet.ResponseBalance> replyTo) {
            this.replyTo = replyTo;
        }
    }

    /**
     * This is class for responding the wallet balance
     * constructor put the balance in the costumer's wallet
     */
    public static final class ResponseBalance {
        public final int walletBalance;
        public ResponseBalance(int walletBalance) {
            this.walletBalance = walletBalance;
        }
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(DeductBalance.class, this::onDeductBalance)
                .onMessage(AddBalance.class, this::onAddBalance)
                .onMessage(GetBalance.class, this::onGetBalance)
                .onMessage(Reset.class, this::onReset)
                .onMessage(InitWallet.class, this::onInitWallet)
                .build();
    }

    /**
     * This function is called when we want to initialize
     * the amount in costumer's wallet.
     * @param command given to wallet actor
     * @return Actor behavior
     */
    public Behavior<Command> onInitWallet(InitWallet command) {

        if(command.amount >= 0) this.walletAmount = command.amount;

        return this;
    }

    /**
     * This function is called when we want to reset the wallet's of the costumer
     * @param command given to wallet actor
     * @return actor behavior
     */
    public Behavior<Command> onReset(Reset command) {

        for(String custID: Globals.wallets.keySet()) {
            ActorRef<Command> wallet = Globals.wallets.get(custID);
            wallet.tell(new InitWallet(Globals.initialBalance));
        }
        command.replyTo.tell(new ResponseBalance(Globals.initialBalance));

        return this;
    }

    /**
     * This function is called when we want to know the amount
     * @param command given to wallet actor
     * @return Actor behavior
     */
    private Behavior<Command> onGetBalance(GetBalance command) {

        int amount = this.walletAmount;
        ResponseBalance resp = new ResponseBalance(amount);
        System.out.println(resp.walletBalance);
        command.replyTo.tell(resp);

        return this;
    }

    /**
     * This function is called when we want to add the amount
     * @param command given to wallet actor
     * @return Actor behavior
     */
    private Behavior<Command> onAddBalance(AddBalance command) {

        if(command.toAdd >= 0) {
            this.walletAmount += command.toAdd;
        }

        return this;
    }

    /**
     * This function is called when we want to deduct the amount
     * @param command given to wallet actor
     * @return Actor behavior
     */
    private Behavior<Command> onDeductBalance(DeductBalance command)  {

        int responseAmount = -1;

        if(command.toDeduct >= 0 && command.toDeduct <= this.walletAmount) {
            this.walletAmount -= command.toDeduct;
            responseAmount = this.walletAmount;
            command.toDeduct = -1;
            command.replyTo.tell(new ResponseBalance(responseAmount));
        }
        else {
            command.replyTo.tell(new ResponseBalance(responseAmount));
        }

        return this;
    }

}
