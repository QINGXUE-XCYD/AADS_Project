import java.util.*;

public class AADS {

    // 在selected创建vpId -> vp的映射
    static Map<String, Viewpoint> buildVpId2VpMap(Map<Viewpoint, Set<String>> selected) {
        Map<String, Viewpoint> VpId2Vp = new HashMap<>();
        for (Viewpoint vp : selected.keySet()) {
            VpId2Vp.put(vp.id, vp);
        }
        return VpId2Vp;
    }


    public static void main(String[] args) throws Exception {
        TimerUtil timer = new TimerUtil();
        timer.start();

        InputData data = JsonParser.parseInput();
        // 数据检查
        double[][] distanceMatrix;
        if (true) {
            System.out.println("✅ Parse Success!");
            System.out.println("Viewpoints: " + data.viewpoints.size());
            System.out.println(data.viewpoints.get(0).toString());
            for (Viewpoint vp : data.viewpoints) {
                if (vp.isMandatory) {
                    System.out.println("Mandatory viewpoint: " + vp);
                }
            }
            System.out.println("Samples: " + data.samplePoints.size());
            System.out.println(data.samplePoints.get(0).toString());
            System.out.println("Directions: " + data.directions.size());
            System.out.println(data.directions.get(0).toString());
            System.out.println("Collision matrix: " + data.collisionMatrix.length);
            System.out.println("Is symmetric: " + GraphUtil.isSymmetric(data.collisionMatrix));
            timer.printElapsed("数据解析");
            distanceMatrix = GraphUtil.buildDistanceMatrix(data.viewpoints, data.collisionMatrix);
            timer.printElapsed("距离矩阵计算");
            if (!GraphUtil.isFullyReachable(data.viewpoints, data.viewpoints, data.collisionMatrix)) {
                System.err.println("⚠️ 整个图不是连通图（有区域永远到不了）。");
                List<Viewpoint> unreachable = GraphUtil.getUnreachable(data.viewpoints, data.viewpoints, data.collisionMatrix);
                unreachable.forEach(vp -> System.err.println("  不可达: " + vp.id));
            }
        }

        // 初始全覆盖
        // 1. selected 初始为全方向
        Map<Viewpoint, Set<String>> selected = selectALlDirections(data.viewpoints);
        // 2. mustVisit = 所有方向非空的视点
        Set<Viewpoint> mustVisit = new LinkedHashSet<>(data.viewpoints);
        // 3. allowedTransit = 所有视点（后续删点会从这里移除）
        // 注意必须是可修改的集合，不要用 List
        Set<Viewpoint> allowedTransit = new LinkedHashSet<>(data.viewpoints);
        // 4. deletedVPs = 初始为空
        Set<Viewpoint> deletedVPs = new HashSet<>();
        // 5. blocked sets
        Set<Viewpoint> blockedVPs = new HashSet<>();
        Set<Pair<Viewpoint, String>> blockedDirections = new HashSet<>();
        // 距离
        TourPlanner.TourResult tourResult =
                TourPlanner.buildTour(data.viewpoints, mustVisit, new ArrayList<>(allowedTransit), distanceMatrix);

        System.out.println("初始路径长度: " + tourResult.totalDistance);
        System.out.println("路径节点数(含中转): " + tourResult.tour.size());
        // 检查覆盖
        if (true) {
            System.out.println("初始全覆盖: " + selected.size());
            System.out.println("缺少覆盖: " + countCoverageLessThan3(data.samplePoints, selected));
            System.out.println("总精度: " + computeTotalPrecision(data.samplePoints, selected));
            timer.printElapsed("初始全覆盖");
        }
        double prec = computeTotalPrecision(data.samplePoints, selected);
        SolutionBuilder.writeSolutionJson(
                "solution.json",
                tourResult,
                selected,
                prec,
                data.viewpoints.size()
        );

    }

    // 初始全覆盖
    static Map<Viewpoint, Set<String>> selectALlDirections(List<Viewpoint> viewpoints) {
        Map<Viewpoint, Set<String>> selected = new HashMap<>();
        for (Viewpoint vp : viewpoints) {
            if (vp.precision != null) {
                Set<String> dirs = new HashSet<>(vp.precision.keySet());
                selected.put(vp, dirs);
            }
        }
        return selected;
    }

    // 检查单点覆盖

    static int countCoverageForSamplePoint(SamplePoint sp, Map<Viewpoint, Set<String>> selected) {
        Map<String, Viewpoint> vpId2Vp = buildVpId2VpMap(selected);
        int count = 0;
        for (CoveringPair cp : sp.coveringPairs) {
            Viewpoint vp = vpId2Vp.get(cp.viewpointId);
            if (vp == null) {
                continue;
            }
            Set<String> dirs = selected.get(vp);
            if (dirs != null && dirs.contains(cp.directionId)) {
                count++;
            }
        }
        return count;
    }

    // 检查缺少覆盖
    static int countCoverageLessThan3(List<SamplePoint> samples, Map<Viewpoint, Set<String>> selected) {
        int count = 0;
        for (SamplePoint sp : samples) {
            if (countCoverageForSamplePoint(sp, selected) < 3) {
                count++;
            }
        }
        return count;
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
    // 复制工具
    /** 深拷贝 selected: Map<Viewpoint, Set<String>> */
    public static Map<Viewpoint, Set<String>> copySelected(Map<Viewpoint, Set<String>> src) {
        Map<Viewpoint, Set<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<Viewpoint, Set<String>> e : src.entrySet()) {
            Viewpoint vp = e.getKey();
            Set<String> dirs = e.getValue();
            copy.put(vp, new LinkedHashSet<>(dirs));  // 深拷贝方向集合
        }
        return copy;
    }

    /** 拷贝 Set<Viewpoint> */
    public static Set<Viewpoint> copySet(Set<Viewpoint> src) {
        return new LinkedHashSet<>(src);
    }

    /** 拷贝 Set<Pair<Viewpoint,String>> 或其他结构 */
    public static <T> Set<T> copyGenericSet(Set<T> src) {
        return new HashSet<>(src);
    }


}

