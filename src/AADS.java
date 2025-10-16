public class AADS {
    public static void main(String[] args) throws Exception {

        TimerUtil timer = new TimerUtil();
        timer.start();

        InputData data = JsonParser.parseInput(System.in);

        System.out.println("✅ Parse Success!");
        System.out.println("Viewpoints: " + data.viewpoints.size());
        System.out.println("Samples: " + data.samplePoints.size());
        System.out.println("Directions: " + data.directions.size());
        System.out.println("Collision matrix: " + data.collisionMatrix.length);
        timer.printElapsed(null);
    }
}
