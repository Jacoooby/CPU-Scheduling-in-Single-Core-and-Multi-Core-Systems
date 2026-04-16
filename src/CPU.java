public class CPU {
    private final int cpuId;

    public CPU(int cpuId) {
        this.cpuId = cpuId;
    }

    public int getCpuId() {
        return cpuId;
    }

    public int runTask(TaskWorker worker, int burstTime) throws InterruptedException {
        if (worker == null) {
            return 0;
        }

        int before = worker.getTask().getCurrentBurst();
        worker.assignCpuBurst(cpuId, burstTime);
        worker.waitForSliceCompletion();
        int after = worker.getTask().getCurrentBurst();

        return after - before;
    }
}