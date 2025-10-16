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

    // Get direction
    double[] getDirection() {
        return new double[]{x, y, z};
    }
}
