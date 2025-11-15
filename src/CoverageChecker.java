import java.util.*;

class CoverageChecker {
    static void checkSelectedDirectionsValid(Map<Viewpoint, Set<String>> selected) {
        System.out.println("===== SolutionChecker: Step 1 检查方向合法性 =====");
        int totalVp = selected.size();
        int totalDirs = 0;
        int invalidInPrecision = 0;
        for (Map.Entry<Viewpoint, Set<String>> entry : selected.entrySet()) {
            Viewpoint vp = entry.getKey();
            Set<String> dirIds = entry.getValue();
            if (vp == null || dirIds == null) {
                continue;
            }
            totalDirs += dirIds.size();
            Map<String, Double> precisionMap = vp.precision;
            for (String dirId : dirIds) {
                if (precisionMap == null || !precisionMap.containsKey(dirId)) {
                    invalidInPrecision++;
                    System.out.println("⚠️ 选中的方向在 precision 中找不到: "
                            + "vpId=" + vp.id + ", dirId=" + dirId);
                }

            }
        }
        System.out.println("视点数量             : " + totalVp);
        System.out.println("总共选中的方向数量   : " + totalDirs);
        System.out.println("precision 中缺失的方向数: " + invalidInPrecision);
        System.out.println("===== SolutionChecker: Step 1 结束 =====");
    }

    static void checkSampleCoverage(
            Map<Viewpoint, Set<String>> selected,
            List<SamplePoint> samplePoints
    ) {
        System.out.println("===== SolutionChecker: Step 2 检查样本覆盖情况 =====");
        // 转换selected为 vpId -> Set<dirId>
        Map<String, Set<String>> selectedIdMap = new HashMap<>();
        for (Map.Entry<Viewpoint, Set<String>> entry : selected.entrySet()) {
            Viewpoint vp = entry.getKey();
            Set<String> dirIds = entry.getValue();
            if (dirIds == null || vp == null ) {
                continue;
            }
            selectedIdMap.put(vp.id, dirIds);
        }
        int totalSamples = samplePoints.size();
        int coveredAtLeast3 = 0;
        int lessThan3 = 0;
        int zeroCovered = 0;
        int maxCover = 0;
        int sumCover = 0;

        List<String> badSamples = new ArrayList<>();
        for (SamplePoint sp : samplePoints) {
            int coverCount  = 0;
            if (sp.coveringPairs != null) {
                for (CoveringPair cp : sp.coveringPairs){
                    String vpId = cp.viewpointId;
                    String dirId = cp.directionId;

                    Set<String> dirIds = selectedIdMap.get(vpId);
                    if (dirIds != null && dirIds.contains(dirId)) {
                        coverCount++;
                    }
                }
            }
            sumCover += coverCount;
            if(coverCount > maxCover) {
                maxCover = coverCount;
            }
            if (coverCount >= 3) {
                coveredAtLeast3++;
            } else {
                lessThan3++;
                if( coverCount == 0) {
                    zeroCovered++;
                    if (sp.coveringPairs != null) {
                        badSamples.add(sp.id + " (cover=" + coverCount
                                + ", possible=" + sp.coveringPairs.size() + ")");
                    }
                }
            }
        }
        double avgCover = totalSamples==0 ? 0 : (double)sumCover / totalSamples;
        System.out.println("样本总数               : " + totalSamples);
        System.out.println(">=3 次覆盖的样本数      : " + coveredAtLeast3);
        System.out.println("<3 次覆盖的样本数       : " + lessThan3);
        System.out.println("  其中 0 次覆盖         : " + zeroCovered);
        System.out.println("最大覆盖次数            : " + maxCover);
        System.out.println("平均覆盖次数            : " + String.format("%.3f", avgCover));

        if (!badSamples.isEmpty()) {
            System.out.println("覆盖不足 3 次 的样本(格式：id(cover, possible))：");
            for (String s : badSamples) {
                System.out.println("  " + s);
            }
        }

        System.out.println("===== SolutionChecker: Step 2 结束 =====");
    }
}
