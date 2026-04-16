import java.util.Comparator;
import java.util.List;

public class NSJFScheduler implements SchedulerStrategy {
    @Override
    public ScheduleDecision chooseNextTask(List<SimTask> readyTasks, SimTask currentTask, int currentTime) {
        if (currentTask != null && !currentTask.isFinished()) {
            return new ScheduleDecision(currentTask, currentTask.getRemainingBurst());
        }

        if (readyTasks.isEmpty()) {
            return new ScheduleDecision(null, 0);
        }

        SimTask selected = readyTasks.stream()
                .min(Comparator.comparingInt(SimTask::getRemainingBurst)
                        .thenComparingInt(SimTask::getArrivalTime)
                        .thenComparingInt(SimTask::getId))
                .orElse(null);

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