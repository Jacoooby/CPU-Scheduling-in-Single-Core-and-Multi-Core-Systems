public class SimTask {
    private final int id;
    private final int maxBurst;
    private int currentBurst;
    private final int arrivalTime;
    private ProcessState state;

    public SimTask(int id, int maxBurst, int arrivalTime) {
        if (id < 0) {
            throw new IllegalArgumentException("Task ID cannot be negative.");
        }
        if (maxBurst < 1) {
            throw new IllegalArgumentException("Burst time must be at least 1.");
        }
        if (arrivalTime < 0) {
            throw new IllegalArgumentException("Arrival time cannot be negative.");
        }

        this.id = id;
        this.maxBurst = maxBurst;
        this.currentBurst = 0;
        this.arrivalTime = arrivalTime;
        this.state = ProcessState.NEW;
    }

    public synchronized int getId() {
        return id;
    }

    public synchronized int getMaxBurst() {
        return maxBurst;
    }

    public synchronized int getCurrentBurst() {
        return currentBurst;
    }

    public synchronized int getArrivalTime() {
        return arrivalTime;
    }

    public synchronized ProcessState getState() {
        return state;
    }

    public synchronized void setState(ProcessState state) {
        this.state = state;
    }

    public synchronized int getRemainingBurst() {
        return maxBurst - currentBurst;
    }

    public synchronized boolean isFinished() {
        return currentBurst >= maxBurst;
    }

    public synchronized void runOneBurst() {
        if (!isFinished()) {
            currentBurst++;
            if (isFinished()) {
                state = ProcessState.FINISHED;
            }
        }
    }

    @Override
    public synchronized String toString() {
        return "ID:" + id + ", Max Burst:" + maxBurst + ", Current Burst:" + currentBurst;
    }
}