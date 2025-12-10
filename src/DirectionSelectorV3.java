import java.util.*;

class DirectionSelectorV3 {
    static Map<Viewpoint, Set<String>> selectDirections(
            List<Viewpoint> viewpoints,
            List<SamplePoint> samplePoints
    ) {
        // 构建对象映射
        // viewpointId -> viewpoint
        Map<String, Viewpoint> vpMap = new HashMap<>();
        for (Viewpoint vp : viewpoints) {
            vpMap.put(vp.id, vp);
        }
        // (viewpointId, directionId) -> precision
        Map<String, Map<String, Double>> precisionMap = new HashMap<>();
        for (Viewpoint vp : viewpoints) {
            if (vp.precision != null) {
                Map<String, Double> innerMap = new HashMap<>(vp.precision);
                precisionMap.put(vp.id, innerMap);
            }
        }
        // (viewpointId, directionId) -> coverSamples
        Map<String, Map<String, Set<String>>> coverMap = new HashMap<>();
        for (SamplePoint sp : samplePoints) {
            if (sp.coveringPairs == null) {
                continue;
            }
            for (CoveringPair cp : sp.coveringPairs) {
                String viewpointId = cp.viewpointId;
                String directionId = cp.directionId;
                coverMap
                        .computeIfAbsent(viewpointId, k -> new HashMap<>())
                        .computeIfAbsent(directionId, k -> new HashSet<>())
                        .add(sp.id);
            }
        }
        Map<String, Integer> need = new HashMap<>();
        samplePoints.forEach(sp -> need.put(sp.id, 3));
        // 贪心
        Map<Viewpoint, Set<String>> result = new HashMap<>();
        int iteration = 0;
        while (true) {
            // 计算当前需要覆盖的样本
            int totalNeed = 0;
            int unsatisfiedCount = 0;
            for (int val : need.values()) {
                if (val > 0) {
                    totalNeed += val;
                    unsatisfiedCount++;
                }
            }
            if (totalNeed == 0) {
                System.err.println("✅ All samples are covered!");
                break;
            }

            // 查找最优
            String bestVpId = null;
            String bestDirId = null;
            int bestCovered = 0;
            for (Map.Entry<String, Map<String, Set<String>>> vpEntry : coverMap.entrySet()) {
                String vpId = vpEntry.getKey();
                Map<String, Set<String>> dirMap = vpEntry.getValue();
                if (dirMap == null) {
                    continue;
                }
                for (Map.Entry<String, Set<String>> dirEntry : dirMap.entrySet()) {
                    String dirId = dirEntry.getKey();
                    Set<String> coveredSamples = dirEntry.getValue();
                    if (coveredSamples == null || coveredSamples.isEmpty()) {
                        continue;
                    }
                    int covered = 0;
                    for (String sampleId : coveredSamples) {
                        int needVal = need.get(sampleId);
                        if (needVal > 0) {
                            covered++;
                        }
                    }
                    if (covered > bestCovered) {
                        bestCovered = covered;
                        bestVpId = vpId;
                        bestDirId = dirId;
                    }
                }
            }
            if (bestCovered == 0 || bestVpId == null || bestDirId == null) {
                System.err.println("⚠️ DirectionSelectorV3: 无法再提升覆盖度，但仍有 "
                        + unsatisfiedCount + " 个样本 need>0，提前结束。");
                break;
            }
            iteration++;
            // 更新result
            Viewpoint chosenVp = vpMap.get(bestVpId);
            if (chosenVp == null) {
                System.err.println("⚠️ DirectionSelectorV3: 找不到id为 " + bestVpId + " 的 viewpoint");
            } else {
                result
                        .computeIfAbsent(chosenVp, k -> new HashSet<>())
                        .add(bestDirId);
            }
            // 更新need
            Set<String> coveredSamples = coverMap.get(bestVpId).get(bestDirId);
            if (coveredSamples != null) {
                for (String sampleId : coveredSamples) {
                    int needVal = need.get(sampleId);
                    if (needVal > 0) {
                        need.put(sampleId, needVal - 1);
                    }
                }
            }
            // 删除已使用的(vpId, dirId)
            Map<String, Set<String>> dirMapUsed = coverMap.get(bestVpId);
            if (dirMapUsed != null) {
                dirMapUsed.remove(bestDirId);
                if (dirMapUsed.isEmpty()) {
                    coverMap.remove(bestVpId);
                }
            }
            // Log
            if (iteration % 20 == 0 || iteration == 1) {
                int satisfiedCount = 0;
                for (int val : need.values()) {
                    if (val == 0) {
                        satisfiedCount++;
                    }
                }
                System.err.printf(
                        "DirectionSelectorV3: 迭代 %d 次，已满足 3 次覆盖的样本数：%d / %d，当前剩余 need 总和：%d%n",
                        iteration, satisfiedCount, need.size(), totalNeed
                );
            }
        }

        // summary
        int totalDirs = 0;
        for (Set<String> dirIds : result.values()) {
            totalDirs += dirIds.size();
        }
        System.err.println("DirectionSelectorV3: 最终选择了 " + totalDirs + " 个方向");
        int totalViewpoints = result.size();
        System.err.println("DirectionSelectorV3: 最终选择了 " + totalViewpoints + " 个视角");
        int remainSamples = 0;
        for (int val : need.values()) {
            if (val > 0) {
                remainSamples++;
            }
        }
        if (remainSamples > 0) {
            System.err.println("⚠️ DirectionSelectorV3: 有 " + remainSamples + " 个样本无法覆盖 3 次");
        }
        return result;
    }
}
