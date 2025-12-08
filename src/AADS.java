import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AADS {
    public static void main(String[] args) throws Exception {
        TimerUtil timer = new TimerUtil();
        timer.start();

        InputData data = JsonParser.parseInput();

        System.out.println("✅ Parse Success!");
        System.out.println("Viewpoints: " + data.viewpoints.size());
        System.out.println(data.viewpoints.get(0).toString());
        for (Viewpoint vp : data.viewpoints) {
            if (vp.isMandatory){
                System.out.println("Mandatory viewpoint: " + vp.toString());
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

}

