import java.util.*;

class DirectionSelector {


    // Direction selection based on global contribution
    // Maximizing contribution to both precision and coverage until all sample points are covered at least 3 times

    // Immutable key representing a (viewpoint, direction) pair
    record VpDirKey(String viewpointId, String directionId) {
    }

    // Store each (viewpoint, direction) pair's contribution
    record DirectionContribution(
            VpDirKey vpDirKey,
            double precision,
            int coveredSamplePoints,
            double score
    ) {
    }

    static Map<Viewpoint, Set<String>> selectDirections(
            List<Viewpoint> viewpoints,
            List<SamplePoint> samples
    ) {

        // Build cover map (vpId, dirId) -> covered sampleIds
        Map<String, Map<String, Set<String>>> coverageMap = new HashMap<>();
        for (SamplePoint sp : samples) {
            if (sp.coveringPairs == null) {
                continue;
            }
            for (CoveringPair cp : sp.coveringPairs) {
                coverageMap
                        .computeIfAbsent(cp.viewpointId, k -> new HashMap<>())
                        .computeIfAbsent(cp.directionId, k -> new HashSet<>())
                        .add(sp.id);
            }
        }

        // Build viewpoint map vpId -> Viewpoint
        Map<String, Viewpoint> viewpointById = new HashMap<>();
        for (Viewpoint vp : viewpoints) {
            viewpointById.put(vp.id, vp);
        }

        // Find high precision directions for each sample as candidate
        Map<String, Set<String>> candidateDirections = new HashMap<>();

        for (SamplePoint sp : samples) {
            // Store precisions of each direction
            Map<VpDirKey, Double> localScores = new HashMap<>();
            for (CoveringPair cp : sp.coveringPairs) {
                Viewpoint vp = viewpointById.get(cp.viewpointId);
                if (vp == null) {
                    continue;
                }
                Double precision = vp.precision.get(cp.directionId);
                if (precision == null) {
                    continue;
                }
                localScores.put(new VpDirKey(cp.viewpointId, cp.directionId), precision);
            }
            // Sort by precision
            localScores.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .limit(3)
                    .forEach(e -> {
                        VpDirKey k = e.getKey();
                        candidateDirections
                                .computeIfAbsent(k.viewpointId(), x -> new HashSet<>())
                                .add(k.directionId());
                    });
        }

        // Global contribution evaluation: score = precision * coverCount
        // Storing global contribution information
        List<DirectionContribution> contributions = new ArrayList<>();

        // Traverse all
        for (Map.Entry<String, Set<String>> e : candidateDirections.entrySet()) {
            // Get corresponding viewpoint
            String vpId = e.getKey();
            Viewpoint vp = viewpointById.get(vpId);
            if (vp == null) {
                continue;
            }
            // Traverse all directions of this viewpoint
            for (String dirId : e.getValue()) {
                Double precision = vp.precision.get(dirId);
                if (precision == null) {
                    continue;
                }

                // Calculate cover count
                int coverCount = 0;
                Map<String, Set<String>> map = coverageMap.get(vpId);
                if (map != null && map.get(dirId) != null) {
                    coverCount = map.get(dirId).size();
                }
                // Calculate score
                contributions.add(new DirectionContribution(
                        new VpDirKey(vpId, dirId),
                        precision,
                        coverCount,
                        precision * coverCount
                ));
            }
        }

        // Sort by contribution score
        contributions.sort((a, b) -> Double.compare(b.score(), a.score()));

        // Greedy selection under coverage constraints
        // Track remaining coverage demand for each sample
        Map<String, Integer> remainNeed = new HashMap<>();
        for (SamplePoint sp : samples) {
            remainNeed.put(sp.id, 3);
        }
        // Final selected directions grouped by viewpoint
        Map<Viewpoint, Set<String>> result = new HashMap<>();

        // Select directions greedily
        boolean allSatisfied = true;
        for (int idx1 = 0; idx1 < contributions.size(); idx1++) {
            DirectionContribution dc = contributions.get(idx1);
            // Get corresponding viewpoint
            VpDirKey k = dc.vpDirKey();
            Viewpoint vp = viewpointById.get(k.viewpointId());

            // Add direction to result
            result
                    .computeIfAbsent(vp, x -> new HashSet<>())
                    .add(k.directionId());

            // Update remaining need
            Map<String, Set<String>> map = coverageMap.get(k.viewpointId());
            if (map != null && map.get(k.directionId()) != null) {
                for (String sampleId : map.get(k.directionId())) {
                    int v = remainNeed.get(sampleId);
                    if (v > 0) {
                        remainNeed.put(sampleId, v - 1);
                    }
                }
            }

            // Check if all need satisfied
            for (int v : remainNeed.values()) {
                if (v > 0) {
                    allSatisfied = false;
                    break;
                }
            }
            if (allSatisfied) {
                // Add the remaining positive precision direction to result
                for (int idx2 = idx1 + 1; idx2 < contributions.size(); idx2++) {
                    DirectionContribution next = contributions.get(idx2);
                    if(next.score()<=0) {
                        break;
                    }
                    result
                            .computeIfAbsent(viewpointById.get(next.vpDirKey().viewpointId()), x -> new HashSet<>())
                            .add(next.vpDirKey().directionId());
                }
                break;
            }
        }
        return result;
    }
}
