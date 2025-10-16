import java.util.HashMap;
import java.util.Map;

public class Viewpoint {
    // Just id
    String id;
    // Where the tour begins and ends
    boolean isMandatory;
    // Coordinates
    double x, y, z;
    // Precision of different angles
    Map<String, Double> precision;

    // Constructor
    Viewpoint(String id, boolean isMandatory, double x, double y, double z, Map<String, Double> precision) {
        this.id = id;
        this.isMandatory = isMandatory;
        this.x = x;
        this.y = y;
        this.z = z;
        this.precision = precision;
    }

    Viewpoint(String id, boolean isMandatory, double x, double y, double z) {
        this.id = id;
        this.isMandatory = isMandatory;
        this.x = x;
        this.y = y;
        this.z = z;
        this.precision = new HashMap<>();
    }
}
