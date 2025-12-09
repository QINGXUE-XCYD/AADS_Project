import java.util.Map;
import java.util.Set;

public class DeleteResult {
    public boolean feasible;   // 是否可行
    public double gain;        // 精度变化 - 路径变化

    public Map<Viewpoint, Set<String>> newSelected;
    public Set<Viewpoint> newMustVisit;
    public Set<Viewpoint> newAllowedTransit;
    public String actionInfo;   // 保存删除动作：如 "Delete dir v23-a3" 或 "Delete VP v23"

    public DeleteResult(boolean feasible) {
        this.feasible = feasible;
    }

    public static DeleteResult infeasible() {
        return new DeleteResult(false);
    }

    public static DeleteResult feasible(
            double gain,
            Map<Viewpoint, Set<String>> selected,
            Set<Viewpoint> mustVisit,
            Set<Viewpoint> allowedTransit,
            String actionInfo
    ) {
        DeleteResult r = new DeleteResult(true);
        r.gain = gain;
        r.newSelected = selected;
        r.newMustVisit = mustVisit;
        r.newAllowedTransit = allowedTransit;
        r.actionInfo = actionInfo;
        return r;
    }
}
