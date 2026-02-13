/**
 * Scheduler interface: unified selection & quantum retrieval.
 */
import java.util.Optional;
public interface Scheduler {
    Optional<Process> selectNextProcess(ReadyQueue rq);
    long timeQuantumFor(Process candidate, ReadyQueue rq);
}