public class SimulationLogger {
    public static synchronized void printSchedulerSelection(SchedulerType schedulerType, Integer quantum) {
        if (schedulerType == SchedulerType.FCFS) {
            System.out.println("Scheduler Algorithm Select: FCFS");
        } else if (schedulerType == SchedulerType.RR) {
            System.out.println("Scheduler Algorithm Select: Round Robin. Time Quantum = " + quantum);
        } else if (schedulerType == SchedulerType.NSJF) {
            System.out.println("Scheduler Algorithm Select: Non Preemptive - Shortest Job First");
        } else {
            System.out.println("Scheduler Algorithm Select: Preemptive - Shortest Job First");
        }
    }

    public static synchronized void printThreadCount(int count) {
        System.out.println("# threads = " + count);
    }

    public static synchronized void printMainCreatesProcess(int processId) {
        System.out.printf("Main thread     | Creating process thread %d%n", processId);
    }

    public static synchronized void printMainForksDispatcher(int dispatcherId) {
        System.out.printf("Main thread     | Forking dispatcher %d%n", dispatcherId);
    }

    public static synchronized void printDispatcherUsesCpu(int dispatcherId, int cpuId) {
        System.out.printf("Dispatcher %-4d | Using CPU %d%n", dispatcherId, cpuId);
    }

    public static synchronized void printDispatcherRelease(int dispatcherId) {
        System.out.printf("Dispatcher %-4d | Now releasing dispatchers.%n", dispatcherId);
        System.out.println();
    }

    public static synchronized void printDispatcherAlgorithm(int dispatcherId, String message) {
        System.out.printf("Dispatcher %-4d | %s%n", dispatcherId, message);
        System.out.println();
    }

    public static synchronized void printDispatcherRunsProcess(int dispatcherId, int processId) {
        System.out.printf("Dispatcher %-4d | Running process %d%n", dispatcherId, processId);
    }

    public static synchronized void printProcessHeader(int processId, int cpuId, int maxBurst, int currentBurst, int burstTime, int burstGoal) {
        System.out.printf("Proc. Thread %-2d | On CPU: MB=%d, CB=%d, BT=%d, BG:=%d%n",
                processId, maxBurst, currentBurst, burstTime, burstGoal);
    }

    public static synchronized void printBurstProgress(int processId, int cpuId, int burstNumber) {
        System.out.printf("Proc. Thread %-2d | Using CPU %d; On burst %d.%n", processId, cpuId, burstNumber);
    }

    public static synchronized void printMainExit() {
        System.out.println();
        System.out.println("Main thread     | Exiting.");
    }
}