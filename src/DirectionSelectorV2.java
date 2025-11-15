import java.util.*;

/**
 * DirectionSelectorV2
 * 多阶段贪心 + 全局排序 + 稀疏优先补充
 * 满足：所有 sample 至少被 3 个 viewpoint-angle 覆盖
 *
 * 输出：
 *   selectedAngles: Map<viewpointId, Set<directionId>>
 *
 */
class DirectionSelectorV2 {

    /**
     * 主入口
     */
    public static Map<String, Set<String>> selectDirections(
            List<Viewpoint> viewpoints,
            List<SamplePoint> samples
    ) {
        long start = System.currentTimeMillis();

        // 建立 viewpoint 映射
        Map<String, Viewpoint> vpMap = new HashMap<>();
        for (Viewpoint v : viewpoints) vpMap.put(v.id, v);

        // ======================
        // Step 1: 构建 cover[v][a] 映射
        // ======================
        Map<String, Map<String, List<String>>> cover = new HashMap<>();

        for (SamplePoint sp : samples) {
            for (CoveringPair cp : sp.coveringPairs) {
                cover
                    .computeIfAbsent(cp.viewpointId, k -> new HashMap<>())
                    .computeIfAbsent(cp.directionId, k -> new ArrayList<>())
                    .add(sp.id);
            }
        }

        // ======================
        // Step 2: 计算分数 score(v,a)，按全局排序
        // ======================
        class VPDir {
            String vp;
            String angle;
            double score;
            List<String> covers;

            VPDir(String vp, String angle, double score, List<String> covers) {
                this.vp = vp;
                this.angle = angle;
                this.score = score;
                this.covers = covers;
            }
        }

        List<VPDir> all = new ArrayList<>();

        for (var ve : cover.entrySet()) {
            String vpId = ve.getKey();
            Map<String, List<String>> angles = ve.getValue();

            for (var ae : angles.entrySet()) {
                String angle = ae.getKey();
                List<String> coveredSamples = ae.getValue();

                double score = 0.0;
                Double p = null;

                // 该方向在该 viewpoint 的精度
                Viewpoint vp = vpMap.get(vpId);
                if (vp != null && vp.precision != null) {
                    p = vp.precision.get(angle);
                }

                if (p != null) score += p * coveredSamples.size();
                else score += coveredSamples.size();  // 无精度则退化为覆盖数量

                all.add(new VPDir(vpId, angle, score, coveredSamples));
            }
        }

        // 全局按分数降序排序
        all.sort((a, b) -> Double.compare(b.score, a.score));


        // ======================
        // Step 3: 需求数组 need[sample] = 3
        // ======================
        Map<String, Integer> need = new HashMap<>();
        for (SamplePoint sp : samples) need.put(sp.id, 3);

        Map<String, Set<String>> selected = new HashMap<>();
        for (Viewpoint v : viewpoints) selected.put(v.id, new HashSet<>());


        // ======================
        // Step 4: 第一阶段 —— 全局贪心选方向
        // ======================
        for (VPDir cur : all) {
            int gain = 0;
            for (String sid : cur.covers) {
                if (need.get(sid) > 0) gain++;
            }
            if (gain == 0) continue;

            selected.get(cur.vp).add(cur.angle);

            for (String sid : cur.covers) {
                need.put(sid, Math.max(need.get(sid) - 1, 0));
            }

            if (isSatisfied(need)) break;
        }

        // ======================
        // Step 5: 第二阶段 —— 稀疏样本优先补充
        // ======================
        if (!isSatisfied(need)) {

            List<String> remaining = new ArrayList<>();
            for (var e : need.entrySet()) if (e.getValue() > 0) remaining.add(e.getKey());

            // 对稀疏样本按 need 倒序排序（需覆盖多的优先）
            remaining.sort((a, b) -> Integer.compare(need.get(b), need.get(a)));

            for (String sid : remaining) {
                int req = need.get(sid);
                if (req <= 0) continue;

                // 所有能覆盖 sid 的 (vp,a) 列表
                List<VPDir> candidates = new ArrayList<>();

                for (VPDir va : all) {
                    if (va.covers.contains(sid)) {
                        candidates.add(va);
                    }
                }

                // 按 score 再排序
                candidates.sort((x, y) -> Double.compare(y.score, x.score));

                // 贪心补充 req 次
                for (VPDir va : candidates) {
                    if (need.get(sid) <= 0) break;
                    if (!selected.get(va.vp).contains(va.angle)) {
                        selected.get(va.vp).add(va.angle);

                        for (String sid2 : va.covers) {
                            need.put(sid2, Math.max(need.get(sid2) - 1, 0));
                        }
                    }
                }
            }
        }


        long end = System.currentTimeMillis();
        System.err.println("DirectionSelectorV2 finished in " + (end - start) + "ms");

        int totalAngles = selected.values().stream().mapToInt(Set::size).sum();
        System.err.println("Selected total angles = " + totalAngles);

        // 检查是否所有 sample 满足 ≥3 覆盖
        int satisfied = 0;
        for (String sid : need.keySet()) {
            if (need.get(sid) <= 0) satisfied++;
        }
        System.err.println("Samples satisfied (≥3): " + satisfied + " / " + samples.size());

        System.out.println(selected);
        return selected;
    }


    // 判断所有 sample 是否满足 need=0
    private static boolean isSatisfied(Map<String, Integer> need) {
        for (int v : need.values()) {
            if (v > 0) return false;
        }
        return true;
    }
}
