import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PSJFScheduler implements SchedulerStrategy {
    @Override
    public ScheduleDecision chooseNextTask(List<SimTask> readyTasks, SimTask currentTask, int currentTime) {
        List<SimTask> candidates = new ArrayList<>(readyTasks);

        if (currentTask != null && !currentTask.isFinished()) {
            candidates.add(currentTask);
        }

        if (candidates.isEmpty()) {
            return new ScheduleDecision(null, 0);
        }

        SimTask selected = candidates.stream()
                .min(Comparator.comparingInt(SimTask::getRemainingBurst)
                        .thenComparingInt(SimTask::getArrivalTime)
                        .thenComparingInt(SimTask::getId))
                .orElse(null);

        if (selected == null) {
            return new ScheduleDecision(null, 0);
        }

        return new ScheduleDecision(selected, 1);
    }

    @Override
    public String getDispatcherMessage() {
        return "Running Preemptive - Shortest Job First";
    }

    @Override
    public SchedulerType getType() {
        return SchedulerType.PSJF;
    }

    @Override
    public boolean isPreemptive() {
        return true;
    }
}