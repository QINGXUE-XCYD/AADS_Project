public class CoveringPair {
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
