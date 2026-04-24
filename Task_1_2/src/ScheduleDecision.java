public class ScheduleDecision {
    private final SimTask selectedTask;
    private final int burstTime;

    public ScheduleDecision(SimTask selectedTask, int burstTime) {
        this.selectedTask = selectedTask;
        this.burstTime = burstTime;
    }

    public SimTask getSelectedTask() {
        return selectedTask;
    }

    public int getBurstTime() {
        return burstTime;
    }
}