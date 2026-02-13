import java.io.PrintWriter;
import java.util.List;

/**
 * DisplayFormatter: prints system snapshots and final statistics.
 * Matches the style of your sample outputs (simplified).
 */
public class DisplayFormatter {

    public void dumpSnapshot(PrintWriter out, long time, PrManager pm) {
        out.println("-------------------------------------------------------");
        out.println("System Status:                                         ");
        out.println("-------------------------------------------------------");
        out.printf("          Time: %.2f%n", (double) time);
        out.printf("  Total Memory: %d%n", pm.services().memorySize());
        out.printf(" Avail. Memory: %d%n", pm.services().availMem());
        out.printf(" Total Devices: %d%n", pm.services().noDevs());
        out.printf("Avail. Devices: %d%n%n", pm.services().availDevs());

        // Ready List
        out.println("Jobs in Ready List                                      ");
        out.println("--------------------------------------------------------");
        if (pm.readyQ().isEmpty()) {
            out.println("  EMPTY\n");
        } else {
            for (Process p : pm.readyQ().snapshot()) {
                out.printf("Job ID %d , %.2f Cycles left to completion.%n", p.pid(), (double) p.remainingBurst());
            }
            out.println();
        }

        // Long Job List (SubmitQueue)
        out.println("Jobs in Long Job List                                   ");
        out.println("--------------------------------------------------------");
        if (pm.submitQ().isEmpty()) {
            out.println("  EMPTY\n");
        } else {
            for (Process p : pm.submitQ().snapshot()) {
                out.printf("Job ID %d , %.2f Cycles left to completion.%n", p.pid(), (double) p.remainingBurst());
            }
            out.println();
        }

        // Hold Queue 1
        out.println("Jobs in Hold List 1                                     ");
        out.println("--------------------------------------------------------");
        if (pm.hq1().isEmpty()) {
            out.println("  EMPTY\n");
        } else {
            for (Process p : pm.hq1().snapshot()) {
                out.printf("Job ID %d , %.2f Cycles left to completion.%n", p.pid(), (double) p.remainingBurst());
            }
            out.println();
        }

        // Hold Queue 2
        out.println("Jobs in Hold List 2                                     ");
        out.println("--------------------------------------------------------");
        if (pm.hq2().isEmpty()) {
            out.println("  EMPTY\n");
        } else {
            for (Process p : pm.hq2().snapshot()) {
                out.printf("Job ID %d , %.2f Cycles left to completion.%n", p.pid(), (double) p.remainingBurst());
            }
            out.println();
        }

        // Finished Jobs
        out.println();
        out.println("Finished Jobs (detailed)                                ");
        out.println("--------------------------------------------------------");
        out.println("  Job    ArrivalTime     CompleteTime     TurnaroundTime    WaitingTime");
        out.println("------------------------------------------------------------------------");

        List<Process> allFinished = FinishedRecorder.snapshot();
        if (allFinished.isEmpty()) {
            out.println("  EMPTY");
        } else {
            for (Process p : allFinished) {
                out.printf("  %-6d %-14.2f %-16.2f %-17.2f %-13.2f%n",
                        p.pid(),
                        (double) p.arrivalTime(),
                        p.completionTime() == null ? 0.0 : (double) p.completionTime(),
                        (double) p.turnaroundTime(),
                        (double) p.waitingTime());
            }
        }
        out.printf("Total Finished Jobs:             %d%n%n%n", allFinished.size());
    }

    public void dumpFinal(PrintWriter out, long time, PrManager pm) {
        out.println("--- Simulation finished at time " + time + ".00 ---");
    }
}