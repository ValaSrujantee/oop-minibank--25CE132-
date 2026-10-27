package model;

import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import model.annotation.Id;
import model.annotation.MaxLength;
import model.annotation.Positive;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public abstract class Account implements Transactable, InterestBearing, Comparable<Account>, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private static long accountCounter;

    @Id
    @MaxLength(20)
    private final String accountNumber;
    private final String ownerName;
    @Positive
    private long balance;
    private final boolean active;

    protected Account(String ownerName, long openingBalance) {
        this(generateAccountNumber(), ownerName, openingBalance);
    }

    protected Account(String ownerName) { this(ownerName, 0); }

    protected Account(String accountNumber, String ownerName, long openingBalance) {
        if (accountNumber == null || accountNumber.isBlank() || accountNumber.contains("|") || accountNumber.contains("->")) {
            throw new IllegalArgumentException("Account number is required and cannot contain | or ->.");
        }
        if (ownerName == null || ownerName.isBlank()) throw new IllegalArgumentException("Owner name is required.");
        if (openingBalance < 0) throw new IllegalArgumentException("Opening balance cannot be negative.");
        this.accountNumber = accountNumber.trim();
        updateGeneratedCounter(this.accountNumber);
        this.ownerName = ownerName.trim();
        this.balance = openingBalance;
        this.active = true;
    }

    public String getAccountNumber() { return accountNumber; }
    public String getOwnerName() { return ownerName; }
    @Override public synchronized long getBalance() { return balance; }
    public boolean isActive() { return active; }
    public abstract double interestRate();
    public abstract boolean canWithdraw(long amount);

    private static synchronized String generateAccountNumber() {
        return String.format("AC%04d", ++accountCounter);
    }

    private static synchronized void updateGeneratedCounter(String accountNumber) {
        if (!accountNumber.matches("AC\\d{4,}")) return;
        try { accountCounter = Math.max(accountCounter, Long.parseLong(accountNumber.substring(2))); }
        catch (NumberFormatException ignored) { /* A custom account number is not a generated sequence number. */ }
    }

    public static void observePersistedAccountNumber(String accountNumber) {
        if (accountNumber != null) updateGeneratedCounter(accountNumber.trim());
    }

    @Override public synchronized void deposit(long amount) throws InvalidAmountException {
        if (amount <= 0) throw new InvalidAmountException("Deposit amount must be greater than zero.");
        try { balance = Math.addExact(balance, amount); }
        catch (ArithmeticException ex) { throw new InvalidAmountException("Deposit exceeds the supported balance limit."); }
    }

    @Override public synchronized boolean withdraw(long amount)
            throws InvalidAmountException, InsufficientFundsException {
        if (amount <= 0) throw new InvalidAmountException("Withdrawal amount must be greater than zero.");
        if (!canWithdraw(amount)) {
            long shortfall = Math.max(0L, amount - balance);
            throw new InsufficientFundsException("Withdrawal is not allowed for this account.", shortfall);
        }
        balance -= amount;
        return true;
    }

    public void transfer(Account to, long amount)
            throws exception.BankException {
        if (to == null) throw new exception.AccountNotFoundException("Destination account was not found.");
        if (to == this) throw new exception.BankException("Source and destination accounts must be different.");
        Account first = this.accountNumber.compareTo(to.accountNumber) <= 0 ? this : to;
        Account second = first == this ? to : this;
        synchronized (first) {
            synchronized (second) {
                boolean debited = false;
                boolean committed = false;
                try {
                    if (amount <= 0) throw new InvalidAmountException("Transfer amount must be greater than zero.");
                    if (!canWithdraw(amount)) {
                        throw new InsufficientFundsException("Insufficient funds for transfer.",
                                Math.max(0L, amount - balance));
                    }
                    balance -= amount;
                    debited = true;
                    try {
                        to.deposit(amount);
                        committed = true;
                    } catch (InvalidAmountException | ArithmeticException ex) {
                        throw new exception.BankException("Transfer failed; source balance restored.", ex);
                    }
                } catch (exception.BankException ex) {
                    throw ex;
                } finally {
                    if (debited && !committed) balance += amount;
                }
            }
        }
    }

    @Override public String toString() {
        return accountNumber + " | " + ownerName + " | Balance: " + getBalance() + " | Active: " + active;
    }
    @Override public boolean equals(Object other) {
        return other instanceof Account account && accountNumber.equals(account.accountNumber);
    }
    @Override public int hashCode() { return Objects.hash(accountNumber); }
    @Override public int compareTo(Account other) { return accountNumber.compareTo(other.accountNumber); }
}
