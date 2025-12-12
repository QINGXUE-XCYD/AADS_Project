import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

// =============Data Structure==============
// Viewpoint
class Viewpoint {
    // Just id
    String id;
    // Index in the graph
    int index;
    // Where the tour begins and ends
    boolean isMandatory;
    // Coordinates
    double x, y, z;
    // Precision of different angles
    Map<String, Double> precision;

    // Constructor
    Viewpoint(String id, boolean isMandatory, double x, double y, double z, Map<String, Double> precision) {
        this.id = id;
        this.index = Integer.parseInt(id.substring(1)) - 1;
        this.isMandatory = isMandatory;
        this.x = x;
        this.y = y;
        this.z = z;
        this.precision = precision;
    }

    // toString
    @Override
    public String toString() {
        return "id: " + id + ", isMandatory: " + isMandatory + ", x: " + x + ", y: " + y + ", z: " + z + ", precision: " + precision;
    }
}

// CoveringPair
class CoveringPair {
    // Id of viewpoint
    String viewpointId;
    // Id of the direction
    String directionId;

    // Constructor
    CoveringPair(String viewpointId, String directionId) {
        this.viewpointId = viewpointId;
        this.directionId = directionId;
    }

    // toString
    @Override
    public String toString() {
        return "viewpointId: " + viewpointId + ", directionId: " + directionId;
    }
}

// Direction
class Direction {
    // Id of the direction(angle)
    String Id;
    // Angle of the direction
    double x, y, z;

    // Constructor
    Direction(String id, double x, double y, double z) {
        this.Id = id;
        this.x = x;
        this.y = y;
        this.z = z;
    }

//    // Get direction
//    double[] getDirection() {
//        return new double[]{x, y, z};
//    }

    // toString
    @Override
    public String toString() {
        return "id: " + Id + ", x: " + x + ", y: " + y + ", z: " + z;
    }
}

// SamplePoint
class SamplePoint {
    // Just id
    String id;
    // Coordinates
    double x, y, z;
    // Sets of feasible viewpoint-direction pairs
    List<CoveringPair> coveringPairs;

//    // Constructor
//    SamplePoint(String id, double x, double y, double z, List<CoveringPair> coveringPairs) {
//        this.id = id;
//        this.x = x;
//        this.y = y;
//        this.z = z;
//        this.coveringPairs = coveringPairs;
//    }

    SamplePoint(String id, double x, double y, double z) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.coveringPairs = new ArrayList<>();
    }

    public void addCoveringPair(String vpId, String directionId) {
        this.coveringPairs.add(new CoveringPair(vpId, directionId));
    }

    // toString
    @Override
    public String toString() {
        return "id: " + id + ", x: " + x + ", y: " + y + ", z: " + z + ", coveringPairs: " + coveringPairs;
    }
}

// InputData
class InputData {
    // Input data for the algorithm
    List<Viewpoint> viewpoints;
    List<SamplePoint> samplePoints;
    List<Direction> directions;
    int[][] collisionMatrix;
    double lambda;

    // Constructor
    InputData(List<Viewpoint> viewpoints, List<SamplePoint> samplePoints,
              List<Direction> directions, int[][] collisionMatrix, double lambda) {
        this.viewpoints = viewpoints;
        this.samplePoints = samplePoints;
        this.directions = directions;
        this.collisionMatrix = collisionMatrix;
        this.lambda = lambda;
    }
}

// =============Tool/Functional Class==============
// SimpleJsonParser
class SimpleJsonParser {
    private final String text;
    private int index = 0;

    // Constructor
    SimpleJsonParser(String text) {
        this.text = text;
    }

    // Read Json File From Stdin
    static Object parseFromInput() throws IOException {
        String text = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
        SimpleJsonParser parser = new SimpleJsonParser(text);
        return parser.parseValue();
    }

    // Main Method
    Object parseValue() {
        skipWhitespace();
        char c = text.charAt(index);
        if (c == '{') return parseObject();
        if (c == '[') return parseArray();
        if (c == '"') return parseString();
        if (c == '-' || Character.isDigit(c)) return parseNumber();
        if (text.startsWith("true", index)) {
            index += 4;
            return Boolean.TRUE;
        }
        if (text.startsWith("false", index)) {
            index += 5;
            return Boolean.FALSE;
        }
        if (text.startsWith("null", index)) {
            index += 4;
            return null;
        }
        throw new RuntimeException("Unexpected character at " + index + ": " + c);
    }

