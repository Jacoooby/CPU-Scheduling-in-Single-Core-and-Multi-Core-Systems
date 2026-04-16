import java.util.List;

public class RRScheduler implements SchedulerStrategy {
    private final int quantum;

    public RRScheduler(int quantum) {
        this.quantum = quantum;
    }

    @Override
    public ScheduleDecision chooseNextTask(List<SimTask> readyTasks, SimTask currentTask, int currentTime) {
        if (readyTasks.isEmpty()) {
            return new ScheduleDecision(null, 0);
        }

        SimTask selected = readyTasks.get(0);
        int burstTime = Math.min(quantum, selected.getRemainingBurst());
        return new ScheduleDecision(selected, burstTime);
    }

    @Override
    public String getDispatcherMessage() {
        return "Running RR algorithm, Time Quantum = " + quantum;
    }

    @Override
    public SchedulerType getType() {
        return SchedulerType.RR;
    }

    @Override
    public boolean isPreemptive() {
        return true;
    }
}