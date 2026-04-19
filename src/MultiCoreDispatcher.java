// Being code changes by Hunter Aden
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiCoreDispatcher extends Thread{
    private final int dispatcherID;
    private final CPU cpu;
    private final ReadyQueue readyQueue;
    private final Map<Integer, TaskWorker> workers;
    private final int totalTaskCount;
    private final SchedulerStrategy scheduler;
    private final AtomicInteger finishedCount;

    public MultiCoreDispatcher (int dispatcherID, CPU cpu, ReadyQueue readyQueue,
                                Map<Integer, TaskWorker> workers, int totalTaskCount,
                                SchedulerStrategy scheduler, AtomicInteger finishedCount){

        this.dispatcherID = dispatcherID;
        this.cpu = cpu;
        this.readyQueue = readyQueue;
        this.workers = workers;
        this.totalTaskCount = totalTaskCount;
        this.scheduler = scheduler;
        this.finishedCount = finishedCount;
    }

    public void run(){
        SimulationLogger.printDispatcherUsesCpu(dispatcherID, cpu.getCpuId());
        SimulationLogger.printDispatcherRelease(dispatcherID);
        SimulationLogger.printDispatcherAlgorithm(dispatcherID, scheduler.getDispatcherMessage());

        int currentTime = 0;

        while (finishedCount.get() < totalTaskCount) {
            ScheduleDecision decision;
            //Synchronize tasks so two dispatchers do not get the same task
            synchronized (readyQueue) {
                List<SimTask> snapshot = readyQueue.snapshot();
                decision = scheduler.chooseNextTask(snapshot, null, currentTime);

                if (decision.getSelectedTask() == null) {
                    //No task, sleep and try again
                    currentTime ++;
                    try {
                        Thread.sleep(1);
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    continue;
                }
                readyQueue.removeTask(decision.getSelectedTask());
            }
            SimTask selectedTask = decision.getSelectedTask();
            SimulationLogger.printDispatcherRunsProcess(dispatcherID, selectedTask.getId());

            try {
                int actualBurstsRan = cpu.runTask(workers.get(selectedTask.getId()), decision.getBurstTime());
                currentTime += actualBurstsRan;

                if (selectedTask.isFinished()) {
                    selectedTask.setState(ProcessState.READY);
                    finishedCount.incrementAndGet();
                } else if (scheduler.getType() == SchedulerType.RR) {
                    selectedTask.setState(ProcessState.READY);
                    readyQueue.addTask(selectedTask);
                } else {
                    selectedTask.setState(ProcessState.READY);
                    readyQueue.addTask(selectedTask);
                }
                System.out.println();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
