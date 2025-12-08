import java.util.*;

class GraphUtil {
    static boolean isSymmetric(int[][] matrix) {
        int n = matrix.length;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (matrix[i][j] != matrix[j][i]) {
                    System.err.printf("⚠️ 矩阵不对称: [%d][%d]=%d, [%d][%d]=%d%n",
                            i, j, matrix[i][j], j, i, matrix[j][i]);
                    return false;
                }
            }
        }
        return true;
    }

    // 平方距离法计算欧氏距离
    static double euclideanDistance(Viewpoint a, Viewpoint b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // 构建距离矩阵
    static double[][] buildDistanceMatrix(List<Viewpoint> vps, int[][] collisionMatrix) {
        int n = vps.size();
        double[][] dist = new double[n][n];

        for (int i = 0; i < n; i++) {
            dist[i][i] = 0.0;
            Viewpoint vi = vps.get(i);

            for (int j = i + 1; j < n; j++) {
                int val = collisionMatrix[i][j];
                double d;
                if (val == 1) {
                    d = euclideanDistance(vi, vps.get(j));
                } else if (val == -1) {
                    d = Double.POSITIVE_INFINITY;
                } else { // val == 0 (自身)
                    d = 0.0;
                }
                dist[i][j] = dist[j][i] = d;
            }
        }
        return dist;
    }

    // 检查可达
    static boolean isFullyReachable(
            List<Viewpoint> subset,
            List<Viewpoint> allVps,
            int[][] collisionMatrix
    ) {
        return getUnreachable(subset, allVps, collisionMatrix).isEmpty();
    }

    static List<Viewpoint> getUnreachable(
            List<Viewpoint> subset,
            List<Viewpoint> allVps,
            int[][] collisionMatrix
    ) {
        int n = allVps.size();
        double[][] dist = GraphUtil.buildDistanceMatrix(allVps, collisionMatrix);

        // ---- 找起点（mandatory）----
        Viewpoint start = null;
        for (Viewpoint vp : allVps) {
            if (vp.isMandatory) {
                start = vp;
                break;
            }
        }
        if (start == null) {
            System.err.println("⚠️ Warning: no mandatory viewpoint found. Using allVps[0] as start.");
            start = allVps.get(0);
        }

        // ---- 建立 Viewpoint → global index 映射 ----
        Map<Viewpoint, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(allVps.get(i), i);

        int startIdx = idx.get(start);

        // ---- BFS 找 reachable 区域 ----
        boolean[] reachable = new boolean[n];
        ArrayDeque<Integer> q = new ArrayDeque<>();

        reachable[startIdx] = true;
        q.add(startIdx);

        while (!q.isEmpty()) {
            int u = q.poll();
            for (int v = 0; v < n; v++) {
                if (!reachable[v] && dist[u][v] < Double.POSITIVE_INFINITY) {
                    reachable[v] = true;
                    q.add(v);
                }
            }
        }

        // ---- 找出 subset 中的不可达点 ----
        List<Viewpoint> unreachable = new ArrayList<>();
        for (Viewpoint vp : subset) {
            int gi = idx.get(vp);
            if (!reachable[gi]) {
                unreachable.add(vp);
            }
        }

        return unreachable;
    }
}
