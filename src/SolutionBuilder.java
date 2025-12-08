import java.util.*;

class SolutionBuilder {
    static String buildSolution(
            List<SamplePoint> samples,
            List<Viewpoint> tour,
            List<Viewpoint> viewPoints,
            Map<Viewpoint, Set<String>> selected,
            double[][] dist,
            double lambda
    ) {
        // build vp -> Index map
        Map<Viewpoint, Integer> vpIndexMap = new HashMap<>();
        for (int i = 0; i < viewPoints.size(); i++) {
            vpIndexMap.put(viewPoints.get(i), i);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        // metadata
        // get precision
        double totalPrecision = 0;
        // vpId -> vp
        Map<String, Viewpoint> vpMap = new HashMap<>();
        for (Viewpoint vp : selected.keySet()) vpMap.put(vp.id, vp);

        for (SamplePoint sp : samples) {

            List<Double> usable = new ArrayList<>();

            if (sp.coveringPairs != null) {
                for (CoveringPair cp : sp.coveringPairs) {
                    Viewpoint vp = vpMap.get(cp.viewpointId);
                    if (vp != null && selected.get(vp).contains(cp.directionId)) {
                        double p = vp.precision.get(cp.directionId);
                        usable.add(p);
                    }
                }
            }

            if (usable.isEmpty()) continue;

            usable.sort((a,b)->Double.compare(b,a)); // 降序

            // 覆盖要求：前 3 个
            for (int i=0; i<Math.min(3, usable.size()); i++) {
                if (usable.get(i) > 0)
                    totalPrecision += usable.get(i);
            }

            // 剩下的正精度方向=额外加分
            for (int i=3; i<usable.size(); i++) {
                if (usable.get(i) > 0)
                    totalPrecision += usable.get(i);
            }
        }

        // get distance
        double totalDistance = 0;
        if (tour.size() > 1) {
            for (int i = 0; i < tour.size() - 1; i++) {
                totalDistance += dist[vpIndexMap.get(tour.get(i))][vpIndexMap.get(tour.get(i + 1))];
            }
        }
        sb.append("  \"metadata\": {\n");
        sb.append("    \"num_viewpoints\": ").append(selected.size()).append(",\n");
        sb.append("    \"objective\": {\n");
        sb.append("      \"distance\": ").append(totalDistance).append(",\n");
        sb.append("      \"precision\": ").append(totalPrecision).append("\n");
        sb.append("    }\n");
        sb.append("  },\n");

        // sequence
        int tourSize = tour.size();
        int pathLen; // 实际用于统计和输出的路径长度

        if (tourSize >= 2 && tour.get(0) == tour.get(tourSize - 1)) {
            // 首尾是同一个 viewpoint（闭环），输出和统计时去掉最后一个
            pathLen = tourSize - 1;
        } else {
            pathLen = tourSize;
        }
        sb.append("  \"sequence\": [\n");

        for (int i = 0; i < pathLen; i++) {
            Viewpoint vp = tour.get(i);

            // 这个 viewpoint 选了哪些角度，如果没选，给空数组
            Set<String> dirs = selected.get(vp);
            if (dirs == null) dirs = Collections.emptySet();

            sb.append("    {\n");
            sb.append("      \"id\": \"").append(vp.id).append("\",\n");
            sb.append("      \"angles\": [");

            int k = 0;
            int K = dirs.size();
            for (String d : dirs) {
                sb.append("\"").append(d).append("\"");
                if (++k < K) sb.append(", ");
            }

            sb.append("]\n");
            sb.append("    }");

            if (i + 1 < pathLen) sb.append(",");
            sb.append("\n");
        }

        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }
}
