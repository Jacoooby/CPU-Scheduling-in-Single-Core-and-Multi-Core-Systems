import java.util.concurrent.Semaphore;

public class TaskWorker extends Thread {
    private final SimTask task;
    private final Semaphore runPermit;
    private final Semaphore sliceComplete;

    private volatile boolean shutdownRequested;
    private volatile int assignedCpuId;
    private volatile int assignedBurstTime;

    public TaskWorker(SimTask task) {
        this.task = task;
        this.runPermit = new Semaphore(0);
        this.sliceComplete = new Semaphore(0);
        this.shutdownRequested = false;
        this.assignedCpuId = -1;
        this.assignedBurstTime = 0;
    }

    public SimTask getTask() {
        return task;
    }

    public void assignCpuBurst(int cpuId, int burstTime) {
        this.assignedCpuId = cpuId;
        this.assignedBurstTime = burstTime;
        runPermit.release();
    }

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

                while (burstsRan < assignedBurstTime && !task.isFinished()) {
                    task.runOneBurst();
                    burstsRan++;
                    SimulationLogger.printBurstProgress(task.getId(), assignedCpuId, task.getCurrentBurst());
                }

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