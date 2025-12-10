import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

class SolutionBuilder {
    public static void writeSolutionJson(
            String outputPath,
            TourPlanner.TourResult tourResult,
            Map<Viewpoint, Set<String>> selected,
            double precisionValue,
            int numViewpoints
    ) throws IOException {

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // ===== metadata =====
        sb.append("  \"metadata\": {\n");
        sb.append("    \"num_viewpoints\": ").append(numViewpoints).append(",\n");
        sb.append("    \"objective\": {\n");
        sb.append("      \"distance\": ").append(tourResult.totalDistance).append(",\n");
        sb.append("      \"precision\": ").append(precisionValue).append("\n");
        sb.append("    }\n");
        sb.append("  },\n");

        // ===== sequence =====
        sb.append("  \"sequence\": [\n");

        List<Viewpoint> seq = tourResult.tour;
        for (int i = 0; i < seq.size(); i++) {
            Viewpoint vp = seq.get(i);
            sb.append("    {\n");
            sb.append("      \"id\": \"").append(vp.id).append("\",\n");

            sb.append("      \"angles\": [");
            Set<String> angles = selected.get(vp);

            if (angles != null && !angles.isEmpty()) {
                int c = 0;
                for (String a : angles) {
                    if (c > 0) sb.append(", ");
                    sb.append("\"").append(a).append("\"");
                    c++;
                }
            }
            sb.append("]\n");

            sb.append("    }");
            if (i != seq.size() - 1) sb.append(",");
            sb.append("\n");
        }

        sb.append("  ]\n");
        sb.append("}\n");

        // ===== 写入文件 =====
        try (FileWriter fw = new FileWriter(outputPath, false)) {
            fw.write(sb.toString());
        }
    }
}