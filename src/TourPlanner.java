import java.util.*;

class TourPlanner {

    static List<Viewpoint> buildTour(
            List<Viewpoint> allVps,
            Map<Viewpoint, Set<String>> selected,
            int[][] collisionMatrix
    ) {

        List<Viewpoint> selectedVps = new ArrayList<>(selected.keySet());
        List<Viewpoint> unreachable = GraphUtil.getUnreachable(
                selectedVps,
                allVps,
                collisionMatrix
        );

        if (!unreachable.isEmpty()) {
            System.err.println("⚠️ 在 selected 中有 viewpoint 从起点不可达:");
            unreachable.forEach(vp -> System.err.println("  " + vp.id));
        }
        int n = allVps.size();
        double[][] dist = GraphUtil.buildDistanceMatrix(allVps, collisionMatrix);

        // -------------------------
        // 1) 找到 start viewpoint
        // -------------------------
        Viewpoint startVp = null;
        for (Viewpoint vp : allVps) {
            if (vp.isMandatory) {
                startVp = vp;
                break;
            }
        }
        if (startVp == null) {
            System.err.println("⚠️ TourPlanner: 没有 mandatory viewpoint，使用第一个作为起点");
            startVp = allVps.get(0);
        }

        // -------------------------
        // 2) 构造必须访问的点集合 needVisit
        // -------------------------
        Set<Viewpoint> needVisitSet = new LinkedHashSet<>();
        needVisitSet.add(startVp);
        needVisitSet.addAll(selected.keySet());

        // -------------------------
        // 3) 将 needVisitSet 映射成局部下标（globalIdx）
        // -------------------------
        Map<Viewpoint, Integer> globalIdx = new HashMap<>();
        for (int i = 0; i < n; i++) {
            globalIdx.put(allVps.get(i), i);
        }

        // 需要访问的 viewpoint 在全局矩阵中的下标
        List<Integer> nodes = new ArrayList<>();
        for (Viewpoint vp : needVisitSet) {
            nodes.add(globalIdx.get(vp));
        }

        int m = nodes.size();
        if (m == 0) {
            return new ArrayList<>();
        }

        // -------------------------
        // 4) 构建子图的 dist_sub（m x m）
        // -------------------------
        double[][] distSub = new double[m][m];
        for (int i = 0; i < m; i++) {
            int gi = nodes.get(i);
            for (int j = 0; j < m; j++) {
                int gj = nodes.get(j);
                distSub[i][j] = dist[gi][gj];
            }
        }

        // -------------------------
        // 5) 在 distSub 上构建 MST（使用 Prim）
        // -------------------------
        // mstEdges[i] = 这个节点的父节点
        int[] mstParent = new int[m];
        Arrays.fill(mstParent, -1);

        double[] key = new double[m];
        boolean[] inMST = new boolean[m];
        Arrays.fill(key, Double.POSITIVE_INFINITY);

        // start local index（局部）
        int startLocal = nodes.indexOf(globalIdx.get(startVp));
        if (startLocal < 0) startLocal = 0;

        key[startLocal] = 0;

        for (int count = 0; count < m - 1; count++) {
            // 找未加入 MST 的最小 key
            double minKey = Double.POSITIVE_INFINITY;
            int u = -1;

            for (int i = 0; i < m; i++) {
                if (!inMST[i] && key[i] < minKey) {
                    minKey = key[i];
                    u = i;
                }
            }
            if (u == -1) break;

            inMST[u] = true;

            // 更新相邻节点 key
            for (int v = 0; v < m; v++) {
                if (!inMST[v] && distSub[u][v] < key[v]) {
                    mstParent[v] = u;
                    key[v] = distSub[u][v];
                }
            }
        }

        // -------------------------
        // 6) 把 MST 转成邻接表
        // -------------------------
        List<List<Integer>> mstAdj = new ArrayList<>();
        for (int i = 0; i < m; i++) mstAdj.add(new ArrayList<>());

        for (int v = 0; v < m; v++) {
            int p = mstParent[v];
            if (p >= 0) {
                mstAdj.get(p).add(v);
                mstAdj.get(v).add(p);
            }
        }

        // -------------------------
        // 7) DFS 遍历 MST 得到路径顺序
        // -------------------------
        List<Integer> tourLocal = new ArrayList<>();
        boolean[] visited = new boolean[m];

        dfsMST(startLocal, mstAdj, visited, tourLocal);

        // 最后回到起点
        tourLocal.add(startLocal);

        // -------------------------
        // 8) 映射回 viewpoint 对象
        // -------------------------
        List<Viewpoint> tour = new ArrayList<>();
        for (int localIdx : tourLocal) {
            int global = nodes.get(localIdx);
            tour.add(allVps.get(global));
        }

        // -------------------------
        // 9) 打印结果
        // -------------------------
        double totalDist = 0;
        for (int i = 0; i + 1 < tour.size(); i++) {
            int a = globalIdx.get(tour.get(i));
            int b = globalIdx.get(tour.get(i + 1));
            totalDist += dist[a][b];
        }
        System.out.printf("TourPlanner(MST+DFS): path %d nodes, distance %.3f%n",
                tour.size(), totalDist);

        return tour;
    }

    // DFS 辅助函数
    private static void dfsMST(int u,
                               List<List<Integer>> adj,
                               boolean[] visited,
                               List<Integer> route) {
        visited[u] = true;
        route.add(u);

        for (int v : adj.get(u)) {
            if (!visited[v]) {
                dfsMST(v, adj, visited, route);
            }
        }
    }
}