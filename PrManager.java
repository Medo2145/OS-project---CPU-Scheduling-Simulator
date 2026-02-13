import java.util.Optional;


public class PrManager {

    private long internalClock = 0L; // Simulation clock as seen by the process manager

    // Queues managed by the OS
    private final SubmitQueue submitQ = new SubmitQueue();
    private final HoldQueue1 hq1 = new HoldQueue1();
    private final HoldQueue2 hq2 = new HoldQueue2();
    private final ReadyQueue readyQ = new ReadyQueue();

    // DRR metrics (globals for reporting/debug):
    // SR = sum of remaining bursts in Ready (excludes running)
    // AR = rounded average remaining burst in Ready (excludes running)
    private long SR = 0L;
    private long AR = 0L;

    // Other kernel services (memory, devices, etc.)
    private final OtherKerServices otherKerServices = new OtherKerServices();

    // Scheduler:
    //  - DRoundRobinScheduler for Dynamic RR (quantum computed here in PrManager)
    //  - SRoundRobinScheduler for Static RR (Q = 10 + teamNumber)
    private Scheduler scheduler = new SRoundRobinScheduler(10 + 7); // default: team 7 → Q = 17

    // State of the currently running process
    private Process running = null;
    private Long nextInternalTime = null; // time of next internal event (slice end or completion); null = "infinity"

    // Last quantum used for the currently running process (for debugging if needed)
    private long quantumTime;

    /**
     * Handle internal events when the simulation clock reaches an internal event time:
     *  - either the current slice ends
     *  - or the running process finishes.
     */
    public void handleInternalAt(long now) {
        internalClock = now;

        if (running == null) {
            return; // nothing to do; no process is currently on CPU
        }

        Long sliceEnd = running.sliceEndsAt();
        if (sliceEnd != null && sliceEnd == now) {
            long ran = now - (running.sliceStart() == null ? now : running.sliceStart());
            long remaining = running.remainingBurst();

            // If the slice consumed all remaining burst time, the job is done.
            if (remaining - ran <= 0) {
                completeRunning(now);
            } else {
                // Otherwise, its quantum expired and it must be preempted.
                preemptRunning(now);
            }
        }

        // After resolving the internal event, try to admit new jobs and dispatch again.
        admitFromHoldQueues();
        dispatchIfIdle(now);
    }

    /**
     * Handle CONFIG command: reset the system and choose the scheduler.
     */
    public void onConfig(InputParser.External cfg, long now) {
        internalClock = now;

        long mem   = cfg.fields().getOrDefault("M", 0L);
        int  devs  = cfg.fields().getOrDefault("S", 0L).intValue();
        int  sched = cfg.fields().getOrDefault("SCHED", 2L).intValue();

        otherKerServices.configure(mem, devs);
        clearAllQueuesAndRunning();
        FinishedRecorder.clear();

        // Select the scheduler based on SCHED field
        switch (sched) {
            case 1 -> scheduler = new DRoundRobinScheduler();        // Dynamic RR (quantum computed in PrManager)
            case 2 -> scheduler = new SRoundRobinScheduler(10 + 7);  // Static RR, team 7 → Q = 17
            default -> scheduler = new SRoundRobinScheduler(10 + 7);
        }

        // Reset SR/AR whenever we reconfigure
        SR = 0L;
        AR = 0L;
    }

    /**
     * Handle external arrival of a new job.
     */
    public void onArrival(InputParser.External arr, long now) {
        internalClock = now;
        Process p = Process.fromArrival(arr);

        // Reject jobs that exceed total system resources
        if (otherKerServices.exceedsTotals(p)) {
            p.markRejected();
            return;
        }

        // If resources are available, admit directly to Ready; otherwise hold.
        if (otherKerServices.availableFor(p)) {
            otherKerServices.allocateFor(p);
            p.markReady();
            readyQ.enqueue(p);

            // DRR: update SR/AR whenever Ready changes
            if (scheduler instanceof DRoundRobinScheduler) {
                updateSRandAR();
            }
        }
        else {
            if (p.priority() == 1) {
                p.markHold1();
                hq1.enqueue(p);
            } else {
                p.markHold2();
                hq2.enqueue(p);
            }
        }

        // If CPU is idle, try to load the next process immediately.
        dispatchIfIdle(now);
    }

    /**
     * Next internal decision time for the simulation driver.
     */
    public long getNextDecisionTime() {
        return nextInternalTime == null ? Long.MAX_VALUE : nextInternalTime;
    }

    /**
     * ID of the currently running process (or -1 if none).
     */
    public long getRunningProcId() {
        return running == null ? -1 : running.pid();
    }

    public Long nextInternalTimeOrNull() {
        return nextInternalTime;
    }

    // ----------------------------------------------------------------------
    // Core helpers
    // ----------------------------------------------------------------------

