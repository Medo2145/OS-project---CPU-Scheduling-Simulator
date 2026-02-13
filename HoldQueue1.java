/**
 * HoldQueue1: sorted ascending by requested memory (priority queue).
 */
import java.util.*;
public class HoldQueue1 {
    private final List<Process> list = new ArrayList<>();

    public void enqueue(Process p) {
        int i = 0;
        while (i < list.size() && list.get(i).memoryReq() <= p.memoryReq()) i++;
        list.add(i, p);
    }

    public Process peekIfAdmissible(OtherKerServices svc) {
        if (list.isEmpty()) return null;
        Process p = list.get(0);
        return svc.availableFor(p) ? p : null;
    }

    public void remove(Process p) { list.remove(p); }

    public boolean isEmpty() { return list.isEmpty(); }

    public List<Process> snapshot() { return List.copyOf(list); }

    public void clear() { list.clear(); }
}