package model;
public class SavingsAccount extends Account {
    private final long minBalance;
    public SavingsAccount(String number, String owner, long openingBalance, long minBalance) {
        super(number, owner, openingBalance);
        if (minBalance < 0 || openingBalance < minBalance) throw new IllegalArgumentException("Opening balance must meet the minimum balance.");
        this.minBalance = minBalance;
    }
    public long getMinBalance() { return minBalance; }
    @Override public double interestRate() { return 4.0; }
    @Override public boolean canWithdraw(long amount) { return amount > 0 && getBalance() - amount >= minBalance; }
}
