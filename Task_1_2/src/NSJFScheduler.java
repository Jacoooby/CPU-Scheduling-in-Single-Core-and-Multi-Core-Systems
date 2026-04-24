import java.util.Comparator;
import java.util.List;

public class NSJFScheduler implements SchedulerStrategy {
    @Override
    public ScheduleDecision chooseNextTask(List<SimTask> readyTasks, SimTask currentTask, int currentTime) {
        // if task is already running just let it finish
        if (currentTask != null && !currentTask.isFinished()) {
            return new ScheduleDecision(currentTask, currentTask.getRemainingBurst());
        }

        if (readyTasks.isEmpty()) {
            return new ScheduleDecision(null, 0);
        }

        // pick the task with the shortest remaining burst, if there is a tie pick the one that arrived first,
        // if there is still a tie pick the one with the smaller id
        SimTask selected = null;
        for (SimTask task : readyTasks) {
            if (selected == null) {
                selected = task;
                continue;
            }
            if (task.getRemainingBurst() < selected.getRemainingBurst()) {
                selected = task;
            } else if (task.getRemainingBurst() == selected.getRemainingBurst()) {
                if (task.getArrivalTime() < selected.getArrivalTime()) {
                    selected = task;
                } else if (task.getArrivalTime() == selected.getArrivalTime()) {
                    if (task.getId() < selected.getId()) {
                        selected = task;
                    }
                }
            }
        }

        if (selected == null) {
            return new ScheduleDecision(null, 0);
        }

        return new ScheduleDecision(selected, selected.getRemainingBurst());
    }

    @Override
    public String getDispatcherMessage() {
        return "Running Non Preemptive - Shortest Job First";
    }

    @Override
    public SchedulerType getType() {
        return SchedulerType.NSJF;
    }

    @Override
    public boolean isPreemptive() {
        return false;
    }
}