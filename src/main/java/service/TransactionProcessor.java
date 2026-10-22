package service;
import java.util.concurrent.*;
public class TransactionProcessor implements AutoCloseable {
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    public void submit(Runnable task) { executor.execute(task); }
    public void stop() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException ex) { executor.shutdownNow(); Thread.currentThread().interrupt(); }
    }
    @Override public void close() { stop(); }
}
