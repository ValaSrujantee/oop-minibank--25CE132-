package service;
import model.*;
import repository.*;
import exception.*;
import util.*;
import static util.Validator.isPositive;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.IOException;
import java.nio.file.Path;

public class BankService {
    private final AccountRepository repository;
    private final AccountStore accountStore = new AccountStore();
    private final Path transactionLogPath;

    public BankService(AccountRepository repository) { this(repository, Path.of("data", "logs", "transactions.log")); }
    public BankService(AccountRepository repository, Path transactionLogPath) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.transactionLogPath = Objects.requireNonNull(transactionLogPath, "transactionLogPath");
        refreshStore();
    }

    @Audit public Account openAccount(String type, String number, String owner, long opening, long rule) throws BankException {
        if (number == null || number.isBlank() || owner == null || owner.isBlank()) throw new BankException("Account number and owner are required.");
        if (repository.findById(number) != null) throw new BankException("Account number already exists.");
        return createAccount(type, number, owner, opening, rule);
    }

    @Audit public Account openAccount(String type, String owner, long opening, long rule) throws BankException {
        if (owner == null || owner.isBlank()) throw new BankException("Owner name is required.");
        return createAccount(type, null, owner, opening, rule);
    }

    private Account createAccount(String type, String number, String owner, long opening, long rule) throws BankException {
        if (opening < 0 || rule < 0) throw new InvalidAmountException("Opening balance/rule cannot be negative.");
        if (opening > 0 && !isPositive(opening)) throw new InvalidAmountException("Opening balance must be positive.");
        if (type == null) throw new BankException("Choose S (Savings), C (Current), or F (Fixed Deposit).");
        Account account;
        try {
            account = switch(type.trim().toUpperCase(Locale.ROOT)) {
                case "S", "SAVINGS" -> number == null ? new SavingsAccount(owner, opening, rule) : new SavingsAccount(number, owner, opening, rule);
                case "C", "CURRENT" -> number == null ? new CurrentAccount(owner, opening, rule) : new CurrentAccount(number, owner, opening, rule);
                case "F", "FIXED" -> number == null ? new FixedDepositAccount(owner, opening) : new FixedDepositAccount(number, owner, opening);
                default -> throw new BankException("Choose S (Savings), C (Current), or F (Fixed Deposit).");
            };
        } catch (IllegalArgumentException ex) { throw new BankException(ex.getMessage(), ex); }
        repository.save(account);
        accountStore.add(account);
        appendLog("OPEN", account.getAccountNumber(), opening);
        return account;
    }

    @Audit public void deposit(String id, long amount) throws BankException {
        repository.require(id).deposit(amount);
        appendLog("DEPOSIT", id, amount);
    }

    @Audit public void withdraw(String id, long amount) throws BankException {
        repository.require(id).withdraw(amount);
        appendLog("WITHDRAW", id, amount);
    }

    @Audit public void transfer(String from, String to, long amount) throws BankException {
        repository.require(from).transfer(repository.require(to), amount);
        appendLog("TRANSFER_OUT", from, amount);
        appendLog("TRANSFER_IN", to, amount);
    }

    public void execute(Command command) throws BankException {
        switch (command.type()) {
            case DEPOSIT -> deposit(command.accountNumber(), command.amount());
            case WITHDRAW -> withdraw(command.accountNumber(), command.amount());
            case TRANSFER -> transfer(command.transferSourceAccountNumber(), command.transferDestinationAccountNumber(), command.amount());
        }
    }

    public void processBatch(List<Command> commands) throws BankException {
        ConcurrentLinkedQueue<BankException> failures = new ConcurrentLinkedQueue<>();
        try (TransactionProcessor processor = new TransactionProcessor(repository.sharedAccounts())) {
            for (Command command : commands) processor.submit(() -> {
                try { execute(command); }
                catch (BankException ex) { failures.add(ex); }
            });
        }
        if (!failures.isEmpty()) throw new BankException("Batch completed with " + failures.size() + " failed transaction(s).", failures.peek());
    }

    public Account statement(String id) throws BankException { return repository.require(id); }
    public List<Account> listAccounts() { return repository.findAll(); }
    public List<Account> listAccountsSortedByBalance() { return accountStore.sortedByBalance(); }
    public List<String> transactionHistory() { return accountStore.getTransactionHistory(); }
    public void save(Path path) throws IOException { repository.persist(path); }
    public void load(Path path) throws IOException { repository.load(path); }

    public void loadState(Path serializedFile, Path legacyTextFile) throws IOException {
        if (java.nio.file.Files.exists(serializedFile)) {
            for (Account account : StatePersister.load(serializedFile)) repository.save(account);
        } else {
            repository.load(legacyTextFile);
        }
        refreshStore();
    }

    public void saveState(Path serializedFile, Path readableTextFile) throws IOException {
        StatePersister.save(StatePersister.toArray(repository.findAll()), serializedFile);
        repository.persist(readableTextFile);
    }

    public String generateEndOfDayReport(Path logsDirectory, Path reportFile) throws IOException {
        return new ReportGenerator().generate(logsDirectory, reportFile);
    }

    public Map<String, Long> report() {
        List<Account> list = listAccounts();
        long total=0; for (Account a:list) total += a.getBalance();
        Map<String,Long> report = new LinkedHashMap<>();
        report.put("Account count", (long)list.size()); report.put("Total balance", total);
        report.put("Savings accounts", list.stream().filter(SavingsAccount.class::isInstance).count());
        report.put("Current accounts", list.stream().filter(CurrentAccount.class::isInstance).count());
        report.put("Fixed deposit accounts", list.stream().filter(FixedDepositAccount.class::isInstance).count());
        return report;
    }
    public void audit() { Validator.logAudit(this); }

    private void appendLog(String type, String accountNumber, long amount) throws BankException {
        String line = java.time.Instant.now() + "|" + type + "|" + accountNumber + "|" + amount;
        try { TransactionLog.append(transactionLogPath, line); }
        catch (IOException ex) { throw new BankException("Transaction completed, but the log could not be written.", ex); }
        accountStore.addHistory(line);
    }

    private void refreshStore() { accountStore.replaceAll(repository.findAll()); }
}
