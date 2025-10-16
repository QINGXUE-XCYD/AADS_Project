import java.util.ArrayList;
import java.util.List;

public class SamplePoint {
    // Just id
    String id;
    // Coordinates
    double x, y, z;
    // Sets of feasible viewpoint-direction pairs
    List<CoveringPair> coveringPairs;

    // Constructor
    SamplePoint(String id, double x, double y, double z, List<CoveringPair> coveringPairs) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.coveringPairs = coveringPairs;
    }

    SamplePoint(String id, double x, double y, double z) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.coveringPairs = new ArrayList<>();
    }

}
