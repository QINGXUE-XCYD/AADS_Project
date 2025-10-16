import java.util.List;
import java.util.Map;

public class TestJson {
    public static void main(String[] args) throws Exception {
        Object root = SimpleJsonParser.parseFromInput(System.in);
        Map<String, Object> json = (Map<String, Object>) root;

        System.out.println("Keys: " + json.keySet());
        System.out.println("Directions size: " + ((List<?>)json.get("directions")).size());
    }
}
