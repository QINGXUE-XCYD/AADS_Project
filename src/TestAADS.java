import java.io.*;
import java.util.*;

/**
 * 简化测试类：验证解析 + 覆盖 + 性能
 */
public class TestAADS {
    public static void main(String[] args) {
        try {

            InputData data = JsonParser.parseInput(System.in);

            // Step 4️⃣: 打印基本信息
            System.out.printf("视点数量: %d, 样本数量: %d, 矩阵维度: %d\n",
                    data.viewpoints.size(),
                    data.samplePoints.size(),
                    data.collisionMatrix.length);

            // Step 5️⃣: 测试方向选择模块
            long t4 = System.currentTimeMillis();
            Map<String, Set<String>> selected = DirectionSelectorV2.selectDirections(
                    data.viewpoints, data.samplePoints);
            long t5 = System.currentTimeMillis();
            System.out.println("[INFO] ✅ 方向选择完成 (" + (t5 - t4) + " ms)");

            // Step 6️⃣: 验证覆盖结果
            checkCoverage(data, selected);

        } catch (Exception e) {
            System.err.println("[ERROR] 程序运行异常：");
            e.printStackTrace();
        }
    }


    // 验证每个 sample 是否至少被 3 次覆盖
    private static void checkCoverage(InputData data, Map<String, Set<String>> selected) {
        // 建立索引
        Map<String, Set<String>> selectedPairs = new HashMap<>();
        for (var vpEntry : selected.entrySet()) {
            for (String angle : vpEntry.getValue()) {
                selectedPairs
                        .computeIfAbsent(vpEntry.getKey(), k -> new HashSet<>())
                        .add(angle);
            }
        }

        int satisfied = 0;
        for (SamplePoint s : data.samplePoints) {
            int count = 0;
            for (CoveringPair cp : s.coveringPairs) {
                if (selectedPairs.containsKey(cp.viewpointId)
                        && selectedPairs.get(cp.viewpointId).contains(cp.directionId)) {
                    count++;
                }
            }
            if (count >= 3) satisfied++;
        }

        System.out.printf("[INFO] 覆盖满足的样本: %d / %d%n",
                satisfied, data.samplePoints.size());

        if (satisfied == data.samplePoints.size()) {
            System.out.println("✅ 所有样本均满足 ≥3 次覆盖约束！");
        } else {
            System.out.printf("⚠️ 有 %d 个样本未满足 ≥3 覆盖%n",
                    data.samplePoints.size() - satisfied);
        }
    }
}
