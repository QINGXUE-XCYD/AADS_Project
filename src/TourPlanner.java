import java.util.*;

/**
 * 路径规划：
 * 1) 最近邻构造初始路径（mustVisit 内的点）
 * 2) 闭环回到起点
 * 3) 用 allowedTransit 作为候选，插入单/双中转点修复 INF 边
 * 4) 返回最终路径与总路程
 */
public class TourPlanner {

    /** 返回值：路径 + 距离 */
    public static class TourResult {
        public final List<Viewpoint> tour;      // 含中转点，首尾闭环
        public final double totalDistance;

        public TourResult(List<Viewpoint> tour, double totalDistance) {
            this.tour = tour;
            this.totalDistance = totalDistance;
        }
    }

    /**
     * 构造 UAV 巡航路径（闭环）：
     *
     * @param allVps        全部视点（用于打印/调试，可不完全等于 allowedTransit）
     * @param mustVisit     必须访问的视点集合（比如 selected.keySet()）
     * @param allowedTransit 可用于中转的视点
     * @param dist          距离矩阵 dist[i][j]，基于 viewpoint.index
     */
    public static TourResult buildTour(
            List<Viewpoint> allVps,
            Collection<Viewpoint> mustVisit,
            List<Viewpoint> allowedTransit,
            double[][] dist
    ) {
        if (mustVisit == null || mustVisit.isEmpty()) {
            return new TourResult(new ArrayList<>(), 0.0);
        }

        // -------- 1. 找 mandatory 作为起点（没有就用 mustVisit 第一个） --------
        Viewpoint start = null;
        for (Viewpoint vp : allVps) {
            if (vp.isMandatory) {
                start = vp;
                break;
            }
        }
        if (start == null) {
            start = mustVisit.iterator().next();
        }

        // -------- 2. 最近邻构造初始路径（不闭环） --------
        List<Viewpoint> nnPath = buildNearestNeighborPath(mustVisit, dist, start);

        // -------- 3. 闭环：回到起点 --------
        nnPath.add(start);

        // -------- 4. Repair：插入中转点，使路径无 INF 边 --------
        List<Viewpoint> repaired = repairPath(nnPath, allowedTransit, dist);

        // -------- 5. 计算总路程 --------
        double totalDist = computePathLength(repaired, dist);

        System.out.printf("TourPlanner(NN+Repair): %d nodes, distance %.3f%n",
                repaired.size(), totalDist);

        return new TourResult(repaired, totalDist);
    }

    // ============================================================
    // 最近邻路径构造（只在 mustVisit 内找下一个点）
    // ============================================================
    private static List<Viewpoint> buildNearestNeighborPath(
            Collection<Viewpoint> mustVisit,
            double[][] dist,
            Viewpoint start
    ) {
        // 去重 + 保序
        List<Viewpoint> nodes = new ArrayList<>(new LinkedHashSet<>(mustVisit));

        // 确保包含起点
        if (!nodes.contains(start)) {
            nodes.add(0, start);
        }

        int m = nodes.size();
        boolean[] visited = new boolean[m];

        // 构建 local index 映射：0..m-1
        Map<Viewpoint, Integer> vp2local = new HashMap<>();
        for (int i = 0; i < m; i++) {
            vp2local.put(nodes.get(i), i);
        }

        int currentLocal = vp2local.get(start);
        visited[currentLocal] = true;

        List<Viewpoint> path = new ArrayList<>();
        path.add(start);

        while (path.size() < m) {
            Viewpoint currentVp = path.get(path.size() - 1);
            int gi = currentVp.index;

            int bestLocal = -1;
            double bestDist = Double.POSITIVE_INFINITY;

            for (int j = 0; j < m; j++) {
                if (visited[j]) continue;
                Viewpoint cand = nodes.get(j);
                double d = dist[gi][cand.index];
                if (d < bestDist) {
                    bestDist = d;
                    bestLocal = j;
                }
            }

            if (bestLocal == -1) {
                // 理论上在连通图中不该发生
                System.err.println("TourPlanner: 最近邻找不到下一个节点，提前结束。");
                break;
            }

            visited[bestLocal] = true;
            path.add(nodes.get(bestLocal));
        }

        return path;
    }

