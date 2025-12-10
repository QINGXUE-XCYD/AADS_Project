import java.io.FileWriter;
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

// =============Tool Class==============
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
        // Write to file (Only for Debugging)
        try (FileWriter fw = new FileWriter(outputPath, false)) {
            fw.write(sb.toString());
        }
    }
}


public class AADS {
    public static void main(String[] args) throws Exception {
        TimerUtil timer = new TimerUtil();
        timer.start();

        // Parse the input data
        InputData data = JsonParser.parseInput();
        if (false) {
            System.out.println("✅ Parse Success!");
            System.out.println("Viewpoints: " + data.viewpoints.size());
            System.out.println(data.viewpoints.get(0).toString());
            for (Viewpoint vp : data.viewpoints) {
                if (vp.isMandatory) {
                    System.out.println("Mandatory viewpoint: " + vp);
                }
            }
            System.out.println("Samples: " + data.samplePoints.size());
            System.out.println(data.samplePoints.get(0).toString());
            System.out.println("Directions: " + data.directions.size());
            System.out.println(data.directions.get(0).toString());
            System.out.println("Collision matrix: " + data.collisionMatrix.length);
            System.out.println("Is symmetric: " + GraphUtil.isSymmetric(data.collisionMatrix));
            timer.printElapsed("数据解析");
            // checkCoverage
            List<SamplePoint> lessThan1 = new ArrayList<>();
            List<SamplePoint> lessThan3 = new ArrayList<>();
            data.samplePoints.forEach(samplePoint -> {
                if (samplePoint.coveringPairs.isEmpty()) {
                    lessThan1.add(samplePoint);
                }
                if (samplePoint.coveringPairs.size() < 3) {
                    lessThan3.add(samplePoint);
                }
            });
            System.out.println("✅ Coverage Check Success!");
            System.out.println("Covering pair less than 1: " + lessThan1.size() + " " + lessThan1);
            System.out.println("Covering pair less than 3: " + lessThan3.size() + " " + lessThan3);
        }
        // Build the distance matrix
        double[][] distanceMatrix = GraphUtil.buildDistanceMatrix(data.viewpoints, data.collisionMatrix);
        timer.printElapsed("距离矩阵计算");

        // Select directions
        //Map<Viewpoint, Set<String>> selectedViewpoints = DirectionSelectorHybrid.selectDirections(data.viewpoints, data.samplePoints);
        Map<Viewpoint,Set<String>> selectedViewpoints = DirectionSelectorHybrid.selectDirections(data.viewpoints, data.samplePoints);
        timer.printElapsed("方向选择");
        // Check the selected directions
        CoverageChecker.checkSelectedDirectionsValid(selectedViewpoints);
        CoverageChecker.checkSampleCoverage(selectedViewpoints, data.samplePoints);
        timer.printElapsed("样本覆盖检查");
        // Build the tour
        TourPlanner.TourResult finalTour = TourPlanner.buildTour(
                data.viewpoints,
                selectedViewpoints.keySet(),
                new ArrayList<>(selectedViewpoints.keySet()),
                distanceMatrix
        );
        System.out.println("路线长度" + finalTour.totalDistance);
        timer.printElapsed("路径规划");

        // Calculate precision
        double finalPrecision = computeTotalPrecision(selectedViewpoints);
        System.out.println("Final precision: " + finalPrecision);
        timer.printElapsed("精度计算");

        // Write the solution to file
        SolutionBuilder.writeSolutionJson(
                "solution.json",
                finalTour,
                selectedViewpoints,
                finalPrecision,
                data.viewpoints.size()
        );

        validateTour(finalTour.tour, data.collisionMatrix);


    }
    // 计算精度
    static double computeTotalPrecision(
            Map<Viewpoint, Set<String>> selected
    ) {
        final double[] totalPrecision = {0};
        selected.forEach((vp, dirIds) -> {
            for (String dirId : dirIds) {
                totalPrecision[0] += vp.precision.get(dirId);
            }
        });
        return totalPrecision[0];
    }

    static void validateTour(List<Viewpoint> tour, int[][] cm) {

        if (tour == null || tour.isEmpty()) {
            System.err.println("❌ Tour is empty.");

        }

        // 1) 检查闭环：首尾必须相同
        if (tour != null && !tour.get(0).equals(tour.get(tour.size() - 1))) {
            System.err.println("❌ Tour is not closed (first != last).");

        }

        // 2) 逐段检查可行性
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

