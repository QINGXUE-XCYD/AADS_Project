import java.util.Map;
import java.util.Set;

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
        System.out.println("Is symmetric: " + GraphUtil.isSymmetric(data.collisionMatrix));
        double[][] distanceMatrix = GraphUtil.buildDistanceMatrix(data.viewpoints, data.collisionMatrix);
        timer.printElapsed("距离矩阵计算");
        timer.printElapsed("数据解析");

        Map<String, Set<String>> selected = DirectionSelector.selectDirections(data.viewpoints, data.samplePoints);

        int minCover = Integer.MAX_VALUE, maxCover = 0;
        String worstSample = null;

        for (SamplePoint s : data.samplePoints) {
            int count = s.coveringPairs.size();
            if (count < minCover) {
                minCover = count;
                worstSample = s.id;
            }
            if (count > maxCover) maxCover = count;
        }

        System.err.printf("最少可覆盖角度: %d (样本 %s)%n", minCover, worstSample);
        System.err.printf("最多可覆盖角度: %d%n", maxCover);

    }
}