    // Parse Object
    Map<String, Object> parseObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        index++; // skip '{'
        skipWhitespace();
        if (text.charAt(index) == '}') {
            index++;
            return map;
        }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            if (text.charAt(index) != ':')
                throw new RuntimeException("Expected ':' at " + index);
            index++; // skip ':'
            Object value = parseValue();
            map.put(key, value);
            skipWhitespace();
            char c = text.charAt(index);
            if (c == '}') {
                index++;
                break;
            }
            if (c == ',') {
                index++;
                continue;
            }
            throw new RuntimeException("Expected ',' or '}' at " + index);
        }
        return map;
    }

    // Parse Array
    List<Object> parseArray() {
        List<Object> list = new ArrayList<>();
        index++; // skip '['
        skipWhitespace();
        if (text.charAt(index) == ']') {
            index++;
            return list;
        }
        while (true) {
            Object val = parseValue();
            list.add(val);
            skipWhitespace();
            char c = text.charAt(index);
            if (c == ']') {
                index++;
                break;
            }
            if (c == ',') {
                index++;
                continue;
            }
            throw new RuntimeException("Expected ',' or ']' at " + index);
        }
        return list;
    }

    // Parse String
    String parseString() {
        StringBuilder sb = new StringBuilder();
        index++; // skip '"'
        while (index < text.length()) {
            char c = text.charAt(index++);
            if (c == '\\') {
                char next = text.charAt(index++);
                if (next == '"' || next == '\\' || next == '/') sb.append(next);
                else if (next == 'b') sb.append('\b');
                else if (next == 'f') sb.append('\f');
                else if (next == 'n') sb.append('\n');
                else if (next == 'r') sb.append('\r');
                else if (next == 't') sb.append('\t');
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // Parse Number
    Double parseNumber() {
        int start = index;
        while (index < text.length() &&
                ("0123456789+-.eE".indexOf(text.charAt(index)) >= 0)) {
            index++;
        }
        return Double.valueOf(text.substring(start, index));
    }

    // Skip Whitespace
    void skipWhitespace() {
        while (index < text.length()) {
            char c = text.charAt(index);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') index++;
            else break;
        }
    }
}

// JsonParser : Convert Json to InputData
class JsonParser {
    @SuppressWarnings("unchecked")
    static InputData parseInput() throws Exception {
        // Get Json Object
        Object rootObj = SimpleJsonParser.parseFromInput();
        if (!(rootObj instanceof Map)) {
            throw new RuntimeException("Root JSON must be an object");
        }
        Map<String, Object> root = (Map<String, Object>) rootObj;

        // Get Lambda
        Map<String, Object> metadata = (Map<String, Object>) root.get("metadata");
        double lambda = ((Number) metadata.get("lambda")).doubleValue();

        // Get Directions
        List<Object> dirArray = (List<Object>) root.get("directions");
        List<Direction> directions = new ArrayList<>();
        for (int i = 0; i < dirArray.size(); i++) {
            List<Object> vec = (List<Object>) dirArray.get(i);
            String id = "a" + (i + 1);
            double x = ((Number) vec.get(0)).doubleValue();
            double y = ((Number) vec.get(1)).doubleValue();
            double z = ((Number) vec.get(2)).doubleValue();
            directions.add(new Direction(id, x, y, z));
        }

        // Get Viewpoints
        List<Object> vpArray = (List<Object>) root.get("viewpoints");
        List<Viewpoint> viewpoints = new ArrayList<>();

        for (Object vo : vpArray) {
            Map<String, Object> v = (Map<String, Object>) vo;
            String id = (String) v.get("id");
            boolean isMandatory = Boolean.TRUE.equals(v.get("is_mandatory"));
            Map<String, Object> coords = (Map<String, Object>) v.get("coordinates");
            double x = ((Number) coords.get("x")).doubleValue();
            double y = ((Number) coords.get("y")).doubleValue();
            double z = ((Number) coords.get("z")).doubleValue();
            Map<String, Double> precision = new HashMap<>();
            Map<String, Object> prec = (Map<String, Object>) v.get("precision");
            for (Map.Entry<String, Object> e : prec.entrySet()) {
                precision.put(e.getKey(), ((Number) e.getValue()).doubleValue());
            }
            viewpoints.add(new Viewpoint(id, isMandatory, x, y, z, precision));
        }

        // Get SamplePoints
        List<Object> sArray = (List<Object>) root.get("sample_points");
        List<SamplePoint> samples = new ArrayList<>();

        for (Object so : sArray) {
            Map<String, Object> s = (Map<String, Object>) so;
            String id = (String) s.get("id");

            Map<String, Object> coords = (Map<String, Object>) s.get("coordinates");
            double x = ((Number) coords.get("x")).doubleValue();
            double y = ((Number) coords.get("y")).doubleValue();
            double z = ((Number) coords.get("z")).doubleValue();

            SamplePoint sp = new SamplePoint(id, x, y, z);
            List<Object> pairs = (List<Object>) s.get("covering_pairs");
            for (Object p : pairs) {
                List<Object> arr = (List<Object>) p;
                String vpId = (String) arr.get(0);
                String angleId = (String) arr.get(1);
                sp.addCoveringPair(vpId, angleId);
            }
            samples.add(sp);
        }

        // Get Collision_matrix
        List<Object> cArray = (List<Object>) root.get("collision_matrix");
        int n = cArray.size();
        int[][] matrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            List<Object> row = (List<Object>) cArray.get(i);
            for (int j = 0; j < row.size(); j++) {
                matrix[i][j] = ((Number) row.get(j)).intValue();
            }
        }

        //  Construct InputData
        return new InputData(viewpoints, samples, directions, matrix, lambda);
    }
}

// GraphUtil : Graph related functions
class GraphUtil {
    // Check if the matrix is symmetric
    static boolean isSymmetric(int[][] matrix) {
        int n = matrix.length;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (matrix[i][j] != matrix[j][i]) {
                    System.err.printf("⚠️ 矩阵不对称: [%d][%d]=%d, [%d][%d]=%d%n",
                            i, j, matrix[i][j], j, i, matrix[j][i]);
                    return false;
                }
            }
        }
        return true;
    }

    // Calculate the Euclidean distance between two viewpoints
    static double euclideanDistance(Viewpoint a, Viewpoint b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // Construct the distance matrix from the collision matrix
    static double[][] buildDistanceMatrix(List<Viewpoint> vps, int[][] collisionMatrix) {
        int n = vps.size();
        double[][] dist = new double[n][n];

        for (int i = 0; i < n; i++) {
            dist[i][i] = 0.0;
            Viewpoint vi = vps.get(i);
            for (int j = i + 1; j < n; j++) {
                int val = collisionMatrix[i][j];
                double d;
                if (val == 1) {
                    d = euclideanDistance(vi, vps.get(j));
                } else if (val == -1) {
                    d = Double.POSITIVE_INFINITY;
                } else {
                    d = 0.0;
                }
                dist[i][j] = dist[j][i] = d;
            }
        }
        return dist;
    }
}

