import java.util.List;

class MathUtil {
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

        long start = System.currentTimeMillis();

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

        long end = System.currentTimeMillis();
        System.err.println("✅ 距离矩阵构建完成，用时: " + (end - start) + " ms");
        return dist;
    }
}
