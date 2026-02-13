import java.util.Optional;

/**
 * Static Round Robin: fixed quantum = 10 + TeamNumber.
 */
public class SRoundRobinScheduler implements Scheduler {
    private final long fixedQuantum;

    public SRoundRobinScheduler(long fixedQuantum) {
        this.fixedQuantum = fixedQuantum;
    }

    @Override
    public Optional<Process> selectNextProcess(ReadyQueue rq) {
        return rq.dequeue();
    }

    @Override
    public long timeQuantumFor(Process candidate, ReadyQueue rq) {
        return fixedQuantum;
    }
}