// SolutionBuilder : Build the solution
class SolutionBuilder {
    public static void writeSolutionJson(
            String outputPath,
            TourPlanner.TourResult tourResult,
            Map<Viewpoint, Set<String>> selected,
            double precisionValue,
            int numViewpoints
    ) {

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
        // Write to file (Only for Debugging)
        // try (FileWriter fw = new FileWriter(outputPath, false)) {
        //     //fw.write(sb.toString());
        //     System.out.println(sb.toString());
        // }
        // Print result to stdout
        System.out.println(sb);
    }
}

// TimerUtil : Count the execution time
class TimerUtil {
    private long startTime;
    private long originTime;

    void start() {
        startTime = System.currentTimeMillis();
        originTime = startTime;
    }

    void printElapsed(String msg) {
        long now = System.currentTimeMillis();
        System.err.println(msg + " Cost: " + (now - startTime) + " ms" + "; Total: " + (now - originTime) + " ms)");
        startTime = now;
    }
}

// CoverageChecker : Check the coverage of the solution
class CoverageChecker {
    // Check if the selected directions are valid
    static void checkSelectedDirectionsValid(Map<Viewpoint, Set<String>> selected) {
        int totalVp = selected.size();
        int totalDirs = 0;
        int invalidInPrecision = 0;
        for (Map.Entry<Viewpoint, Set<String>> entry : selected.entrySet()) {
            Viewpoint vp = entry.getKey();
            Set<String> dirIds = entry.getValue();
            if (vp == null || dirIds == null) {
                continue;
            }
            totalDirs += dirIds.size();
            Map<String, Double> precisionMap = vp.precision;
            for (String dirId : dirIds) {
                if (precisionMap == null || !precisionMap.containsKey(dirId)) {
                    invalidInPrecision++;
                    System.out.println("Invalid direction: " + "vpId=" + vp.id + ", dirId=" + dirId);
                }
            }
        }
        System.out.println("Viewpoints: " + totalVp);
        System.out.println("Selected directions: " + totalDirs);
        System.out.println("Invalid directions: " + invalidInPrecision);
    }

