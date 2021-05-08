package pods.cabs;

public class CabStatus {
    MajorState majorState;
    MinorState minorState;
    Boolean interested;
    Integer location;

    public CabStatus() {
        this.majorState = MajorState.SignedOut;
        this.minorState = MinorState.NotAvailable;
        this.interested = false;
        this.location = 0;
    }
}
