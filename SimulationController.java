import java.io.*;

/**
 * SimulationController: handles high-level loop, file IO, and statistics display.
 * UML methods: sysGen(), displayFinalStatistics(), main(), parseCmd(String)
 */
public class SimulationController {

    private long currentTime = 0L;        // UML: currentTime
    private String inputFile;             // UML: inputFile
    private String outputFile;            // UML: outputFile

    private final PrManager prManager = new PrManager();
    private final DisplayFormatter formatter = new DisplayFormatter();
    private final InputParser parser = new InputParser();

    /**
     * Main simulation entry (maps to UML main()).
     * args[0] = input path, args[1] = output path.
     */
    public void main(String[] args) throws Exception {
        this.inputFile = args.length > 0 ? args[0] : "input.txt";
        this.outputFile = args.length > 1 ? args[1] : "output.txt";

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile));
             PrintWriter out = new PrintWriter(new FileWriter(outputFile))) {

            InputParser.External next = parser.readNextExternalOrNone(br); // one-line lookahead
            Long nextInternal = null; // time of next internal event (null => infinity)

            // DEBUG: System.out.println("Start simulation loop");

            while (true) {
                long i = (next == null) ? Long.MAX_VALUE : next.time();          // next external
                long e = (nextInternal == null) ? Long.MAX_VALUE : nextInternal; // next internal
                long T = Math.min(i, e);

                if (T == Long.MAX_VALUE) {
                    // DEBUG: System.out.println("No more events, exiting loop");
                    break;
                }

                // Jump time directly
                currentTime = T;
                // DEBUG: System.out.println("[TIME JUMP] currentTime=" + currentTime + " i=" + i + " e=" + e);

                // Internal first if tie or if internal precedes external
                if (e <= i) {
                    prManager.handleInternalAt(currentTime);
                    nextInternal = prManager.nextInternalTimeOrNull();
                    // DEBUG: System.out.println("[INTERNAL] processed at time " + currentTime + ", nextInternal=" + nextInternal);
                }

                // External event (if its timestamp equals currentTime)
                if (next != null && next.time() == currentTime) {
                    switch (next.kind()) {
                        case CONFIG -> {
                            // DEBUG: System.out.println("[CONFIG] line=" + next.raw());
                            sysGen(next.raw());
                            prManager.onConfig(next, currentTime);
                            nextInternal = prManager.nextInternalTimeOrNull();
                        }
                        case ARRIVAL -> {
                            // DEBUG: System.out.println("[ARRIVAL] line=" + next.raw());
                            prManager.onArrival(next, currentTime);
                            nextInternal = prManager.nextInternalTimeOrNull();
                        }
                        case DISPLAY -> {
                            // DEBUG: System.out.println("[DISPLAY] line=" + next.raw());
                            formatter.dumpSnapshot(out, currentTime, prManager);
                            if (next.isFinalDisplay()) {
                                // Final display does NOT stop internal processes automatically; they are done.
                                // DEBUG: System.out.println("[DISPLAY] Final display encountered.");
                            }
                        }
                    }
                    // Consume external and read next
                    next = parser.readNextExternalOrNone(br);
                }
            }

            displayFinalStatistics(out);
        }
    }

    // UML: sysGen()
    public void sysGen() {
        // Provided for completeness if needed elsewhere
    }

    // Overload taking raw config line
    public void sysGen(String line) {
        // Parse config banner here if needed. Already handled in PrManager.onConfig.
        // DEBUG: System.out.println("[sysGen] raw config line: " + line);
    }

    // UML: displayFinalStatistics()
    public void displayFinalStatistics(PrintWriter out) {
        formatter.dumpFinal(out, currentTime, prManager);
    }

    public void displayFinalStatistics() {
        // Could print to stdout if needed
    }

    // UML: parseCmd(line) - not strictly used but included
    public void parseCmd(String line) {
        // You could parse single-line commands outside main loop if needed.
    }
}