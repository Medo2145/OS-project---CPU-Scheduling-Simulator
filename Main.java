/**
 * Entry point: runs the simulation using the fixed input path and output file.
 */
public class Main {
    // Adjust if you want to pass via args.

    private static final String INPUT_PATH = "inputSRR.txt";
    private static final String OUTPUT_PATH = "system_outputs\\outputSRR.txt";

    public static void main(String[] args) {
        try {
            SimulationController controller = new SimulationController();
                controller.main(new String[]{INPUT_PATH, OUTPUT_PATH});
            }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}