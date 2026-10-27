package repository;

import model.Account;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/** Practical 12 collection store with account lookup, history, and balance sorting. */
public class AccountStore {
    private final HashMap<String, Account> accounts = new HashMap<>();
    private final ArrayList<String> transactionHistory = new ArrayList<>();

    public synchronized void add(Account account) { accounts.put(account.getAccountNumber(), account); }
    public synchronized Account get(String accountNumber) { return accounts.get(accountNumber); }
    public synchronized ArrayList<Account> getAll() { return new ArrayList<>(accounts.values()); }
    public synchronized void addHistory(String entry) { transactionHistory.add(entry); }
    public synchronized ArrayList<String> getTransactionHistory() { return new ArrayList<>(transactionHistory); }
    public synchronized void replaceAll(List<Account> values) {
        accounts.clear();
        for (Account account : values) add(account);
    }
    public synchronized ArrayList<Account> sortedByBalance() {
        ArrayList<Account> sorted = getAll();
        sorted.sort(Comparator.comparingLong(Account::getBalance).thenComparing(Account::getAccountNumber));
        return sorted;
    }
}
