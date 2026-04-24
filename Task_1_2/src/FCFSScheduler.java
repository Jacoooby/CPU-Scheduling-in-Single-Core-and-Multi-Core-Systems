import java.util.List;

public class FCFSScheduler implements SchedulerStrategy {
    @Override
    public ScheduleDecision chooseNextTask(List<SimTask> readyTasks, SimTask currentTask, int currentTime) {
        if (currentTask != null && !currentTask.isFinished()) {
            return new ScheduleDecision(currentTask, currentTask.getRemainingBurst());
        }

        if (readyTasks.isEmpty()) {
            return new ScheduleDecision(null, 0);
        }

        SimTask selected = readyTasks.get(0);
        return new ScheduleDecision(selected, selected.getRemainingBurst());
    }

    @Override
    public String getDispatcherMessage() {
        return "Running FCFS algorithm";
    }

    @Override
    public SchedulerType getType() {
        return SchedulerType.FCFS;
    }

    @Override
    public boolean isPreemptive() {
        return false;
    }
}