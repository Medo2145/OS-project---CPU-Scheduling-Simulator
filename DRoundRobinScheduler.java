import java.util.Optional;

/**
 * Dynamic Round Robin:
 * NOTE: timeQuantumFor is intentionally NOT used for DRR.
 * Quantum is computed inside PrManager.dispatchIfIdle().
 */
public class DRoundRobinScheduler implements Scheduler {

    @Override
    public Optional<Process> selectNextProcess(ReadyQueue rq) {
        return rq.dequeue();
    }

    // Unused for DRR (Static RR uses its own timeQuantumFor).
    @Override
    public long timeQuantumFor(Process candidate, ReadyQueue rq) {
        return 0L; // unused stub by design
    }
}