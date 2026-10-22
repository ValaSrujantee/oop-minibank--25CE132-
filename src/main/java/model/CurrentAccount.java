package model;
public class CurrentAccount extends Account {
    private final long overdraftLimit;
    public CurrentAccount(String number, String owner, long openingBalance, long overdraftLimit) {
        super(number, owner, openingBalance);
        if (overdraftLimit < 0) throw new IllegalArgumentException("Overdraft limit cannot be negative.");
        this.overdraftLimit = overdraftLimit;
    }
    public long getOverdraftLimit() { return overdraftLimit; }
    @Override public double interestRate() { return 0.0; }
    @Override public boolean canWithdraw(long amount) { return amount > 0 && getBalance() - amount >= -overdraftLimit; }
}
