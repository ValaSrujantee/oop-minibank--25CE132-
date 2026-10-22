package service;
import model.*;
import repository.*;
import exception.*;
import util.*;
import java.util.*;
import java.io.IOException;
import java.nio.file.Path;

public class BankService {
    private final AccountRepository repository;
    public BankService(AccountRepository repository) { this.repository = repository; }
    @Audit public Account openAccount(String type, String number, String owner, long opening, long rule) throws BankException {
        if (number == null || number.isBlank() || owner == null || owner.isBlank()) throw new BankException("Account number and owner are required.");
        if (repository.findById(number).isPresent()) throw new BankException("Account number already exists.");
        if (opening < 0 || rule < 0) throw new InvalidAmountException("Opening balance/rule cannot be negative.");
        Account account;
        try {
            account = switch(type.toUpperCase(Locale.ROOT)) {
                case "S", "SAVINGS" -> new SavingsAccount(number, owner, opening, rule);
                case "C", "CURRENT" -> new CurrentAccount(number, owner, opening, rule);
                case "F", "FIXED" -> new FixedDepositAccount(number, owner, opening);
                default -> throw new BankException("Choose S (Savings), C (Current), or F (Fixed Deposit).");
            };
        } catch (IllegalArgumentException ex) { throw new BankException(ex.getMessage(), ex); }
        repository.save(account); return account;
    }
    @Audit public void deposit(String id, long amount) throws BankException { repository.require(id).deposit(amount); }
    @Audit public void withdraw(String id, long amount) throws BankException { repository.require(id).withdraw(amount); }
    @Audit public void transfer(String from, String to, long amount) throws BankException { repository.require(from).transfer(repository.require(to), amount); }
    public Account statement(String id) throws BankException { return repository.require(id); }
    public List<Account> listAccounts() { return repository.findAll(); }
    public void save(Path path) throws IOException { repository.persist(path); }
    public void load(Path path) throws IOException { repository.load(path); }
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
}