    // Check if the samples are covered at least 3 times
    static void checkSampleCoverage(
            Map<Viewpoint, Set<String>> selected,
            List<SamplePoint> samplePoints
    ) {
        // Build vpId -> Set<dirId>
        Map<String, Set<String>> selectedIdMap = new HashMap<>();
        for (Map.Entry<Viewpoint, Set<String>> entry : selected.entrySet()) {
            Viewpoint vp = entry.getKey();
            Set<String> dirIds = entry.getValue();
            if (dirIds == null || vp == null) {
                continue;
            }
            selectedIdMap.put(vp.id, dirIds);
        }
        int totalSamples = samplePoints.size();
        int coveredAtLeast3 = 0;
        int lessThan3 = 0;
        int zeroCovered = 0;
        int maxCover = 0;
        int sumCover = 0;

        List<String> badSamples = new ArrayList<>();
        for (SamplePoint sp : samplePoints) {
            int coverCount = 0;
            if (sp.coveringPairs != null) {
                for (CoveringPair cp : sp.coveringPairs) {
                    String vpId = cp.viewpointId;
                    String dirId = cp.directionId;

                    Set<String> dirIds = selectedIdMap.get(vpId);
                    if (dirIds != null && dirIds.contains(dirId)) {
                        coverCount++;
                    }
                }
            }
            sumCover += coverCount;
            if (coverCount > maxCover) {
                maxCover = coverCount;
            }
            if (coverCount >= 3) {
                coveredAtLeast3++;
            } else {
                lessThan3++;
                if (coverCount == 0) {
                    zeroCovered++;
                    if (sp.coveringPairs != null) {
                        badSamples.add(sp.id + " (cover=" + coverCount
                                + ", possible=" + sp.coveringPairs.size() + ")");
                    }
                }
            }
        }
        double avgCover = totalSamples == 0 ? 0 : (double) sumCover / totalSamples;
        System.out.println("SamplePoints: " + totalSamples);
        System.out.println("Coverage over 3: " + coveredAtLeast3);
        System.out.println("Coverage below 3: " + lessThan3);
        System.out.println("Zero covered sp: " + zeroCovered);
        System.out.println("Maximum Coverage: " + maxCover);
        System.out.println("Average Coverage: " + String.format("%.3f", avgCover));
        if (!badSamples.isEmpty()) {
            System.out.println("Samples that have been covered less than 3 times(format: id(cover, possible)):");
            for (String s : badSamples) {
                System.out.println("  " + s);
            }
        }
    }
}

// DirectionSelector : Select directions based on global contribution
class DirectionSelector {
    // Maximizing contribution to both precision and coverage until all sample points are covered at least 3 times
    // Immutable key representing a (viewpoint, direction) pair
    record VpDirKey(String viewpointId, String directionId) {
    }

    // Store each (viewpoint, direction) pair's contribution
    record DirectionContribution(
            VpDirKey vpDirKey,
            double precision,
            int coveredSamplePoints,
            double score
    ) {
    }

