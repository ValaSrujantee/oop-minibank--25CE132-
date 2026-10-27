package model;
import java.io.Serial;
public class CurrentAccount extends Account {
    @Serial private static final long serialVersionUID = 1L;
    private final long overdraftLimit;
    public CurrentAccount(String owner, long openingBalance, long overdraftLimit) {
        super(owner, openingBalance);
        this.overdraftLimit = validateLimit(overdraftLimit);
    }
    public CurrentAccount(String number, String owner, long openingBalance, long overdraftLimit) {
        super(number, owner, openingBalance);
        this.overdraftLimit = validateLimit(overdraftLimit);
    }
    private static long validateLimit(long overdraftLimit) {
        if (overdraftLimit < 0) throw new IllegalArgumentException("Overdraft limit cannot be negative.");
        return overdraftLimit;
    }
    public long getOverdraftLimit() { return overdraftLimit; }
    @Override public double interestRate() { return 0.0; }
    @Override public boolean canWithdraw(long amount) {
        if (amount <= 0) return false;
        try { return Math.subtractExact(getBalance(), amount) >= -overdraftLimit; }
        catch (ArithmeticException ex) { return false; }
    }
}
