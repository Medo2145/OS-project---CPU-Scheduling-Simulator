import java.io.*;
import java.util.*;

/**
 * InputParser: reads one external command (CONFIG/ARRIVAL/DISPLAY) at a time.
 * Format examples:
 *   C 2 M=100 S=1 SCHED=2
 *   A 3 J=1 M=50 S=0 R=14 P=2
 *   D 16
 */
public class InputParser {

    public enum Kind { CONFIG, ARRIVAL, DISPLAY }

    public static class External {
        private final Kind kind;
        private final long time;
        private final Map<String, Long> fields;
        private final String raw;

        public External(Kind kind, long time, Map<String, Long> fields, String raw) {
            this.kind = kind;
            this.time = time;
            this.fields = fields;
            this.raw = raw;
        }

        public Kind kind() { return kind; }
        public long time() { return time; }
        public Map<String, Long> fields() { return fields; }
        public String raw() { return raw; }

        public long jobId() { return fields.getOrDefault("J", -1L); }
        public long mem() { return fields.getOrDefault("M", 0L); }
        public long burst() { return fields.getOrDefault("R", 0L); }
        public int priority() { return fields.getOrDefault("P", 0L).intValue(); }
        public int devs() { return fields.getOrDefault("S", 0L).intValue(); }
        public boolean isFinalDisplay() { return time >= 999999L; }
    }

    public External readNextExternalOrNone(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            char first = line.charAt(0);
            Kind kind;
            if (first == 'C') kind = Kind.CONFIG;
            else if (first == 'A') kind = Kind.ARRIVAL;
            else if (first == 'D') kind = Kind.DISPLAY;
            else continue; // skip unknown

            String[] parts = line.split("\\s+");
            if (parts.length < 2) continue;
            long t = parseLong(parts[1]);

            Map<String, Long> map = new HashMap<>();
            for (int i = 2; i < parts.length; i++) {
                String token = parts[i];
                if (!token.contains("=")) continue;
                String[] kv = token.split("=");
                if (kv.length == 2) {
                    map.put(kv[0], parseLong(kv[1]));
                }
            }
            return new External(kind, t, map, line);
        }
        return null;
    }

    private long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}