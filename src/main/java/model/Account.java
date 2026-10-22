package model;

import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import java.util.Objects;

public abstract class Account implements Transactable, InterestBearing {
    private final String accountNumber;
    private final String ownerName;
    protected long balance;
    private final boolean active;

    protected Account(String accountNumber, String ownerName, long openingBalance) {
        if (accountNumber == null || accountNumber.isBlank()) throw new IllegalArgumentException("Account number is required.");
        if (ownerName == null || ownerName.isBlank()) throw new IllegalArgumentException("Owner name is required.");
        if (openingBalance < 0) throw new IllegalArgumentException("Opening balance cannot be negative.");
        this.accountNumber = accountNumber.trim();
        this.ownerName = ownerName.trim();
        this.balance = openingBalance;
        this.active = true;
    }

    public String getAccountNumber() { return accountNumber; }
    public String getOwnerName() { return ownerName; }
    @Override public synchronized long getBalance() { return balance; }
    public boolean isActive() { return active; }
    public abstract boolean canWithdraw(long amount);

    @Override public synchronized void deposit(long amount) throws InvalidAmountException {
        if (amount <= 0) throw new InvalidAmountException("Deposit amount must be greater than zero.");
        balance = Math.addExact(balance, amount);
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
                if (amount <= 0) throw new InvalidAmountException("Transfer amount must be greater than zero.");
                if (!canWithdraw(amount)) {
                    throw new InsufficientFundsException("Insufficient funds for transfer.",
                            Math.max(0L, amount - balance));
                }
                balance -= amount;
                try {
                    to.deposit(amount);
                } catch (InvalidAmountException | ArithmeticException ex) {
                    balance += amount;
                    throw new exception.BankException("Transfer failed; source balance restored.", ex);
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
}