    /**
     * Dispatch a process onto the CPU if there is no running process.
     * Decides the time quantum according to the active scheduler.

     * SR/AR globals are updated to reflect the state at dispatch time for visibility.
     */
    private void dispatchIfIdle(long now) {
        if (running != null) return;

        Optional<Process> next = scheduler.selectNextProcess(readyQ);

        if (next.isEmpty()) {
            // No ready processes; no internal event scheduled.
            nextInternalTime = null;
            return;
        }
        running = next.get();

        if (scheduler instanceof DRoundRobinScheduler) {
            // Dynamic RR: compute quantum here
            if (readyQ.isEmpty()) {
                // Running process is the ONLY ready-to-run process
                quantumTime = Math.max(1L, running.remainingBurst());

                // Track SR/AR at dispatch for visibility
                SR = running.remainingBurst();
                AR = running.remainingBurst();
            } else {
                long sum = running.remainingBurst();
                int count = 1;

                for (Process p : readyQ.snapshot()) {
                    sum += p.remainingBurst();
                    count++;
                }

                long avgRounded = Math.max(1L, Math.round((double) sum / count));
                quantumTime = avgRounded;

                // Track SR/AR at dispatch (SR includes running here for clarity)
                SR = sum;
                AR = avgRounded;
            }
        }
        else {
            // Static RR: use scheduler's fixed quantum
            quantumTime = scheduler.timeQuantumFor(running, readyQ);
        }

        running.startSliceAt(now, quantumTime);
        nextInternalTime = running.sliceEndsAt();
    }

    /**
     * Recalculate SR and AR for DRR based on the current system state.
     * This counts only processes currently in the Ready queue (not running).
     * AR is rounded to nearest int to be consistent with dispatch quantums.
     */
    private void updateSRandAR() {
        if (!(scheduler instanceof DRoundRobinScheduler)) {
            SR = 0L;
            AR = 0L;
            return;
        }

        long sum = 0L;
        int count = 0;

        for (Process p : readyQ.snapshot()) {
            sum += p.remainingBurst();
            count++;
        }

        SR = sum;
        if (count == 0) {
            AR = 0L;
        } else {
            AR = Math.max(1L, Math.round((double) SR / count));
        }
    }

    /**
     * Finish the currently running process and free its resources.
     */
    private void completeRunning(long now) {
        otherKerServices.releaseFor(running);
        running.markCompletedAt(now);
        FinishedRecorder.record(running);
        running = null;
        nextInternalTime = null;

        // DRR rule: after CPU finishes a job, update SR and AR.
        if (scheduler instanceof DRoundRobinScheduler) {
            updateSRandAR();
        }
    }

    /**
     * Preempt the running process at quantum expiry and return it to Ready.
     */
    private void preemptRunning(long now) {
        running.endSliceAt(now);
        readyQ.enqueue(running);
        running = null;
        nextInternalTime = null;

        // DRR: update SR/AR after Ready changes
        if (scheduler instanceof DRoundRobinScheduler) {
            updateSRandAR();
        }
    }

    /**
     * Admit processes from hold queues into Ready when resources become available.
     * For DRR, every admission into Ready must refresh SR and AR.
     */
    private void admitFromHoldQueues() {
        boolean admitted = true;
        while (admitted) {
            admitted = false;

            Process p = hq1.peekIfAdmissible(otherKerServices);
            if (p != null) {
                hq1.remove(p);
                otherKerServices.allocateFor(p);
                p.markReady();
                readyQ.enqueue(p);
                admitted = true;

                if (scheduler instanceof DRoundRobinScheduler) {
                    updateSRandAR();
                }
                continue;
            }

            Process q = hq2.peekIfAdmissible(otherKerServices);
            if (q != null) {
                hq2.dequeue();
                otherKerServices.allocateFor(q);
                q.markReady();
                readyQ.enqueue(q);
                admitted = true;

                if (scheduler instanceof DRoundRobinScheduler) {
                    updateSRandAR();
                }
            }
        }
    }

    /**
     * Reset all queues and the currently running process.
     */
    private void clearAllQueuesAndRunning() {
        submitQ.clear();
        hq1.clear();
        hq2.clear();
        readyQ.clear();
        running = null;
        nextInternalTime = null;
        SR = 0L;
        AR = 0L;
    }

    // ----------------------------------------------------------------------
    // Accessors for DisplayFormatter and debugging
    // ----------------------------------------------------------------------

    public SubmitQueue submitQ() { return submitQ; }
    public HoldQueue1 hq1()      { return hq1; }
    public HoldQueue2 hq2()      { return hq2; }
    public ReadyQueue readyQ()   { return readyQ; }
    public OtherKerServices services() { return otherKerServices; }
    public Process running()     { return running; }

    // Helpful getters if you want to print SR/AR for DRR debugging
    public long getSR() { return SR; }
    public long getAR() { return AR; }
}