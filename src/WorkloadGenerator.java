import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WorkloadGenerator {
    private static final Random RANDOM = new Random();

    public static List<SimTask> generateRandomTasks(SchedulerType schedulerType) {
        int taskCount = RANDOM.nextInt(25) + 1;
        List<SimTask> tasks = new ArrayList<>();
        boolean hasLateArrival = false;

        for (int i = 0; i < taskCount; i++) {
            int burst = RANDOM.nextInt(50) + 1;
            int arrivalTime = 0;

            if (schedulerType == SchedulerType.PSJF) {
                arrivalTime = RANDOM.nextInt(11); // 0..10
                if (arrivalTime > 0) {
                    hasLateArrival = true;
                }
            }

            tasks.add(new SimTask(i, burst, arrivalTime));
        }

        if (schedulerType == SchedulerType.PSJF && taskCount > 1 && !hasLateArrival) {
            SimTask replacement = new SimTask(
                    tasks.get(taskCount - 1).getId(),
                    tasks.get(taskCount - 1).getMaxBurst(),
                    1
            );
            tasks.set(taskCount - 1, replacement);
        }

        return tasks;
    }

    public static List<SimTask> generateFixedTasks(int[] bursts, SchedulerType schedulerType) {
        List<SimTask> tasks = new ArrayList<>();

        for (int i = 0; i < bursts.length; i++) {
            int arrivalTime = 0;
            if (schedulerType == SchedulerType.PSJF && i > 0) {
                arrivalTime = i; // simple predictable arrivals for testing
            }
            tasks.add(new SimTask(i, bursts[i], arrivalTime));
        }

        return tasks;
    }
}