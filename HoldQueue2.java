/**
 * HoldQueue2: simple FIFO for lower-priority processes.
 */
public class HoldQueue2 extends OSQueue {

    public Process peekIfAdmissible(OtherKerServices svc) {
        Process p = dq.peekFirst();
        if (p == null) return null;
        return svc.availableFor(p) ? p : null;
    }
}