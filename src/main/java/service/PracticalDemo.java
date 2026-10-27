package service;

import exception.BankException;
import model.*;
import repository.AccountRepository;
import repository.AccountStore;
import repository.Repository;
import util.AnnotationValidator;
import util.CommandParser;
import util.StatementFormatter;
import util.Validator;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicInteger;

/** Runs focused examples for the MiniBank milestones without entering the interactive menu. */
public final class PracticalDemo {
    private PracticalDemo() { }

    public static void run() {
        try {
            System.out.println(new BankInfo("MiniBank", "Ahmedabad"));
            Account savings = new SavingsAccount("Srujantee", 5_000, 500);
            Account current = new CurrentAccount("Srujantee", 2_000, 1_000);
            Account fixed = new FixedDepositAccount("Srujantee", 10_000);
            Account[] accounts = { savings, current, fixed };
            System.out.println("Accounts: " + java.util.Arrays.toString(accounts));
            System.out.println("Savings and duplicate-id equality check: " + savings.equals(
                    new SavingsAccount(savings.getAccountNumber(), "Srujantee", 5_000, 500)));
            System.out.println("Pattern instanceof savings: " + (savings instanceof SavingsAccount typed && typed.getMinBalance() == 500));

            Customer customer = new Customer("Srujantee", "srujantee@example.invalid", "9876543210",
                    new Customer.Address("1 Sample Road", "Ahmedabad", "380001"));
            System.out.println("Customer: " + customer.getCustomerId() + " / " + customer.getName()
                    + " / " + customer.clone().getAddress());

            System.out.println("Validator examples: mobile=" + Validator.isValidMobile("9876543210") + "/"
                    + Validator.isValidMobile("123"));
            System.out.println("Email/PAN/IFSC examples: " + Validator.isValidEmail("srujantee@example.com") + "/"
                    + Validator.isValidPan("ABCDE1234F") + "/" + Validator.isValidIfsc("ABCD0123456"));
            System.out.println("Parsed command: " + CommandParser.parse("DEPOSIT 25ce132 500"));
            System.out.println(StatementFormatter.buildStatement(savings));

            savings.transfer(current, 100);
            System.out.println("Interest polymorphism: " + savings.interestRate() + ", "
                    + current.interestRate() + ", " + fixed.interestRate());
            WithdrawRule anonymous = new WithdrawRule() {
                @Override public boolean allow(Account account, long amount) { return account.canWithdraw(amount); }
            };
            WithdrawRule lambda = (account, amount) -> account.canWithdraw(amount);
            System.out.println("Withdrawal rules: " + anonymous.allow(savings, 1) + "/" + lambda.allow(savings, 1));

            Field balance = Account.class.getDeclaredField("balance");
            balance.setAccessible(true);
            balance.setLong(savings, -1);
            System.out.println("Annotation validation: " + String.join(", ", AnnotationValidator.validate(savings)));

            runWorkerDemo();
            runProducerConsumerDemo();
            runSafeTransferDemo();

            AccountRepository concurrentRepository = new AccountRepository();
            concurrentRepository.saveAll(accounts);
            System.out.println("Thread-safe repository size: " + concurrentRepository.findAll().size());
            try (TransactionProcessor processor = new TransactionProcessor(concurrentRepository.sharedAccounts())) {
                processor.submit(() -> System.out.println("Processor sees account: " +
                        processor.findAccount(savings.getAccountNumber()).getAccountNumber()));
            }
            AccountStore store = new AccountStore();
            for (Account account : accounts) store.add(account);
            store.addHistory("DEPOSIT|" + savings.getAccountNumber() + "|500");
            System.out.println("Sorted by balance: " + store.sortedByBalance());
            System.out.println("Repository average example: " + Repository.average(new Integer[] { 10, 20, 30 }));
            Repository<Account> genericRepository = new Repository<>();
            genericRepository.saveAll(accounts);
            System.out.println("Generic @Id lookup: " + genericRepository.findById(savings.getAccountNumber()));

            Path demoDirectory = Path.of("tmp", "practical-demo");
            StatePersister.save(accounts, demoDirectory.resolve("accounts.ser"));
            System.out.println("Serialized state reloaded: " + StatePersister.load(demoDirectory.resolve("accounts.ser")).length + " accounts");
            TransactionLog.append(demoDirectory.resolve("logs", "transactions.log"),
                    "2026-10-27T00:00:00Z|DEPOSIT|25ce132|500");
            System.out.println(new ReportGenerator().generate(demoDirectory.resolve("logs"),
                    demoDirectory.resolve("report.txt")));
            try (ClosableDemo resource = new ClosableDemo()) { resource.use(); }
        } catch (Exception ex) {
            System.err.println("Practical demo stopped: " + ex.getMessage());
        }
    }

