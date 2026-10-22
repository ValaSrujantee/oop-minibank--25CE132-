package service;
import java.util.ArrayDeque;
import java.util.Queue;
public class TransactionBuffer {
    private final Queue<Runnable> buffer = new ArrayDeque<>();
    private final int capacity;
    public TransactionBuffer(int capacity) { this.capacity = capacity; }
    public synchronized void put(Runnable task) throws InterruptedException {
        while (buffer.size() == capacity) wait();
        buffer.add(task); notifyAll();
    }
    public synchronized Runnable take() throws InterruptedException {
        while (buffer.isEmpty()) wait();
        Runnable task = buffer.remove(); notifyAll(); return task;
    }
}
