import java.util.List;

public interface SchedulerStrategy {
    ScheduleDecision chooseNextTask(List<SimTask> readyTasks, SimTask currentTask, int currentTime);

    String getDispatcherMessage();

    SchedulerType getType();

    boolean isPreemptive();
}