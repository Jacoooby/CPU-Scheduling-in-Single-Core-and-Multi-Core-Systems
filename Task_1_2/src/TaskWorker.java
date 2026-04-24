import java.util.concurrent.Semaphore;

// TaskWorker works as a process thread that waits to be assigned CPU bursts by the dispatcher
public class TaskWorker extends Thread {
    private final SimTask task;
    // semaphore used to block the worker until the dispatcher assigns it a CPU burst
    private final Semaphore runPermit;
    // semaphore used to signal the dispatcher that the assigned burst slice has completed
    private final Semaphore sliceComplete;

    private volatile boolean shutdownRequested;
    private volatile int assignedCpuId;
    private volatile int assignedBurstTime;

    public TaskWorker(SimTask task) {
        this.task = task;
        // start at 0 so it blocks straight away until given permission to run
        this.runPermit = new Semaphore(0);
        this.sliceComplete = new Semaphore(0);
        this.shutdownRequested = false;
        this.assignedCpuId = -1;
        this.assignedBurstTime = 0;
    }

    public SimTask getTask() {
        return task;
    }

    // called by the dispatcher to give worker a CPU and burst time, then after it unblocks it
    public void assignCpuBurst(int cpuId, int burstTime) {
        this.assignedCpuId = cpuId;
        this.assignedBurstTime = burstTime;
        runPermit.release();
    }

    // dispatcher calls this to wait until the worker finishes its assigned slice
    public void waitForSliceCompletion() throws InterruptedException {
        sliceComplete.acquire();
    }

    public void shutdownWorker() {
        shutdownRequested = true;
        runPermit.release();
    }

    @Override
    public void run() {
        try {
            while (true) {
                // block dispatcher until it assigns a burst
                runPermit.acquire();

                if (shutdownRequested) {
                    break;
                }

                int startBurst = task.getCurrentBurst();
                int burstGoal = Math.min(task.getMaxBurst(), startBurst + assignedBurstTime);

                task.setState(ProcessState.RUNNING);

                SimulationLogger.printProcessHeader(
                        task.getId(),
                        assignedCpuId,
                        task.getMaxBurst(),
                        startBurst,
                        assignedBurstTime,
                        burstGoal
                );

                int burstsRan = 0;

                // run one burst at a time until the slice is done or if the task finishes
                while (burstsRan < assignedBurstTime && !task.isFinished()) {
                    task.runOneBurst();
                    burstsRan++;
                    SimulationLogger.printBurstProgress(task.getId(), assignedCpuId, task.getCurrentBurst());
                }

                // signal dispatcher that a slice is completed
                sliceComplete.release();

                if (task.isFinished()) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}