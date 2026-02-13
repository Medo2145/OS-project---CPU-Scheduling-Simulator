/**
 * OtherKerServices: handles memory and device allocation (pre-allocation),
 * rejection logic, and release on completion.
 */
public class OtherKerServices {
    private long memorySize = 0;
    private int noDevs = 0;

    private long availMem = 0;
    private int availDevs = 0;

    public void configure(long mem, int devs) {
        this.memorySize = mem;
        this.noDevs = devs;
        this.availMem = mem;
        this.availDevs = devs;
        // DEBUG: System.out.println("[configure] memorySize=" + mem + " devices=" + devs);
    }

    public boolean exceedsTotals(Process p) {
        return p.memoryReq() > memorySize || p.devReq() > noDevs;
    }

    public boolean availableFor(Process p) {
        return p.memoryReq() <= availMem && p.devReq() <= availDevs;
    }

    public void allocateFor(Process p) {
        availMem -= p.memoryReq();
        availDevs -= p.devReq();
        // DEBUG: System.out.println("[allocateFor] PID=" + p.pid() + " availMem=" + availMem + " availDevs=" + availDevs);
    }

    public void releaseFor(Process p) {
        availMem += p.memoryReq();
        availDevs += p.devReq();
        // DEBUG: System.out.println("[releaseFor] PID=" + p.pid() + " availMem=" + availMem + " availDevs=" + availDevs);
    }

    public long memorySize() { return memorySize; }
    public int noDevs() { return noDevs; }
    public long availMem() { return availMem; }
    public int availDevs() { return availDevs; }
}