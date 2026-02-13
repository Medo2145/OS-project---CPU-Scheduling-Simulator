/**
 * Process (PCB) keeping arrival time, burst times, resources, priority, and statistics.
 */
public class Process {
    private final long pid;
    private final long arrivalTime;
    private final long burstTime;
    private long remaining;

    private final int priority;
    private final long memoryReq;
    private final int devReq;

    private ProcessState state = ProcessState.NEW;

    private Long sliceStart = null;
    private Long sliceEndsAt = null;
    private Long completionTime = null;

    public static Process fromArrival(InputParser.External a) {
        return new Process(
                a.jobId(),
                a.time(),
                a.burst(),
                a.priority(),
                a.mem(),
                a.devs()
        );
    }

    public Process(long pid, long arrival, long burst, int prio, long mem, int devs) {
        this.pid = pid;
        this.arrivalTime = arrival;
        this.burstTime = burst;
        this.remaining = burst;
        this.priority = prio;
        this.memoryReq = mem;
        this.devReq = devs;
    }

    // Slice handling
    public void startSliceAt(long now, long quantum) {
        sliceStart = now;
        long run = Math.min(remaining, quantum);
        sliceEndsAt = now + run;
        state = ProcessState.RUNNING;
    }

    public void endSliceAt(long now) {
        if (sliceStart != null) {
            long ran = now - sliceStart;
            remaining = Math.max(0, remaining - ran);
        }
        sliceStart = null;
        sliceEndsAt = null;
        if (remaining <= 0) {
            state = ProcessState.FINISHED;
            completionTime = now;
        } else {
            state = ProcessState.READY;
        }
    }

    public void markCompletedAt(long now) {
        remaining = 0;
        completionTime = now;
        state = ProcessState.FINISHED;
        sliceStart = null;
        sliceEndsAt = now;
    }

    // State transitions for queues
    public void markReady() { state = ProcessState.READY; }
    public void markHold1() { state = ProcessState.HOLD1; }
    public void markHold2() { state = ProcessState.HOLD2; }
    public void markRejected() { state = ProcessState.REJECTED; }

    // Getters
    public long pid() { return pid; }
    public long arrivalTime() { return arrivalTime; }
    public long burstTime() { return burstTime; }
    public long remainingBurst() { return remaining; }
    public int priority() { return priority; }
    public long memoryReq() { return memoryReq; }
    public int devReq() { return devReq; }
    public ProcessState state() { return state; }
    public Long sliceEndsAt() { return sliceEndsAt; }
    public Long sliceStart() { return sliceStart; }
    public Long completionTime() { return completionTime; }

    // Metrics
    public long turnaroundTime() {
        return completionTime == null ? 0 : (completionTime - arrivalTime);
    }

    public long waitingTime() {
        return turnaroundTime() - burstTime;
    }
}