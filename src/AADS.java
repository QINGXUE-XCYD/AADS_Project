import java.io.FileWriter;
import java.util.*;

public class AADS {

    // 在selected创建vpId -> vp的映射
    static Map<String, Viewpoint> buildVpId2VpMap(Map<Viewpoint, Set<String>> selected) {
        Map<String, Viewpoint> VpId2Vp = new HashMap<>();
        for (Viewpoint vp : selected.keySet()) {
            VpId2Vp.put(vp.id, vp);
        }
        return VpId2Vp;
    }


    public static void main(String[] args) throws Exception {
        TimerUtil timer = new TimerUtil();
        timer.start();

        InputData data = JsonParser.parseInput();
        // 数据检查
        if (false) {
            System.out.println("✅ Parse Success!");
            System.out.println("Viewpoints: " + data.viewpoints.size());
            System.out.println(data.viewpoints.get(0).toString());
            for (Viewpoint vp : data.viewpoints) {
                if (vp.isMandatory) {
                    System.out.println("Mandatory viewpoint: " + vp);
                }
            }
            System.out.println("Samples: " + data.samplePoints.size());
            System.out.println(data.samplePoints.get(0).toString());
            System.out.println("Directions: " + data.directions.size());
            System.out.println(data.directions.get(0).toString());
            System.out.println("Collision matrix: " + data.collisionMatrix.length);
            System.out.println("Is symmetric: " + GraphUtil.isSymmetric(data.collisionMatrix));
            timer.printElapsed("数据解析");
            double[][] distanceMatrix = GraphUtil.buildDistanceMatrix(data.viewpoints, data.collisionMatrix);
            timer.printElapsed("距离矩阵计算");
            if (!GraphUtil.isFullyReachable(data.viewpoints, data.viewpoints, data.collisionMatrix)) {
                System.err.println("⚠️ 整个图不是连通图（有区域永远到不了）。");
                List<Viewpoint> unreachable = GraphUtil.getUnreachable(data.viewpoints, data.viewpoints, data.collisionMatrix);
                unreachable.forEach(vp -> System.err.println("  不可达: " + vp.id));
            }
        }

        // 初始全覆盖
        Map<Viewpoint, Set<String>> selected = selectALlDirections(data.viewpoints);
        // 检查覆盖
        if (true) {
            System.out.println("初始全覆盖: " + selected.size());
            System.out.println("缺少覆盖: " + countCoverageLessThan3(data.samplePoints, selected));
            System.out.println("总精度: " + computeTotalPrecision(data.samplePoints, selected));
            timer.printElapsed("初始全覆盖");
        }

    }

    // 初始全覆盖
    static Map<Viewpoint, Set<String>> selectALlDirections(List<Viewpoint> viewpoints) {
        Map<Viewpoint, Set<String>> selected = new HashMap<>();
        for (Viewpoint vp : viewpoints) {
            if (vp.precision != null) {
                Set<String> dirs = new HashSet<>(vp.precision.keySet());
                selected.put(vp, dirs);
            }
        }
        return selected;
    }

    // 检查单点覆盖
    static int countCoverageForSamplePoint(SamplePoint sp, Map<Viewpoint, Set<String>> selected) {
        Map<String, Viewpoint> vpId2Vp = buildVpId2VpMap(selected);
        int count = 0;
        for (CoveringPair cp : sp.coveringPairs) {
            Viewpoint vp = vpId2Vp.get(cp.viewpointId);
            if (vp == null) {
                continue;
            }
            Set<String> dirs = selected.get(vp);
            if (dirs != null && dirs.contains(cp.directionId)) {
                count++;
            }
        }
        return count;
    }

    // 检查缺少覆盖
    static int countCoverageLessThan3(List<SamplePoint> samples, Map<Viewpoint, Set<String>> selected) {
        int count = 0;
        for (SamplePoint sp : samples) {
            if (countCoverageForSamplePoint(sp, selected) < 3) {
                count++;
            }
        }
        return count;
    }

    // 计算精度
    static double computeTotalPrecision(
            List<SamplePoint> samples,
            Map<Viewpoint, Set<String>> selected
    ) {
        Map<String, Viewpoint> vpId2Vp = buildVpId2VpMap(selected);
        double totalPrecision = 0;
        try (FileWriter fw = new FileWriter("precision.csv")) {
            fw.write("sample_point_id,precision_values\n");

            for (SamplePoint sp : samples) {
                List<Double> usable = new ArrayList<>();
                for (CoveringPair cp : sp.coveringPairs) {
                    Viewpoint vp = vpId2Vp.get(cp.viewpointId);
                    if (vp == null) {
                        continue;
                    }
                    Set<String> dirs = selected.get(vp);
                    if (dirs == null || !dirs.contains(cp.directionId)) {
                        continue;
                    }
                    Double precision = vp.precision.get(cp.directionId);
                    if (precision != null) {
                        usable.add(precision);
                    }
                }
                if (usable.isEmpty()) {
                    continue;
                }
                usable.sort((a, b) -> Double.compare(b, a));
                // 取前3个
                // === 写入 CSV ===
                StringBuilder sb = new StringBuilder();
                sb.append(sp.id);
                int n = Math.min(3, usable.size());
                for (int i = 0; i < n; i++) {
                    totalPrecision += usable.get(i);
                    sb.append(",").append(usable.get(i));
                }
                // 后续还有正值
                if (usable.size() > 3) {
                    for (int i = n; i < usable.size(); i++) {
                        if (usable.get(i) > 0) {
                            totalPrecision += usable.get(i);
                            sb.append(",").append(usable.get(i));
                        }
                    }
                }
                sb.append("\n");
                fw.write(sb.toString());
            }
        } catch (Exception ignored) {
        }

        return totalPrecision;
    }

}

