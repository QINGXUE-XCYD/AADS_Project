import java.util.*;

class DirectionSelectorV4 {

    static Map<Viewpoint, Set<String>> selectDirections(
            List<Viewpoint> viewpoints,
            List<SamplePoint> samples
    ) {
        // 构建 sample 覆盖表: (vp,dir) -> Set<sampleId>
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

        // flatten viewpointId -> Viewpoint
        Map<String, Viewpoint> vpMap = new HashMap<>();
        for (Viewpoint vp : viewpoints) {
            vpMap.put(vp.id, vp);
        }

        // Step 1: 构建所有方向的贡献项
        class DirItem {
            String vpId;
            String dirId;
            double precision;
            int coverCount;
            double score;
        }
        List<DirItem> items = new ArrayList<>();

        for (Viewpoint vp : viewpoints) {
            if (vp.precision == null) continue;
            Map<String, Double> pmap = vp.precision;
            Map<String, Set<String>> vpCover = coverMap.get(vp.id);

            for (Map.Entry<String, Double> e : pmap.entrySet()) {
                String dirId = e.getKey();
                double prec = e.getValue();


                Set<String> cset = (vpCover == null ? null : vpCover.get(dirId));
                int cc = (cset == null ? 0 : cset.size());
                if (cc == 0) continue;

                DirItem it = new DirItem();
                it.vpId = vp.id;
                it.dirId = dirId;
                it.precision = prec;
                it.coverCount = cc;
                it.score = prec * cc;

                items.add(it);
            }
        }

        // Step 2: 按贡献值排序
        items.sort((a, b) -> Double.compare(b.score, a.score));

        // Step 3: need=3 覆盖检查
        Map<String, Integer> need = new HashMap<>();
        for (SamplePoint sp : samples) {
            need.put(sp.id, 3);
        }

        Map<Viewpoint, Set<String>> result = new HashMap<>();

        // Step 3 + 4: 先覆盖 need，再补齐所有正贡献项
        for (DirItem it : items) {

            // 所有覆盖需求已满足，但方向仍有正精度 → 继续选
            boolean needAllZero = true;
            for (int v : need.values()) {
                if (v > 0) {
                    needAllZero = false;
                    break;
                }
            }

            // 添加方向
            Viewpoint vpo = vpMap.get(it.vpId);
            result.computeIfAbsent(vpo, k -> new HashSet<>()).add(it.dirId);

            // 如果仍有覆盖需求，更新 need
            if (!needAllZero) {
                Map<String, Set<String>> mp = coverMap.get(it.vpId);
                if (mp != null) {
                    Set<String> covered = mp.get(it.dirId);
                    if (covered != null) {
                        for (String sid : covered) {
                            int nv = need.get(sid);
                            if (nv > 0) need.put(sid, nv - 1);
                        }
                    }
                }
            }
        }

        // Step 5: 打印总结
        int totalDirs = 0;
        for (Set<String> s : result.values()) totalDirs += s.size();
        System.err.println("DirectionSelectorV4: 选中了方向数 = " + totalDirs);
        System.err.println("DirectionSelectorV4: 涉及视点数 = " + result.size());

        int remain = 0;
        for (int v : need.values()) if (v > 0) remain++;
        if (remain > 0) {
            System.err.println("⚠️ Warning: 有 " + remain + " 个 sample 未满足 3 覆盖");
        }

        return result;
    }
}
