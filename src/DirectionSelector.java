import java.util.*;

/**
 * 方向选择器：
 * 根据所有采样点的 covering_pairs，选择最优的 Viewpoint–angle 组合，
 * 满足：每个 sample 至少被 3 次覆盖，同时尽量使用更少、更高精度的角度。
 */
class DirectionSelector {

    /**
     * 主函数：选择每个 Viewpoint 的拍摄方向
     *
     * @param Viewpoints  所有视角
     * @param samples     所有采样点
     * @return Map<ViewpointId, Set<AngleId>>
     */
    static Map<String, Set<String>> selectDirections(List<Viewpoint> Viewpoints, List<SamplePoint> samples) {
        long start = System.currentTimeMillis();


        // --- 建立 ViewpointId -> Viewpoint 对象映射 ---
        Map<String, Viewpoint> vpMap = new HashMap<>();
        for (Viewpoint v : Viewpoints) vpMap.put(v.id, v);

        // --- 初始化：每个 sample 当前被覆盖次数 ---
        Map<String, Integer> coverageCount = new HashMap<>();
        for (SamplePoint s : samples) coverageCount.put(s.id, 0);

        // --- 初始化：每个 Viewpoint 已选角度集合 ---
        Map<String, Set<String>> selectedAngles = new HashMap<>();
        for (Viewpoint v : Viewpoints) selectedAngles.put(v.id, new HashSet<>());

        // --- 建立 Viewpoint–angle → 可覆盖的 sample 列表 ---
        Map<String, Map<String, List<String>>> vpAngleToSamples = new HashMap<>();
        for (SamplePoint s : samples) {
            for (CoveringPair p : s.coveringPairs) {
                vpAngleToSamples
                    .computeIfAbsent(p.viewpointId, k -> new HashMap<>())
                    .computeIfAbsent(p.directionId, k -> new ArrayList<>())
                    .add(s.id);
            }
        }

        int totalSamples = samples.size();
        int satisfied = 0;

        // --- 贪心循环：直到所有 sample 的覆盖次数 >= 3 ---
        while (satisfied < totalSamples) {
            String bestVp = null, bestAngle = null;
            int bestGain = 0;
            double bestPrecision = -Double.MAX_VALUE;

            // 遍历所有 Viewpoint–angle 对，寻找最优组合
            for (var vpEntry : vpAngleToSamples.entrySet()) {
                String vpId = vpEntry.getKey();
                for (var angleEntry : vpEntry.getValue().entrySet()) {
                    String angleId = angleEntry.getKey();

                    // 已选角度跳过
                    if (selectedAngles.get(vpId).contains(angleId)) continue;

                    List<String> coverable = angleEntry.getValue();
                    int gain = 0;
                    for (String sid : coverable) {
                        if (coverageCount.get(sid) < 3) gain++;
                    }

                    if (gain == 0) continue;

                    // 当前角度的平均精度
                    double precision = vpMap.get(vpId).precision.getOrDefault(angleId, 0.0);

                    // 贪心选择：优先覆盖更多未满足样本，其次看精度
                    if (gain > bestGain || (gain == bestGain && precision > bestPrecision)) {
                        bestVp = vpId;
                        bestAngle = angleId;
                        bestGain = gain;
                        bestPrecision = precision;
                    }
                }
            }

            // 若无法找到新角度，说明部分 sample 永远无法达到3次覆盖
            if (bestVp == null) {
                System.err.println("⚠️ 警告：无法进一步满足覆盖约束，部分样本不可覆盖。");
                break;
            }

            // 选中该方向
            selectedAngles.get(bestVp).add(bestAngle);

            // 更新 coverageCount
            for (String sid : vpAngleToSamples.get(bestVp).get(bestAngle)) {
                coverageCount.put(sid, coverageCount.get(sid) + 1);
            }

            // 更新满足数量
            satisfied = 0;
            for (int count : coverageCount.values()) {
                if (count >= 1) satisfied++;
            }

            if (satisfied % 10 == 0 || satisfied == totalSamples) {
                System.err.printf("当前覆盖进度：%d / %d 样本已满足%n", satisfied, totalSamples);
            }
        }

        long end = System.currentTimeMillis();
        System.err.printf("✅ 覆盖要求完成！总耗时：%d ms%n", (end - start));

        int totalSelected = 0;
        for (var e : selectedAngles.entrySet()) totalSelected += e.getValue().size();
        System.err.printf("选中总方向数量：%d%n", totalSelected);

        return selectedAngles;
    }
}
