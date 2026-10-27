package service;

import model.Account;
import java.util.Objects;

public class AccountWorker implements Runnable {
    private final Account account;
    private final int times;
    private final long amount;

    public AccountWorker(Account account, int times, long amount) {
        this.account = Objects.requireNonNull(account, "account");
        if (times < 0) throw new IllegalArgumentException("times cannot be negative.");
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive.");
        this.times = times;
        this.amount = amount;
    }

    @Override public void run() {
        System.out.println(Thread.currentThread().getName() + " processing " + times + " deposits.");
        for (int i = 0; i < times; i++) {
            try { account.deposit(amount); }
            catch (Exception ex) { throw new IllegalStateException("Worker deposit failed.", ex); }
        }
    }
}
