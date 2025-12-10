import java.util.*;

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
        if (!GraphUtil.isFullyReachable(data.viewpoints, data.viewpoints, data.collisionMatrix)) {
            System.err.println("⚠️ 整个图不是连通图（有区域永远到不了）。");
            List<Viewpoint> unreachable = GraphUtil.getUnreachable(data.viewpoints, data.viewpoints, data.collisionMatrix);
            unreachable.forEach(vp -> System.err.println("  不可达: " + vp.id));
        }
        Map<Viewpoint, Set<String>> selectedViewpoints = DirectionSelectorV4.selectDirections(data.viewpoints,data.samplePoints);
        timer.printElapsed("方向选择");
        CoverageChecker.checkSelectedDirectionsValid(selectedViewpoints);
        CoverageChecker.checkSampleCoverage(selectedViewpoints,data.samplePoints);
        timer.printElapsed("样本覆盖检查");
        TourPlanner.TourResult finalTour = TourPlanner.buildTour(
                data.viewpoints,
                selectedViewpoints.keySet(),
                new ArrayList<>(selectedViewpoints.keySet()),
                distanceMatrix
        );
        System.out.println("路线长度"+finalTour.totalDistance);

        timer.printElapsed("路径规划");
        double finalPrecision = computeTotalPrecision(data.samplePoints, selectedViewpoints);
        System.out.println("Final precision: " + finalPrecision);
        timer.printElapsed("精度计算");
        SolutionBuilder.writeSolutionJson(
                "solution.json",
                finalTour,
                selectedViewpoints,
                finalPrecision,
                data.viewpoints.size()
        );



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

    // 在selected创建vpId -> vp的映射
    static Map<String, Viewpoint> buildVpId2VpMap(Map<Viewpoint, Set<String>> selected) {
        Map<String, Viewpoint> VpId2Vp = new HashMap<>();
        for (Viewpoint vp : selected.keySet()) {
            VpId2Vp.put(vp.id, vp);
        }
        return VpId2Vp;
    }
    // 计算精度

    static double computeTotalPrecision(
            List<SamplePoint> samples,
            Map<Viewpoint, Set<String>> selected
    ) {
        Map<String, Viewpoint> vpId2Vp = buildVpId2VpMap(selected);
        double totalPrecision = 0;
        for (SamplePoint sp : samples) {
            for (CoveringPair cp : sp.coveringPairs) {

                Viewpoint vp = vpId2Vp.get(cp.viewpointId);
                if (vp == null) continue;
                Set<String> dirs = selected.get(vp);
                if (dirs == null || !dirs.contains(cp.directionId)) continue;
                Double p = vp.precision.get(cp.directionId);
                if (p != null) {
                    totalPrecision += p;   // ⭐ 全加
                }
            }
        }
        return totalPrecision;
    }
}

