package model;
import java.io.Serial;
public class SavingsAccount extends Account {
    @Serial private static final long serialVersionUID = 1L;
    private final long minBalance;
    public SavingsAccount(String owner, long openingBalance, long minBalance) {
        super(owner, openingBalance);
        this.minBalance = validateMinimum(openingBalance, minBalance);
    }
    public SavingsAccount(String number, String owner, long openingBalance, long minBalance) {
        super(number, owner, openingBalance);
        this.minBalance = validateMinimum(openingBalance, minBalance);
    }
    private static long validateMinimum(long openingBalance, long minBalance) {
        if (minBalance < 0 || openingBalance < minBalance) throw new IllegalArgumentException("Opening balance must meet the minimum balance.");
        return minBalance;
    }
    public long getMinBalance() { return minBalance; }
    @Override public double interestRate() { return 4.0; }
    @Override public boolean canWithdraw(long amount) { return amount > 0 && getBalance() - amount >= minBalance; }
}
