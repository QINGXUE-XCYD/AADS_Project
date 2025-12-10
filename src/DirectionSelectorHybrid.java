import java.util.*;

class DirectionSelectorHybrid {

    static Map<Viewpoint, Set<String>> selectDirections(
            List<Viewpoint> viewpoints,
            List<SamplePoint> samples
    ) {

        /* =======================================================
         * Stage 0：Precompute coverMap
         * (vp,dir) -> set(sampleId)
         * ======================================================= */
        Map<String, Map<String, Set<String>>> coverMap = new HashMap<>();
        for (SamplePoint sp : samples) {
            if (sp.coveringPairs == null) continue;
            for (CoveringPair cp : sp.coveringPairs) {
                coverMap
                        .computeIfAbsent(cp.viewpointId, k -> new HashMap<>())
                        .computeIfAbsent(cp.directionId, k -> new HashSet<>())
                        .add(sp.id);
            }
        }

        // vpId -> Viewpoint
        Map<String, Viewpoint> vpMap = new HashMap<>();
        for (Viewpoint vp : viewpoints) vpMap.put(vp.id, vp);


        /* =======================================================
         * Stage 1：Sample-based Top-3（你同学的方法）
         * 每个 sample 选三条最高精度方向
         * ======================================================= */
        Map<String, Set<String>> initialSelected = new HashMap<>();

        for (SamplePoint sp : samples) {

            class CC { String vpId; String angId; double p; }

            List<CC> list = new ArrayList<>();

            for (CoveringPair cp : sp.coveringPairs) {
                Viewpoint vp = vpMap.get(cp.viewpointId);
                if (vp == null) continue;

                Double pr = vp.precision.get(cp.directionId);
                if (pr == null) continue;

                CC cc = new CC();
                cc.vpId = cp.viewpointId;
                cc.angId = cp.directionId;
                cc.p = pr;
                list.add(cc);
            }

            // 按精度从大到小排序
            list.sort((a, b) -> Double.compare(b.p, a.p));

            int K = Math.min(3, list.size());
            for (int i = 0; i < K; i++) {
                CC cc = list.get(i);
                initialSelected
                        .computeIfAbsent(cc.vpId, k -> new HashSet<>())
                        .add(cc.angId);
            }
        }


        /* =======================================================
         * Stage 2：统计这些方向的全局贡献 score = precision * coverCount
         * ======================================================= */
        class DirItem {
            String vpId;
            String dirId;
            double precision;
            int coverCount;
            double score;
        }

        List<DirItem> items = new ArrayList<>();

        for (Map.Entry<String, Set<String>> e : initialSelected.entrySet()) {
            String vpId = e.getKey();
            Viewpoint vp = vpMap.get(vpId);

            for (String dirId : e.getValue()) {
                Double pr = vp.precision.get(dirId);
                if (pr == null) continue;

                Map<String, Set<String>> mp = coverMap.get(vpId);
                int cc = 0;
                if (mp != null && mp.get(dirId) != null) {
                    cc = mp.get(dirId).size();
                }

                DirItem it = new DirItem();
                it.vpId = vpId;
                it.dirId = dirId;
                it.precision = pr;
                it.coverCount = cc;
                it.score = pr * cc;

                items.add(it);
            }
        }

        // 按贡献排序
        items.sort((a, b) -> Double.compare(b.score, a.score));


        /* =======================================================
         * Stage 3：重新加入方向，保证 need=3
         * 覆盖不足时优先加入贡献高的
         * 剪掉大量冗余方向
         * ======================================================= */
        Map<String, Integer> need = new HashMap<>();
        for (SamplePoint sp : samples) need.put(sp.id, 3);

        Map<Viewpoint, Set<String>> result = new HashMap<>();

        for (DirItem it : items) {

            // 添加方向
            Viewpoint vp = vpMap.get(it.vpId);
            result.computeIfAbsent(vp, k -> new HashSet<>()).add(it.dirId);

            // 更新 need（如果还没满足）
            Map<String, Set<String>> mp = coverMap.get(it.vpId);
            if (mp != null && mp.get(it.dirId) != null) {
                for (String sid : mp.get(it.dirId)) {
                    int v = need.get(sid);
                    if (v > 0) need.put(sid, v - 1);
                }
            }

            // 检查是否覆盖满足
            boolean allZero = true;
            for (int v : need.values()) {
                if (v > 0) { allZero = false; break; }
            }

            // 如果已经全部满足覆盖需求，则可直接结束！
            if (allZero) break;
        }


        /* =======================================================
         * Stage 4：可选 —— 再加入剩余正精度方向补精度（不破坏路径规划）
         * ======================================================= */
        for (DirItem it : items) {
            if (it.precision <= 0) continue; // 不加负精度
            Viewpoint vp = vpMap.get(it.vpId);
            result.computeIfAbsent(vp, k -> new HashSet<>()).add(it.dirId);
        }


        /* =======================================================
         * 统计与输出
         * ======================================================= */
        int totalDirs = 0;
        for (Set<String> s : result.values()) totalDirs += s.size();

        int remain = 0;
        for (int v : need.values()) if (v > 0) remain++;

        System.err.println("Hybrid Selector: 最终方向数量 = " + totalDirs);
        System.err.println("Hybrid Selector: 涉及视点数 = " + result.size());
        if (remain > 0) {
            System.err.println("⚠️ 覆盖不足 sample = " + remain);
        }

        return result;
    }
}
