public class AADS {
    public static void main(String[] args) throws Exception {

        TimerUtil timer = new TimerUtil();
        timer.start();

        InputData data = JsonParser.parseInput(System.in);

        System.out.println("✅ Parse Success!");
        System.out.println("Viewpoints: " + data.viewpoints.size());
        System.out.println(data.viewpoints.get(0).toString());
        System.out.println("Samples: " + data.samplePoints.size());
        System.out.println(data.samplePoints.get(0).toString());
        System.out.println("Directions: " + data.directions.size());
        System.out.println(data.directions.get(0).toString());
        System.out.println("Collision matrix: " + data.collisionMatrix.length);
        System.out.println("Collision matrix: " + data.collisionMatrix[0].length);
        timer.printElapsed(null);
    }
}