    static Map<Viewpoint, Set<String>> selectDirections(
            List<Viewpoint> viewpoints,
            List<SamplePoint> samples
    ) {

        // Build cover map (vpId, dirId) -> covered sampleIds
        Map<String, Map<String, Set<String>>> coverageMap = new HashMap<>();
        for (SamplePoint sp : samples) {
            if (sp.coveringPairs == null) {
                continue;
            }
            for (CoveringPair cp : sp.coveringPairs) {
                coverageMap
                        .computeIfAbsent(cp.viewpointId, k -> new HashMap<>())
                        .computeIfAbsent(cp.directionId, k -> new HashSet<>())
                        .add(sp.id);
            }
        }

        // Build viewpoint map vpId -> Viewpoint
        Map<String, Viewpoint> viewpointById = new HashMap<>();
        for (Viewpoint vp : viewpoints) {
            viewpointById.put(vp.id, vp);
        }

        // Find high precision directions for each sample as candidate
        Map<String, Set<String>> candidateDirections = new HashMap<>();

        for (SamplePoint sp : samples) {
            // Store precisions of each direction
            Map<VpDirKey, Double> localScores = new HashMap<>();
            for (CoveringPair cp : sp.coveringPairs) {
                Viewpoint vp = viewpointById.get(cp.viewpointId);
                if (vp == null) {
                    continue;
                }
                Double precision = vp.precision.get(cp.directionId);
                if (precision == null) {
                    continue;
                }
                localScores.put(new VpDirKey(cp.viewpointId, cp.directionId), precision);
            }
            // Sort by precision
            localScores.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .limit(3)
                    .forEach(e -> {
                        VpDirKey k = e.getKey();
                        candidateDirections
                                .computeIfAbsent(k.viewpointId(), x -> new HashSet<>())
                                .add(k.directionId());
                    });
        }

        // Global contribution evaluation: score = precision * coverCount
        // Storing global contribution information
        List<DirectionContribution> contributions = new ArrayList<>();

        // Traverse all
        for (Map.Entry<String, Set<String>> e : candidateDirections.entrySet()) {
            // Get corresponding viewpoint
            String vpId = e.getKey();
            Viewpoint vp = viewpointById.get(vpId);
            if (vp == null) {
                continue;
            }
            // Traverse all directions of this viewpoint
            for (String dirId : e.getValue()) {
                Double precision = vp.precision.get(dirId);
                if (precision == null) {
                    continue;
                }

                // Calculate cover count
                int coverCount = 0;
                Map<String, Set<String>> map = coverageMap.get(vpId);
                if (map != null && map.get(dirId) != null) {
                    coverCount = map.get(dirId).size();
                }
                // Calculate score
                contributions.add(new DirectionContribution(
                        new VpDirKey(vpId, dirId),
                        precision,
                        coverCount,
                        precision * coverCount
                ));
            }
        }

        // Sort by contribution score
        contributions.sort((a, b) -> Double.compare(b.score(), a.score()));

        // Greedy selection under coverage constraints
        // Track remaining coverage demand for each sample
        Map<String, Integer> remainNeed = new HashMap<>();
        for (SamplePoint sp : samples) {
            remainNeed.put(sp.id, 3);
        }
        // Final selected directions grouped by viewpoint
        Map<Viewpoint, Set<String>> result = new HashMap<>();

        // Select directions greedily
        boolean allSatisfied = true;
        for (int idx1 = 0; idx1 < contributions.size(); idx1++) {
            DirectionContribution dc = contributions.get(idx1);
            // Get corresponding viewpoint
            VpDirKey k = dc.vpDirKey();
            Viewpoint vp = viewpointById.get(k.viewpointId());

            // Add direction to result
            result
                    .computeIfAbsent(vp, x -> new HashSet<>())
                    .add(k.directionId());

            // Update remaining need
            Map<String, Set<String>> map = coverageMap.get(k.viewpointId());
            if (map != null && map.get(k.directionId()) != null) {
                for (String sampleId : map.get(k.directionId())) {
                    int v = remainNeed.get(sampleId);
                    if (v > 0) {
                        remainNeed.put(sampleId, v - 1);
                    }
                }
            }

            // Check if all need satisfied
            for (int v : remainNeed.values()) {
                if (v > 0) {
                    allSatisfied = false;
                    break;
                }
            }
            if (allSatisfied) {
                // Add the remaining positive precision direction to result
                for (int idx2 = idx1 + 1; idx2 < contributions.size(); idx2++) {
                    DirectionContribution next = contributions.get(idx2);
                    if (next.score() <= 0) {
                        break;
                    }
                    result
                            .computeIfAbsent(viewpointById.get(next.vpDirKey().viewpointId()), x -> new HashSet<>())
                            .add(next.vpDirKey().directionId());
                }
                break;
            }
        }
        return result;
    }
}

