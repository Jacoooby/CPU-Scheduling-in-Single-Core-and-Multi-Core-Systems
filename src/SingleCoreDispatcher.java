import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class SingleCoreDispatcher extends Thread {
    private final int dispatcherId;
    private final CPU cpu;
    private final ReadyQueue readyQueue;
    private final List<SimTask> pendingTasks;
    private final Map<Integer, TaskWorker> workers;
    private final SchedulerStrategy scheduler;
    private final int totalTaskCount;

    public SingleCoreDispatcher(int dispatcherId,
                                CPU cpu,
                                ReadyQueue readyQueue,
                                List<SimTask> pendingTasks,
                                Map<Integer, TaskWorker> workers,
                                SchedulerStrategy scheduler,
                                int totalTaskCount) {
        this.dispatcherId = dispatcherId;
        this.cpu = cpu;
        this.readyQueue = readyQueue;
        this.pendingTasks = pendingTasks;
        this.workers = workers;
        this.scheduler = scheduler;
        this.totalTaskCount = totalTaskCount;

        this.pendingTasks.sort(Comparator.comparingInt(SimTask::getArrivalTime)
                .thenComparingInt(SimTask::getId));
    }

    @Override
    public void run() {
        SimulationLogger.printDispatcherUsesCpu(dispatcherId, cpu.getCpuId());
        SimulationLogger.printDispatcherRelease(dispatcherId);
        SimulationLogger.printDispatcherAlgorithm(dispatcherId, scheduler.getDispatcherMessage());

        int finishedCount = 0;
        int currentTime = 0;
        SimTask currentTask = null;

        while (finishedCount < totalTaskCount) {
            boolean arrivalsReleased = releasePendingArrivals(currentTime);

            if (arrivalsReleased) {
                readyQueue.printQueue();
            }

            ScheduleDecision decision = scheduler.chooseNextTask(
                    readyQueue.snapshot(),
                    currentTask,
                    currentTime
            );

            SimTask selectedTask = decision.getSelectedTask();

            if (selectedTask == null) {
                currentTime++;
                continue;
            }

            if (selectedTask != currentTask) {
                if (currentTask != null && !currentTask.isFinished()) {
                    currentTask.setState(ProcessState.READY);
                    readyQueue.addTask(currentTask);
                }

                readyQueue.removeTask(selectedTask);
            }

            SimulationLogger.printDispatcherRunsProcess(dispatcherId, selectedTask.getId());

            try {
                int actualBurstsRan = cpu.runTask(
                        workers.get(selectedTask.getId()),
                        decision.getBurstTime()
                );

                currentTime += actualBurstsRan;

                if (selectedTask.isFinished()) {
                    selectedTask.setState(ProcessState.FINISHED);
                    finishedCount++;
                    currentTask = null;
                } else if (scheduler.getType() == SchedulerType.RR) {
                    selectedTask.setState(ProcessState.READY);
                    readyQueue.addTask(selectedTask);
                    currentTask = null;
                } else if (scheduler.getType() == SchedulerType.PSJF) {
                    selectedTask.setState(ProcessState.READY);
                    currentTask = selectedTask;
                } else {
                    currentTask = null;
                }

                System.out.println();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private boolean releasePendingArrivals(int currentTime) {
        boolean releasedAny = false;
        int i = 0;

        while (i < pendingTasks.size()) {
            SimTask task = pendingTasks.get(i);

            if (task.getArrivalTime() <= currentTime) {
                task.setState(ProcessState.READY);
                readyQueue.addTask(task);
                pendingTasks.remove(i);
                releasedAny = true;
            } else {
                i++;
            }
        }

        return releasedAny;
    }
}