import java.util.HashMap;
import java.util.Map;

public class Viewpoint {
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

    Viewpoint(String id, boolean isMandatory, double x, double y, double z) {
        this.id = id;
        this.index = Integer.parseInt(id.substring(1)) - 1;
        this.isMandatory = isMandatory;
        this.x = x;
        this.y = y;
        this.z = z;
        this.precision = new HashMap<>();
    }

    // toString
    @Override
    public String toString() {
        return "id: " + id + ", isMandatory: " + isMandatory + ", x: " + x + ", y: " + y + ", z: " + z + ", precision: " + precision;
    }
}