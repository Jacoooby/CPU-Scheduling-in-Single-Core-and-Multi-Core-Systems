import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// PSJFScheduler always picks the task with the least remaining burst time, running only 1 burst at a time
// this way a newly arrived shorter task can preempt the current one on the next cycle
public class PSJFScheduler implements SchedulerStrategy {
    @Override
    public ScheduleDecision chooseNextTask(List<SimTask> readyTasks, SimTask currentTask, int currentTime) {
        List<SimTask> candidates = new ArrayList<>(readyTasks);

        // include currently running tasks as a candidate so it can be compared against new arrivals
        if (currentTask != null && !currentTask.isFinished()) {
            candidates.add(currentTask);
        }

        if (candidates.isEmpty()) {
            return new ScheduleDecision(null, 0);
        }

        // pick the task with the shortest remaining burst, if there is a tie pick the one that arrived first,
        // if there is still a tie pick the one with the smaller id
        SimTask selected = null;
        for (SimTask task : candidates) {
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

        // burst time of 1 to force the dispatcher to re-evaluate after every cycle
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