// TourPlanner : Build a closed tour
class TourPlanner {
    // Save the tour
    static class TourResult {
        public final List<Viewpoint> tour;      // tour
        public final double totalDistance;      // total travel distance

        // Constructor
        public TourResult(List<Viewpoint> tour, double totalDistance) {
            this.tour = tour;
            this.totalDistance = totalDistance;
        }
    }

    // Main method to build a tour
    static TourResult buildTour(
            List<Viewpoint> allVps, // all viewpoints
            Collection<Viewpoint> mustVisit,    // viewpoints that must be visited
            List<Viewpoint> allowedTransit, // viewpoints that can be visited as transit
            double[][] dist
    ) {
        // Check whether there are viewpoints to visit
        if (mustVisit == null || mustVisit.isEmpty()) {
            return new TourResult(new ArrayList<>(), 0.0);
        }

        // Find the start viewpoint
        Viewpoint start = null;
        for (Viewpoint vp : allVps) {
            if (vp.isMandatory) {
                start = vp;
                break;
            }
        }
        // If there is no mandatory viewpoint, start from the first viewpoint in mustVisit
        if (start == null) {
            start = mustVisit.iterator().next();
        }

        // Make sure allowedTransit contains mustVisit (good for backtrack)
        LinkedHashSet<Viewpoint> transitSet = new LinkedHashSet<>(allowedTransit);
        transitSet.addAll(mustVisit);
        List<Viewpoint> fullTransit = new ArrayList<>(transitSet);

        // Nearest Neighbor Path
        List<Viewpoint> nnPath = buildNearestNeighborPath(mustVisit, dist, start);

        // Closed loop to the Beginning
        nnPath.add(start);

        // Insert a stopover point so that the path has no INF edges
        List<Viewpoint> repaired = repairPath(nnPath, fullTransit, dist);

        // Calculate the total distance
        double totalDist = computePathLength(repaired, dist);
        return new TourResult(repaired, totalDist);
    }

    // Nearest Neighbor Path Construct
    private static List<Viewpoint> buildNearestNeighborPath(
            Collection<Viewpoint> mustVisit,
            double[][] dist,
            Viewpoint start
    ) {
        // Ensure unique viewpoint list while preserving insertion order
        List<Viewpoint> nodes = new ArrayList<>(new LinkedHashSet<>(mustVisit));

        // Ensure to include a starting point
        if (!nodes.contains(start)) {
            nodes.add(0, start);
        }

        int m = nodes.size();
        boolean[] visited = new boolean[m];

        // Build a local index map: vp -> local index, for marking visited
        Map<Viewpoint, Integer> vp2local = new HashMap<>();
        for (int i = 0; i < m; i++) {
            vp2local.put(nodes.get(i), i);
        }
        int currentLocal = vp2local.get(start);
        visited[currentLocal] = true;

        List<Viewpoint> path = new ArrayList<>();
        path.add(start);

        // Main loop
        // At each step, choose the closest unvisited viewpoint
        // INF edges are allowed and will later be repaired via allowedTransit
        while (path.size() < m) {
            Viewpoint currentVp = path.get(path.size() - 1);
            int gi = currentVp.index;

            int bestLocal = -1;
            double bestDist = Double.POSITIVE_INFINITY;

            for (int j = 0; j < m; j++) {
                if (visited[j]) {
                    continue;
                }
                Viewpoint cand = nodes.get(j);
                double d = dist[gi][cand.index];
                if (bestLocal == -1 || d < bestDist) {
                    bestDist = d;
                    bestLocal = j;
                }
            }
            // In theory this should never happen unless mustVisit is empty
            if (bestLocal == -1) {
                System.err.println("TourPlanner: The nearest neighbor cannot find the next node and end early。");
                break;
            }
            // Mark the bestLocal as visited and add it to the path
            visited[bestLocal] = true;
            path.add(nodes.get(bestLocal));
        }
        return path;
    }

