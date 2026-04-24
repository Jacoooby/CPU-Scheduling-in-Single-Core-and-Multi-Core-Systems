import java.util.Comparator;
import java.util.List;
import java.util.Map;

// SingleCoreDispatcher runs as its own thread and handles selecting and running tasks when using a single CPU.
public class SingleCoreDispatcher extends Thread {
    private final int dispatcherId;
    private final CPU cpu;
    private final ReadyQueue readyQueue;
    // used for PSJF late arrivals, handles tasks that have not arrived yet
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

        // sort pending tasks by arrival time so they can be released in order
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

        // keep looping until tasks have finished
        while (finishedCount < totalTaskCount) {
            // check if any pending tasks have arrived by looking at currentTime. If they have arrived add them
            // to the ready queue
            boolean arrivalsReleased = releasePendingArrivals(currentTime);

            if (arrivalsReleased) {
                readyQueue.printQueue();
            }

            // use scheduler to determine what task to run and for how long it should run
            ScheduleDecision decision = scheduler.chooseNextTask(
                    readyQueue.snapshot(),
                    currentTask,
                    currentTime
            );

            SimTask selectedTask = decision.getSelectedTask();

            // if no task is available increment time and try again
            if (selectedTask == null) {
                currentTime++;
                continue;
            }

            // if switching to a different task, the old task is put back in the ready queue
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
                    // RR always puts the task back in the queue after its quantum
                    selectedTask.setState(ProcessState.READY);
                    readyQueue.addTask(selectedTask);
                    currentTask = null;
                } else if (scheduler.getType() == SchedulerType.PSJF) {
                    // PSJF keeps track of current tasks so it can be preempted on next iteration
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

    // moves any tasks whose arrival time has been reached into the ready queue
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