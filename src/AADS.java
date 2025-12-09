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
        // 1. 找到 mandatory 起点，加入 blockedVPs
        Viewpoint mandatory = null;
        for (Viewpoint vp : data.viewpoints) {
            if (vp.isMandatory) {
                mandatory = vp;
                break;
            }
        }
        if (mandatory != null) blockedVPs.add(mandatory);

// ========================
// 计时器，限制最多 90 秒
// ========================
        long startTime = System.currentTimeMillis();
        long timeLimitMs = 90_000;

// ========================
// 初始精度与路径距离
// ========================
        double oldPrecision = computeTotalPrecision(data.samplePoints, selected);

        TourPlanner.TourResult oldTour = TourPlanner.buildTour(
                new ArrayList<>(allowedTransit),
                mustVisit,
                new ArrayList<>(allowedTransit),
                distanceMatrix
        );
        double oldDistance = oldTour.totalDistance;

// ========================
// 主循环：贪心删除
// ========================
        while (true) {

            // 时间检查
            long now = System.currentTimeMillis();
            if (now - startTime > timeLimitMs) {
                System.out.println("时间到，停止删除优化");
                break;
            }

            double bestGain = -1e18;
            DeleteResult bestDelete = null;


            // ==========================================
            // 1) 尝试删除整个视点 vp
            // ==========================================

            for (Viewpoint vp : new ArrayList<>(mustVisit)) {

                // mandatory 或已 block 的不删
                if (blockedVPs.contains(vp)) continue;

                DeleteResult res = tryDeleteViewpoint(
                        vp,
                        selected, mustVisit, allowedTransit,
                        data.samplePoints,
                        distanceMatrix
                );

                if (!res.feasible) {
                    blockedVPs.add(vp);
                    continue;
                }

                if (res.gain > bestGain) {
                    bestGain = res.gain;
                    bestDelete = res;
                }
            }
// ==========================================
            // 2) 尝试删除所有方向 (vp,dir)
            // ==========================================
            for (Map.Entry<Viewpoint, Set<String>> entry : selected.entrySet()) {
                Viewpoint vp = entry.getKey();

                // 忽略 block 的点
                if (blockedVPs.contains(vp)) continue;

                for (String dir : entry.getValue()) {
                    Pair<Viewpoint,String> key = new Pair<>(vp, dir);
                    if (blockedDirections.contains(key)) continue;

                    DeleteResult res = tryDeleteDirection(
                            vp, dir,
                            selected, mustVisit, allowedTransit,
                            data.samplePoints, distanceMatrix
                    );

                    if (!res.feasible) {
                        // 不可删 → 永久 block
                        blockedDirections.add(key);
                        continue;
                    }

                    if (res.gain > bestGain) {
                        bestGain = res.gain;
                        bestDelete = res;
                    }
                }
            }

            // ==========================================
            // 没有找到可收益的删除动作，退出
            // ==========================================
            if (bestDelete == null || bestGain <= 0) {
                System.out.println("无正收益删除，退出优化循环");
                break;
            }

            // ==========================================
            // 应用最佳删除动作
            // ==========================================
            selected = bestDelete.newSelected;
            mustVisit = bestDelete.newMustVisit;
            allowedTransit = bestDelete.newAllowedTransit;

            // 更新当前精度与距离
            oldPrecision = computeTotalPrecision(data.samplePoints, selected);
            oldTour = TourPlanner.buildTour(
                    new ArrayList<>(allowedTransit),
                    mustVisit,
                    new ArrayList<>(allowedTransit),
                    distanceMatrix
            );
            oldDistance = oldTour.totalDistance;

            System.out.printf("执行删除：%s, gain=%.3f 当前距离=%.3f 当前精度=%.3f%n",
                    bestDelete.actionInfo, bestGain, oldDistance, oldPrecision);
        }
        // ============= 删除完毕，构建最终路径 =============
        TourPlanner.TourResult finalTour = TourPlanner.buildTour(
                new ArrayList<>(allowedTransit),
                mustVisit,
                new ArrayList<>(allowedTransit),
                distanceMatrix
        );

// 计算最终精度
        double finalPrecision = computeTotalPrecision(data.samplePoints, selected);

