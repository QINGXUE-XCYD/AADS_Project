import java.util.List;

class InputData {
    // Input data for the algorithm
    List<Viewpoint> viewpoints;
    List<SamplePoint> samplePoints;
    List<Direction> directions;
    int[][] collisionMatrix;

    // Constructor
    InputData(List<Viewpoint> viewpoints, List<SamplePoint> samplePoints,
              List<Direction> directions, int[][] collisionMatrix) {
        this.viewpoints = viewpoints;
        this.samplePoints = samplePoints;
        this.directions = directions;
        this.collisionMatrix = collisionMatrix;
    }
}
