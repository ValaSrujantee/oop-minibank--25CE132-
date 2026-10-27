package service;

import model.Account;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.*;

public class TransactionProcessor implements AutoCloseable {
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final ConcurrentMap<String, Account> sharedAccounts;

    public TransactionProcessor() { this(new ConcurrentHashMap<>()); }
    public TransactionProcessor(ConcurrentMap<String, Account> sharedAccounts) {
        this.sharedAccounts = sharedAccounts;
    }

    public void submit(Runnable task) { executor.execute(task); }
    public void register(Account account) { sharedAccounts.put(account.getAccountNumber(), account); }
    public Account findAccount(String accountNumber) { return sharedAccounts.get(accountNumber); }
    public ConcurrentMap<String, Account> sharedAccounts() { return sharedAccounts; }

    public void stop() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException ex) { executor.shutdownNow(); Thread.currentThread().interrupt(); }
    }
    @Override public void close() { stop(); }
}
