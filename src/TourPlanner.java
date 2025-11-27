import java.util.*;

class TourPlanner {
    static List<Viewpoint> buildTour(
            List<Viewpoint> viewPoints,
            Map<Viewpoint, Set<String>> selected,
            double[][] distanceMatrix
    ) {
        int n = viewPoints.size();
        Set<Viewpoint> mustVisit = new LinkedHashSet<>();
        // find mandatory viewPoint
        Viewpoint startVp = null;
        for (Viewpoint vp : viewPoints) {
            if (vp.isMandatory) {
                mustVisit.add(vp);
                if (startVp == null) {
                    startVp = vp;
                }
            }
        }
        mustVisit.addAll(selected.keySet());
        if (startVp == null) {
            if (!mustVisit.isEmpty()) {
                startVp = viewPoints.iterator().next();
            } else {
                System.err.println("No Start Point");
                return new ArrayList<>();
            }
        }
        // build vp -> Index map
        Map<Viewpoint, Integer> vpIndexMap = new HashMap<>();
        for (int i = 0; i < n; i++) {
            vpIndexMap.put(viewPoints.get(i), i);
        }


        // used Index List
        List<Integer> usedIndex = new ArrayList<>();
        for (Viewpoint vp : mustVisit) {
            Integer index = vpIndexMap.get(vp);
            if (index != null) {
                usedIndex.add(index);
            }
        }

        int m = usedIndex.size();
        if (m == 0) {
            System.err.println("No Used Viewpoints");
            return new ArrayList<>();
        }
        // find startVp index
        int startIndex = vpIndexMap.get(startVp);
        int startLocal = -1;
        for (int i = 0; i < m; i++) {
            if (usedIndex.get(i) == startIndex) {
                startLocal = i;
                break;
            }
        }
        if (startLocal == -1) {
            usedIndex.add(0, startIndex);
            startLocal = 0;
            m++;
        }

        // build tour
        boolean[] visited = new boolean[m];
        List<Integer> routeLocal = new ArrayList<>();
        int current = startLocal;
        visited[current] = true;
        routeLocal.add(current);
        for (int step = 0; step < m; step++) {
            int bestNext = -1;
            double bestDistance = Double.POSITIVE_INFINITY;
            int currentGlobalIndex = usedIndex.get(current);

            for (int i = 0; i < m; i++) {
                if (visited[i]) {
                    continue;
                }
                int nextGlobalIndex = usedIndex.get(i);
                double distance = distanceMatrix[currentGlobalIndex][nextGlobalIndex];
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestNext = i;
                }
            }
            if (bestNext == -1) {
                System.err.println("Cannot Find Next Viewpoint");

                // 强行解法，，记得改
                //
                //
                //
                // !!!!!!!
                for (int i = 0; i < m; i++) {
                    if (!visited[i]) {
                        visited[i] = true;
                        routeLocal.add(i);
                    }
                }
                break;
            }
            visited[bestNext] = true;
            routeLocal.add(bestNext);
            current = bestNext;
        }
        // build tour with vps
        List<Viewpoint> tour = new ArrayList<>();
        for (int localIndex : routeLocal) {
            int globalIndex = usedIndex.get(localIndex);
            tour.add(viewPoints.get(globalIndex));
        }
        double totalDistance = 0;
        for (int i = 0; i < tour.size() - 1; i++) {
            totalDistance += distanceMatrix[vpIndexMap.get(tour.get(i))][vpIndexMap.get(tour.get(i + 1))];
        }
        System.out.printf("TourPlanner: 路径包含 %d 个独立视点，总路程约为 %.3f%n",
                m, totalDistance);
        return tour;
    }
}