    // Repair a path by replacing any INF edges with feasible relay points
    private static List<Viewpoint> repairPath(
            List<Viewpoint> path,
            List<Viewpoint> allowedTransit,
            double[][] dist
    ) {
        // Copy the path so original is not modified
        List<Viewpoint> res = new ArrayList<>(path);
        boolean changed = true;

        // Continue repairing until a full pass finds no changes
        while (changed) {
            changed = false;

            // Scan adjacent pairs
            for (int i = 0; i < res.size() - 1; i++) {
                Viewpoint A = res.get(i);
                Viewpoint B = res.get(i + 1);

                // If A → B is valid (not INF), nothing to repair
                if (dist[A.index][B.index] != Double.POSITIVE_INFINITY) {
                    continue;
                }

                // Attempt single-hop repair: A → K → B
                Viewpoint K = findSingleHop(A, B, allowedTransit, dist);
                if (K != null) {
                    // System.out.printf("[REPAIR-1] %s → %s → %s\n", A.id, K.id, B.id);
                    // Insert K between A and B
                    res.add(i + 1, K);
                    // Restart scanning from the beginning, because path changed
                    changed = true;
                    break; // 重新从头扫描
                }

                // Attempt double-hop repair: A → K1 → K2 → B
                List<Viewpoint> two = findTwoHop(A, B, allowedTransit, dist);
                if (two != null) {
                    // System.out.printf("[REPAIR-2] %s → %s → %s → %s\n",A.id, two.get(0).id, two.get(1).id, B.id);
                    // Insert K1 and K2 between A and B
                    res.add(i + 1, two.get(0));
                    res.add(i + 2, two.get(1));
                    // Restart scanning from the beginning, because path changed
                    changed = true;
                    break; // 重新从头扫描
                }

                // No repair found: This A → B cannot be fixed with the given set
                throw new RuntimeException("TourPlanner: Path segments cannot be repaired: "
                        + A.id + " → " + B.id);
            }
        }
        return res;
    }

    // Try single-hop repair for an INF edge A → B
    // Look for any viewpoint K in allowedTransit such that: dist[A][K] < INF  AND  dist[K][B] < INF
    private static Viewpoint findSingleHop(
            Viewpoint A,
            Viewpoint B,
            List<Viewpoint> allowedTransit,
            double[][] dist
    ) {
        int Ai = A.index;
        int Bi = B.index;

        for (Viewpoint K : allowedTransit) {
            int Ki = K.index;
            // Both sides must be reachable
            if (dist[Ai][Ki] != Double.POSITIVE_INFINITY &&
                    dist[Ki][Bi] != Double.POSITIVE_INFINITY) {
                return K;
            }
        }
        return null;
    }

    // Try double-hop repair for an INF edge A → B
    private static List<Viewpoint> findTwoHop(
            Viewpoint A,
            Viewpoint B,
            List<Viewpoint> allowedTransit,
            double[][] dist
    ) {
        int Ai = A.index;
        int Bi = B.index;

        for (Viewpoint K1 : allowedTransit) {
            int K1i = K1.index;
            // First hop must be reachable: A → K1
            if (dist[Ai][K1i] == Double.POSITIVE_INFINITY) continue;
            for (Viewpoint K2 : allowedTransit) {
                int K2i = K2.index;
                // Check if K1 → K2 → B is reachable
                if (dist[K1i][K2i] != Double.POSITIVE_INFINITY &&
                        dist[K2i][Bi] != Double.POSITIVE_INFINITY) {
                    return Arrays.asList(K1, K2);
                }
            }
        }
        return null;
    }

    // Compute total path length
    // For each consecutive pair A → B:
    // If dist[A][B] is finite, add it.
    // If it is INF, print a warning (should not happen after repair)
    private static double computePathLength(List<Viewpoint> path, double[][] dist) {
        double total = 0.0;
        for (int i = 0; i < path.size() - 1; i++) {
            Viewpoint A = path.get(i);
            Viewpoint B = path.get(i + 1);
            double d = dist[A.index][B.index];
            if (Double.isInfinite(d)) {
                System.err.println("Warning: INF edges still exist in the path: " + A.id + " → " + B.id);
            } else {
                total += d;
            }
        }
        return total;
    }
}

