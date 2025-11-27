import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonParser {
    static InputData parseInput() throws Exception {
        // 1️⃣ 用我们自己的 JSON 解析器读入
        Object rootObj = SimpleJsonParser.parseFromInput(System.in);
        if (!(rootObj instanceof Map)) {
            throw new RuntimeException("Root JSON must be an object");
        }
        Map<String, Object> root = (Map<String, Object>) rootObj;

        // 读取lambda
        Map<String, Object> metadata = (Map<String, Object>) root.get("metadata");
        double lambda = ((Number) metadata.get("lambda")).doubleValue();


        // 2️⃣ 解析 directions
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

        // 3️⃣ 解析 viewpoints
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

        // 4️⃣ 解析 samples
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

        // 5️⃣ 解析 collision_matrix
        List<Object> cArray = (List<Object>) root.get("collision_matrix");
        int n = cArray.size();
        int[][] matrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            List<Object> row = (List<Object>) cArray.get(i);
            for (int j = 0; j < row.size(); j++) {
                matrix[i][j] = ((Number) row.get(j)).intValue();
            }
        }

        // ✅ 返回封装好的 InputData
        return new InputData(viewpoints, samples, directions, matrix, lambda);
    }
}
