import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AADS {
    public static void main(String[] args) throws Exception {

        TimerUtil timer = new TimerUtil();
        timer.start();

        InputData data = JsonParser.parseInput();

        System.out.println("✅ Parse Success!");
        System.out.println("Viewpoints: " + data.viewpoints.size());
        System.out.println(data.viewpoints.get(0).toString());
        for (Viewpoint vp : data.viewpoints) {
            if (vp.isMandatory){
                System.out.println("Mandatory viewpoint: " + vp.toString());
            }
        }
        System.out.println("Samples: " + data.samplePoints.size());
        System.out.println(data.samplePoints.get(0).toString());
        System.out.println("Directions: " + data.directions.size());
        System.out.println(data.directions.get(0).toString());
        System.out.println("Collision matrix: " + data.collisionMatrix.length);
        System.out.println("Is symmetric: " + GraphUtil.isSymmetric(data.collisionMatrix));
        timer.printElapsed("数据解析");
        double[][] distanceMatrix = GraphUtil.buildDistanceMatrix(data.viewpoints, data.collisionMatrix);
        timer.printElapsed("距离矩阵计算");
        checkCoverage(data);
        Map<Viewpoint, Set<String>> selectedViewpoints = DirectionSelectorV3.selectDirections(data.viewpoints,data.samplePoints);
        timer.printElapsed("方向选择");
        CoverageChecker.checkSelectedDirectionsValid(selectedViewpoints);
        CoverageChecker.checkSampleCoverage(selectedViewpoints,data.samplePoints);
        timer.printElapsed("样本覆盖检查");
        List<Viewpoint> tour = TourPlanner.buildTour(data.viewpoints,selectedViewpoints,distanceMatrix);
        System.out.println(tour);
        timer.printElapsed("路径规划");
        System.out.println(SolutionBuilder.buildSolution(tour,data.viewpoints,selectedViewpoints,distanceMatrix, data.lambda));


    }

    // check coverage constraints
    static void checkCoverage(InputData data) {
        List<SamplePoint> lessThan1 = new ArrayList<>();
        List<SamplePoint> lessThan3 = new ArrayList<>();
        data.samplePoints.forEach(samplePoint -> {
            if (samplePoint.coveringPairs.isEmpty()) {
                lessThan1.add(samplePoint);
            }
            if (samplePoint.coveringPairs.size() < 3) {
                lessThan3.add(samplePoint);
            }
        });
        System.out.println("✅ Coverage Check Success!");
        System.out.println("Covering pair less than 1: " + lessThan1.size() + " " + lessThan1);
        System.out.println("Covering pair less than 3: " + lessThan3.size() + " " + lessThan3);
    }
}

