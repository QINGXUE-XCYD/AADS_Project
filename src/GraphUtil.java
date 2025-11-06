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
}