    private static void runWorkerDemo() throws InterruptedException {
        int workerCount = 10;
        int operationsPerWorker = 100;
        UnsafeBalance unsafeBalance = new UnsafeBalance();
        CyclicBarrier allRead = new CyclicBarrier(workerCount);
        CyclicBarrier allWrote = new CyclicBarrier(workerCount);
        Thread[] unsafeWorkers = new Thread[workerCount];
        for (int i = 0; i < unsafeWorkers.length; i++) {
            unsafeWorkers[i] = new Thread(() -> {
                try {
                    for (int operation = 0; operation < operationsPerWorker; operation++) {
                        long observed = unsafeBalance.get();
                        allRead.await();
                        unsafeBalance.set(observed + 1);
                        allWrote.await();
                    }
                } catch (Exception ex) { throw new IllegalStateException(ex); }
            }, "unsafe-account-worker-" + (i + 1));
            unsafeWorkers[i].start();
        }
        for (Thread worker : unsafeWorkers) worker.join();
        System.out.println("Unsynchronized race: expected " + (workerCount * operationsPerWorker)
                + ", actual " + unsafeBalance.get());

        SavingsAccount account = new SavingsAccount("Srujantee", 0, 0);
        Thread[] workers = new Thread[workerCount];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Thread(new AccountWorker(account, operationsPerWorker, 1), "account-worker-" + (i + 1));
            workers[i].start();
        }
        for (Thread worker : workers) worker.join();
        System.out.println("Synchronized workers expected and final balance: " + account.getBalance());
    }

    private static void runProducerConsumerDemo() throws InterruptedException {
        TransactionBuffer buffer = new TransactionBuffer(8);
        AtomicInteger completed = new AtomicInteger();
        Runnable stop = () -> { };
        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    Runnable task = buffer.take();
                    if (task == stop) break;
                    task.run();
                }
            } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        }, "transaction-consumer");
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 25; i++) buffer.put(completed::incrementAndGet);
                buffer.put(stop);
            } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        }, "transaction-producer");
        consumer.start();
        producer.start();
        producer.join();
        consumer.join();
        System.out.println("Producer/consumer transactions processed: " + completed.get());
    }

    private static void runSafeTransferDemo() throws InterruptedException {
        demonstrateDeadlockRisk();
        CurrentAccount first = new CurrentAccount("Srujantee", 10_000, 10_000);
        CurrentAccount second = new CurrentAccount("Srujantee", 10_000, 10_000);
        Thread leftToRight = new Thread(() -> transferMany(first, second), "transfer-A-to-B");
        Thread rightToLeft = new Thread(() -> transferMany(second, first), "transfer-B-to-A");
        leftToRight.start();
        rightToLeft.start();
        leftToRight.join(5_000);
        rightToLeft.join(5_000);
        if (leftToRight.isAlive() || rightToLeft.isAlive()) throw new IllegalStateException("Ordered-lock transfer demo timed out.");
        System.out.println("Opposite-direction transfers completed with account-number lock ordering.");
    }

    private static void demonstrateDeadlockRisk() throws InterruptedException {
        ReentrantLock firstLock = new ReentrantLock();
        ReentrantLock secondLock = new ReentrantLock();
        CountDownLatch bothHoldOneLock = new CountDownLatch(2);
        AtomicInteger timedOut = new AtomicInteger();
        Thread firstOrder = new Thread(() -> acquireOppositeLocks(firstLock, secondLock, bothHoldOneLock, timedOut), "opposite-lock-A");
        Thread secondOrder = new Thread(() -> acquireOppositeLocks(secondLock, firstLock, bothHoldOneLock, timedOut), "opposite-lock-B");
        firstOrder.start();
        secondOrder.start();
        firstOrder.join(2_000);
        secondOrder.join(2_000);
        if (firstOrder.isAlive() || secondOrder.isAlive()) throw new IllegalStateException("Timed deadlock-risk demonstration did not finish.");
        System.out.println("Opposite lock order risk observed with timed acquisition: " + (timedOut.get() > 0));
    }

    private static void acquireOppositeLocks(ReentrantLock first, ReentrantLock second,
                                             CountDownLatch bothHoldOneLock, AtomicInteger timedOut) {
        first.lock();
        try {
            bothHoldOneLock.countDown();
            bothHoldOneLock.await();
            if (second.tryLock(100, TimeUnit.MILLISECONDS)) second.unlock();
            else timedOut.incrementAndGet();
        } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        finally { first.unlock(); }
    }

    private static final class UnsafeBalance {
        private volatile long balance;
        long get() { return balance; }
        void set(long amount) { balance = amount; }
    }

    private static void transferMany(Account from, Account to) {
        try { for (int i = 0; i < 1_000; i++) from.transfer(to, 1); }
        catch (BankException ex) { throw new IllegalStateException(ex); }
    }
}
