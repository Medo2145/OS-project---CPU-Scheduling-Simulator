import java.util.ArrayList;
import java.util.List;

/**
 * FinishedRecorder: central place to store completed processes.
 * If you prefer, you can move this into PrManager as a member list.
 */
public class FinishedRecorder {
    private static final List<Process> FINISHED = new ArrayList<>();

    public static void record(Process p) {
        FINISHED.add(p);
    }

    public static List<Process> snapshot() {
        return List.copyOf(FINISHED);
    }

    public static void clear() {
        FINISHED.clear();
    }
}