// 输出 JSON
        SolutionBuilder.writeSolutionJson(
                "solution.json",
                finalTour,
                selected,
                finalPrecision,
                data.viewpoints.size()
        );

        System.out.println("优化完成！最终距离=" + finalTour.totalDistance +
                " 最终精度=" + finalPrecision +
                "  输出已写入 solution.json");

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

    // 删除
    static DeleteResult tryDeleteDirection(
            Viewpoint vp,
            String dir,
            Map<Viewpoint, Set<String>> selected,
            Set<Viewpoint> mustVisit,
            Set<Viewpoint> allowedTransit,
            List<SamplePoint> samples,
            double[][] distanceMatrix
    ) {
        // 1. deep copy current state
        Map<Viewpoint, Set<String>> selCopy = copySelected(selected);
        Set<Viewpoint> mvCopy = copySet(mustVisit);
        Set<Viewpoint> atCopy = copySet(allowedTransit);

        // ---------- 2. 删除该方向 ----------
        Set<String> dirs = selCopy.get(vp);
        if (dirs == null || !dirs.contains(dir)) {
            // direction not present → treat as infeasible
            return DeleteResult.infeasible();
        }
        dirs.remove(dir);

        // ---------- 3. 若该 vp 方向删空 → 它变 transit-only ----------
        if (dirs.isEmpty()) {
            // 移出 mustVisit
            mvCopy.remove(vp);
            // 但仍然允许作为 transit 点
            atCopy.add(vp);
        }

        // ---------- 4. 检查 coverage ----------
        int missing = countCoverageLessThan3(samples, selCopy);
        if (missing > 0) {
            return DeleteResult.infeasible(); // 不能删
        }

        // ---------- 5. 跑 TourPlanner，看路径是否可行 ----------
        TourPlanner.TourResult newTour;
        try {
            newTour = TourPlanner.buildTour(
                    new ArrayList<>(atCopy), // 传入 allowedTransit 的 list
                    mvCopy,
                    new ArrayList<>(atCopy),
                    distanceMatrix
            );
        } catch (Exception e) {
            // repair 失败等情况
            return DeleteResult.infeasible();
        }

        double newDistance = newTour.totalDistance;
        double newPrecision = computeTotalPrecision(samples, selCopy);

        // ---------- 6. 计算 gain ----------
        double oldDistance = TourPlanner.buildTour(
                new ArrayList<>(allowedTransit),
                mustVisit,
                new ArrayList<>(allowedTransit),
                distanceMatrix
        ).totalDistance;

        double oldPrecision = computeTotalPrecision(samples, selected);

        double deltaPrecision = newPrecision - oldPrecision;
        double deltaDistance = newDistance - oldDistance;

        double gain = deltaPrecision - deltaDistance;

        // ---------- 7. 成功删除，返回结果 ----------
        return DeleteResult.feasible(
                gain,
                selCopy,
                mvCopy,
                atCopy,
                "Delete direction: " + vp.id + "-" + dir
        );
    }

    static DeleteResult tryDeleteViewpoint(
            Viewpoint vp,
            Map<Viewpoint, Set<String>> selected,
            Set<Viewpoint> mustVisit,
            Set<Viewpoint> allowedTransit,
            List<SamplePoint> samples,
            double[][] distanceMatrix
    ) {
        // 如果是强制点，直接判定不可删（保险一点）
        if (vp.isMandatory) {
            return DeleteResult.infeasible();
        }

        // 1. 深拷贝当前状态
        Map<Viewpoint, Set<String>> selCopy = copySelected(selected);
        Set<Viewpoint> mvCopy = copySet(mustVisit);
        Set<Viewpoint> atCopy = copySet(allowedTransit);

        // 2. 从拷贝中移除这个视点
        // 2.1 删除该视点的所有方向
        selCopy.remove(vp);

        // 2.2 从 mustVisit 中删除
        mvCopy.remove(vp);

        // 2.3 从 allowedTransit 中删除（彻底不能作为中转）
        atCopy.remove(vp);

        // 3. 检查 coverage 是否仍然满足 >= 3
        int missing = countCoverageLessThan3(samples, selCopy);
        if (missing > 0) {
            // 删这个点会导致覆盖不足 → 不可删
            return DeleteResult.infeasible();
        }

        // 4. 尝试用新的 mustVisit/allowedTransit 构造路径
        TourPlanner.TourResult newTour;
        try {
            newTour = TourPlanner.buildTour(
                    new ArrayList<>(atCopy),   // allVps：这里用当前仍可用的点集合
                    mvCopy,                    // 必须访问的点
                    new ArrayList<>(atCopy),   // allowedTransit
                    distanceMatrix
            );
        } catch (Exception e) {
            // repair 失败或其他异常 → 当前删除方案不可行
            return DeleteResult.infeasible();
        }

        double newDistance = newTour.totalDistance;
        double newPrecision = computeTotalPrecision(samples, selCopy);

        // 5. 计算删除前的精度和距离
        TourPlanner.TourResult oldTour;
        try {
            oldTour = TourPlanner.buildTour(
                    new ArrayList<>(allowedTransit),
                    mustVisit,
                    new ArrayList<>(allowedTransit),
                    distanceMatrix
            );
        } catch (Exception e) {
            // 理论上不应该发生：当前解本身就不可行
            return DeleteResult.infeasible();
        }

        double oldDistance = oldTour.totalDistance;
        double oldPrecision = computeTotalPrecision(samples, selected);

        double deltaPrecision = newPrecision - oldPrecision;
        double deltaDistance = newDistance - oldDistance;

        double gain = deltaPrecision - deltaDistance;

        // 6. 返回可行删除结果
        return DeleteResult.feasible(
                gain,
                selCopy,
                mvCopy,
                atCopy,
                "Delete direction: " + vp.id
        );
    }

}