    // ============================================================
    // Repair：沿路径检查 INF 边，必要时插入单/双中转点
    // allowedTransit：可用来做中转的 vp（不会包含已彻底删除的点）
    // ============================================================
    private static List<Viewpoint> repairPath(
            List<Viewpoint> path,
            List<Viewpoint> allowedTransit,
            double[][] dist
    ) {
        List<Viewpoint> res = new ArrayList<>(path);
        boolean changed = true;

        while (changed) {
            changed = false;

            for (int i = 0; i < res.size() - 1; i++) {
                Viewpoint A = res.get(i);
                Viewpoint B = res.get(i + 1);

                if (dist[A.index][B.index] != Double.POSITIVE_INFINITY) {
                    continue; // 这段没问题
                }

                // ===== 尝试单中转：A -> K -> B =====
                Viewpoint K = findSingleHop(A, B, allowedTransit, dist);
                if (K != null) {
                    res.add(i + 1, K);
                    changed = true;
                    break; // 重新从头扫描
                }

                // ===== 尝试双中转：A -> K1 -> K2 -> B =====
                List<Viewpoint> two = findTwoHop(A, B, allowedTransit, dist);
                if (two != null) {
                    res.add(i + 1, two.get(0));
                    res.add(i + 2, two.get(1));
                    changed = true;
                    break; // 重新从头扫描
                }

                // 如果连双中转都找不到，说明在当前 allowedTransit 集合下无法修复这条边
                throw new RuntimeException("TourPlanner: 无法修复路径段 "
                        + A.id + " → " + B.id + "，这次删除/选择方案不可行。");
            }
        }

        return res;
    }

    // 单中转：A -> K -> B
    private static Viewpoint findSingleHop(
            Viewpoint A,
            Viewpoint B,
            List<Viewpoint> allowedTransit,
            double[][] dist
    ) {
        int Ai = A.index;
        int Bi = B.index;

        for (Viewpoint K : allowedTransit) {
            int Ki = K.index;
            if (dist[Ai][Ki] != Double.POSITIVE_INFINITY &&
                    dist[Ki][Bi] != Double.POSITIVE_INFINITY) {
                return K;
            }
        }
        return null;
    }

    // 双中转：A -> K1 -> K2 -> B
    private static List<Viewpoint> findTwoHop(
            Viewpoint A,
            Viewpoint B,
            List<Viewpoint> allowedTransit,
            double[][] dist
    ) {
        int Ai = A.index;
        int Bi = B.index;

        for (Viewpoint K1 : allowedTransit) {
            int K1i = K1.index;
            if (dist[Ai][K1i] == Double.POSITIVE_INFINITY) continue;

            for (Viewpoint K2 : allowedTransit) {
                int K2i = K2.index;

                if (dist[K1i][K2i] != Double.POSITIVE_INFINITY &&
                        dist[K2i][Bi] != Double.POSITIVE_INFINITY) {
                    return Arrays.asList(K1, K2);
                }
            }
        }
        return null;
    }

    // ============================================================
    // 路径长度计算
    // ============================================================
    private static double computePathLength(List<Viewpoint> path, double[][] dist) {
        double total = 0.0;
        for (int i = 0; i < path.size() - 1; i++) {
            Viewpoint A = path.get(i);
            Viewpoint B = path.get(i + 1);
            double d = dist[A.index][B.index];
            if (Double.isInfinite(d)) {
                // 理论上 repair 之后不应再出现
                System.err.println("Warning: 路径中仍存在 INF 边: "
                        + A.id + " → " + B.id);
            } else {
                total += d;
            }
        }
        return total;
    }
}
