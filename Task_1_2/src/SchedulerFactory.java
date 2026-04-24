public class SchedulerFactory {
    public static SchedulerStrategy create(SchedulerType schedulerType, Integer quantum) {
        return switch (schedulerType) {
            case FCFS -> new FCFSScheduler();
            case RR -> new RRScheduler(quantum);
            case NSJF -> new NSJFScheduler();
            case PSJF -> new PSJFScheduler();
        };
    }
}