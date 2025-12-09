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

        // --------- 初始化 ---------
        double lambda = data.lambda;  // 例如 input 中给出的 100
        long startTime = System.currentTimeMillis();
        long timeLimitMs = 999_000;    // 90 秒

// mustVisit 不删点，因此它永远是全部视点
        Set<Viewpoint> mustVisit = new LinkedHashSet<>(data.viewpoints);

// allowedTransit 也永远是全部视点
        Set<Viewpoint> allowedTransit = new LinkedHashSet<>(data.viewpoints);

// selected 初始全方向
        Map<Viewpoint, Set<String>> selected = selectALlDirections(data.viewpoints);

// 计算初始精度和距离
        double currentPrecision = computeTotalPrecision(data.samplePoints, selected);
        double currentDistance  = TourPlanner.buildTour(
                data.viewpoints,
                mustVisit,
                new ArrayList<>(allowedTransit),
                distanceMatrix
        ).totalDistance;

// 方向不可删列表
        Set<Pair<Viewpoint,String>> blockedDirs = new HashSet<>();

        System.out.println("Start delete-direction optimization...");


// ------------ 主循环开始 --------------
        while (true) {

            // 时间限制
            if (System.currentTimeMillis() - startTime > timeLimitMs) {
                System.out.println("Time exceeded, stop optimization.");
                break;
            }

            // -------- Step 1：收集所有方向并计算 DirScore --------
            List<Triple<Viewpoint,String,Double>> dirList = new ArrayList<>();

            for (var e : selected.entrySet()) {
                Viewpoint vp = e.getKey();
                for (String dir : e.getValue()) {
                    double score = computeDirScore(vp, dir, data.samplePoints);
                    dirList.add(new Triple<>(vp, dir, score));
                }
            }

            // 若没有方向可删则结束
            if (dirList.isEmpty()) break;

            // -------- Step 2：按 DirScore 从小到大排序（越小越可删） --------
            dirList.sort(Comparator.comparingDouble(t -> t.c()));

            // 只尝试前 30% 的方向
            int K = (int)(dirList.size() * 0.3);
            if (K < 1) K = 1;
            dirList = dirList.subList(0, K);

            // 打乱顺序避免某个 VP 连删
            Collections.shuffle(dirList);

            // -------- Step 3：尝试删这些方向 --------
            double bestGain = -1e18;
            DeleteResult bestDelete = null;

            for (Triple<Viewpoint,String,Double> t : dirList) {

                Viewpoint vp = t.a();
                String dir = t.b();

                Pair<Viewpoint,String> key = new Pair<>(vp, dir);
                if (blockedDirs.contains(key)) continue;

                DeleteResult res = tryDeleteDirection(
                        vp, dir,
                        selected, mustVisit, allowedTransit,
                        data.samplePoints,
                        distanceMatrix,
                        currentPrecision,
                        currentDistance,
                        lambda
                );

                if (!res.feasible) {
                    blockedDirs.add(key);
                    continue;
                }

                if (res.gain > bestGain) {
                    bestGain = res.gain;
                    bestDelete = res;
                }
            }

            // -------- Step 4：判断是否有可行且有收益的删除动作 --------
            if (bestDelete == null || bestGain <= 0) {
                System.out.println("No more positive-gain deletions, stop.");
                break;
            }

            // -------- Step 5：应用删除动作 --------
            selected = bestDelete.newSelected;
            mustVisit = bestDelete.newMustVisit;
            allowedTransit = bestDelete.newAllowedTransit;

            currentPrecision = computeTotalPrecision(data.samplePoints, selected);
            currentDistance = TourPlanner.buildTour(
                    data.viewpoints,
                    mustVisit,
                    new ArrayList<>(allowedTransit),
                    distanceMatrix
            ).totalDistance;

            System.out.printf("Delete: %s (gain=%.3f), distance=%.3f, precision=%.3f%n",
                    bestDelete.actionInfo, bestGain, currentDistance, currentPrecision);
        }

        System.out.println("Optimization finished.");
        TourPlanner.TourResult finalTour = TourPlanner.buildTour(
                data.viewpoints,
                mustVisit,
                new ArrayList<>(allowedTransit),
                distanceMatrix
        );

        double finalPrecision = computeTotalPrecision(data.samplePoints, selected);

        SolutionBuilder.writeSolutionJson(
                "solution.json",
                finalTour,
                selected,
                finalPrecision,
                data.viewpoints.size()
        );

        System.out.println("Final distance=" + finalTour.totalDistance
                + " precision=" + finalPrecision);

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

    // 删除方向（启发式使用）
    static DeleteResult tryDeleteDirection(
            Viewpoint vp,
            String dir,
            Map<Viewpoint, Set<String>> selected,
            Set<Viewpoint> mustVisit,
            Set<Viewpoint> allowedTransit,
            List<SamplePoint> samples,
            double[][] distanceMatrix,
            double currentPrecision,   // 当前主循环提供
            double currentDistance,    // 当前主循环提供
            double lambda
    ) {
        // 1. deep copy current state
        Map<Viewpoint, Set<String>> selCopy = copySelected(selected);
        Set<Viewpoint> mvCopy = copySet(mustVisit);
        Set<Viewpoint> atCopy = copySet(allowedTransit);

        // ---------- 2. 删除该方向 ----------
        Set<String> dirs = selCopy.get(vp);
        if (dirs == null || !dirs.contains(dir)) {
            return DeleteResult.infeasible();
        }
        dirs.remove(dir);

        // ---------- 3. 若该 vp 方向删空 → 它变 transit-only ----------
        if (dirs.isEmpty()) {
            mvCopy.remove(vp);   // 不再强制访问
            atCopy.add(vp);      // 允许中转（如果你不想中转，也可以不加）
        }

        // ---------- 4. 检查 coverage ----------
        if (countCoverageLessThan3(samples, selCopy) > 0) {
            return DeleteResult.infeasible();
        }

        // ---------- 5. 构建新的 Tour ----------
        TourPlanner.TourResult newTour;
        try {
            newTour = TourPlanner.buildTour(
                    new ArrayList<>(atCopy),     // allowedTransit = 所有点
                    mvCopy,
                    new ArrayList<>(atCopy),
                    distanceMatrix
            );
        } catch (Exception e) {
            return DeleteResult.infeasible();
        }

        double newDistance = newTour.totalDistance;

        // ---------- 6. 新 precision ----------
        double newPrecision = computeTotalPrecision(samples, selCopy);

        // ---------- 7. Δ值 ----------
        double deltaPrecision = newPrecision - currentPrecision;
        double deltaDistance  = newDistance  - currentDistance;

        // ---------- 8. 主目标函数增益 ----------
        double gain = lambda * deltaPrecision - deltaDistance;

        // ---------- 9. 返回成功删除 ----------
        return DeleteResult.feasible(
                gain,
                selCopy,
                mvCopy,
                atCopy,
                "Delete direction: " + vp.id + "-" + dir
        );
    }
    // 启发
    static double computeDirScore(
            Viewpoint vp,
            String dir,
            List<SamplePoint> samples
    ) {
        int count = 0;

        // 统计覆盖次数
        for (SamplePoint sp : samples) {
            for (CoveringPair cp : sp.coveringPairs) {
                if (cp.viewpointId.equals(vp.id) && cp.directionId.equals(dir)) {
                    count++;
                }
            }
        }

        // 自身精度
        double prec = vp.precision.get(dir);

        // DirScore = 覆盖次数 × 精度
        return count * prec;
    }
    public record Triple<A,B,C>(A a, B b, C c) {}

}

