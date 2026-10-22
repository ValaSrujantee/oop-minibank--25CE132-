package repository;
import model.Account;
import exception.AccountNotFoundException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Base64;

public class AccountRepository implements Repository<Account> {
    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
    @Override public void save(Account account) { accounts.put(account.getAccountNumber(), account); }
    @Override public Optional<Account> findById(String id) { return Optional.ofNullable(accounts.get(id)); }
    @Override public List<Account> findAll() {
        List<Account> result = new ArrayList<>(accounts.values());
        result.sort(Comparator.comparing(Account::getAccountNumber));
        return result;
    }
    @Override public boolean deleteById(String id) { return accounts.remove(id) != null; }
    public Account require(String id) throws AccountNotFoundException {
        return findById(id).orElseThrow(() -> new AccountNotFoundException("Account not found: " + id));
    }
    public void load(Path path) throws IOException {
        if (!Files.exists(path)) return;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] p = line.split("\\|", -1);
            if (p.length < 5) continue;
            String number=p[0], owner=new String(Base64.getDecoder().decode(p[1]), StandardCharsets.UTF_8);
            long balance=Long.parseLong(p[2]); String type=p[3]; long rule=Long.parseLong(p[4]);
            Account a = switch(type) {
                case "SAVINGS" -> new model.SavingsAccount(number, owner, balance, rule);
                case "CURRENT" -> restoreCurrent(number, owner, balance, rule);
                case "FIXED" -> new model.FixedDepositAccount(number, owner, balance);
                default -> null;
            };
            if (a != null) save(a);
        }
    }
    private Account restoreCurrent(String number, String owner, long balance, long limit) {
        model.CurrentAccount current = new model.CurrentAccount(number, owner, Math.max(0, balance), limit);
        if (balance < 0) {
            try { current.withdraw(-balance); }
            catch (exception.BankException ex) { throw new IllegalArgumentException("Could not restore current account " + number, ex); }
        }
        return current;
    }
    public void persist(Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        List<String> lines = new ArrayList<>();
        lines.add("# accountNumber|base64(owner)|balance|type|rule");
        for (Account a : findAll()) {
            String owner=Base64.getEncoder().encodeToString(a.getOwnerName().getBytes(StandardCharsets.UTF_8));
            String type; long rule;
            if (a instanceof model.SavingsAccount s) { type="SAVINGS"; rule=s.getMinBalance(); }
            else if (a instanceof model.CurrentAccount c) { type="CURRENT"; rule=c.getOverdraftLimit(); }
            else { type="FIXED"; rule=0; }
            lines.add(a.getAccountNumber()+"|"+owner+"|"+a.getBalance()+"|"+type+"|"+rule);
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }
}
