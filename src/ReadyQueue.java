import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ReadyQueue {
    private final LinkedList<SimTask> queue;

    public ReadyQueue() {
        this.queue = new LinkedList<>();
    }

    public synchronized void addTask(SimTask task) {
        if (task != null && !queue.contains(task)) {
            queue.add(task);
        }
    }

    public synchronized boolean removeTask(SimTask task) {
        return queue.remove(task);
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized List<SimTask> snapshot() {
        return new ArrayList<>(queue);
    }

    public synchronized void printQueue() {
        System.out.println();
        System.out.println("--------------- Ready Queue ---------------");
        for (SimTask task : queue) {
            System.out.println("ID:" + task.getId()
                    + ", Max Burst:" + task.getMaxBurst()
                    + ", Current Burst:" + task.getCurrentBurst());
        }
        System.out.println("-------------------------------------------");
        System.out.println();
    }
}