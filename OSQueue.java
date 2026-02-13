import java.util.*;

/**
 * Base FIFO queue abstraction.
 */
public abstract class OSQueue {
    protected final Deque<Process> dq = new ArrayDeque<>();

    public void enqueue(Process p) { dq.addLast(p); }

    public Optional<Process> dequeue() {
        return Optional.ofNullable(dq.pollFirst());
    }

    public boolean isEmpty() { return dq.isEmpty(); }

    public List<Process> snapshot() { return List.copyOf(dq); }

    public int size() { return dq.size(); }

    public void clear() { dq.clear(); }
}