public class AADS {
    public static void main(String[] args) throws Exception {
        // TimerUtil timer = new TimerUtil();
        // timer.start();

        // Parse the input data
        InputData data = JsonParser.parseInput();
        // System.out.println("✅ Parse Success!");
        // System.out.println("Viewpoints: " + data.viewpoints.size());
        // System.out.println(data.viewpoints.get(0).toString());
        // for (Viewpoint vp : data.viewpoints) {
        //     if (vp.isMandatory) {
        //         System.out.println("Mandatory viewpoint: " + vp);
        //     }
        // }
        // System.out.println("Samples: " + data.samplePoints.size());
        // System.out.println(data.samplePoints.get(0).toString());
        // System.out.println("Directions: " + data.directions.size());
        // System.out.println(data.directions.get(0).toString());
        // System.out.println("Collision matrix: " + data.collisionMatrix.length);
        // System.out.println("Is symmetric: " + GraphUtil.isSymmetric(data.collisionMatrix));
        // // timer.printElapsed("数据解析");
        // // checkCoverage
        // List<SamplePoint> lessThan1 = new ArrayList<>();
        // List<SamplePoint> lessThan3 = new ArrayList<>();
        // data.samplePoints.forEach(samplePoint -> {
        //     if (samplePoint.coveringPairs.isEmpty()) {
        //         lessThan1.add(samplePoint);
        //     }
        //     if (samplePoint.coveringPairs.size() < 3) {
        //         lessThan3.add(samplePoint);
        //     }
        // });
        // System.out.println("✅ Coverage Check Success!");
        // System.out.println("Covering pair less than 1: " + lessThan1.size() + " " + lessThan1);
        // System.out.println("Covering pair less than 3: " + lessThan3.size() + " " + lessThan3);
        // Build the distance matrix
        double[][] distanceMatrix = GraphUtil.buildDistanceMatrix(data.viewpoints, data.collisionMatrix);
        // timer.printElapsed("距离矩阵计算");

        // Select directions
        Map<Viewpoint, Set<String>> selectedViewpoints = DirectionSelector.selectDirections(data.viewpoints, data.samplePoints);
        // timer.printElapsed("方向选择");
        // timer.printElapsed("样本覆盖检查");
        // Build the tour
        TourPlanner.TourResult finalTour = TourPlanner.buildTour(
                data.viewpoints,
                selectedViewpoints.keySet(),
                new ArrayList<>(selectedViewpoints.keySet()),
                distanceMatrix
        );
        // System.out.println("Route length: " + finalTour.totalDistance);
        // timer.printElapsed("路径规划");

        // Calculate precision
        double finalPrecision = computeTotalPrecision(selectedViewpoints);
        // System.out.println("Final precision: " + finalPrecision);
        // timer.printElapsed("精度计算");

        // Write the solution to file
        SolutionBuilder.writeSolutionJson(
                "solution.json",
                finalTour,
                selectedViewpoints,
                finalPrecision,
                finalTour.tour.size()
        );
        // validateTour(finalTour.tour, data.collisionMatrix);
    }

    // Calculate precision
    static double computeTotalPrecision(Map<Viewpoint, Set<String>> selected) {
        final double[] totalPrecision = {0};
        selected.forEach((vp, dirIds) -> {
            for (String dirId : dirIds) {
                totalPrecision[0] += vp.precision.get(dirId);
            }
        });
        return totalPrecision[0];
    }

    // Check the tour is valid
    static void validateTour(List<Viewpoint> tour, int[][] cm) {
        // Check if the tour exists and is not empty
        if (tour == null || tour.isEmpty()) {
            System.err.println("❌ Tour is empty.");

        }

        // Check if the tour is closed (first == last)
        if (tour != null && !tour.get(0).equals(tour.get(tour.size() - 1))) {
            System.err.println("❌ Tour is not closed (first != last).");

        }

        // Check if the tour is reachable
        if (tour != null) {
            for (int i = 0; i + 1 < tour.size(); i++) {
                int a = tour.get(i).index;
                int b = tour.get(i + 1).index;
                if (cm[a][b] != 1) {
                    System.err.printf("❌ Illegal transition: %d → %d (cm=%d)%n", a, b, cm[a][b]);
                }
            }
        }
        System.out.println("✅ Tour validation passed.");
        if (tour != null) {
            System.out.println("   Path length = " + tour.size());
        }
    }
}
