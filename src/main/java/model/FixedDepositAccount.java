package model;
public class FixedDepositAccount extends Account {
    public FixedDepositAccount(String number, String owner, long openingBalance) { super(number, owner, openingBalance); }
    @Override public double interestRate() { return 7.0; }
    @Override public boolean canWithdraw(long amount) { return false; }
}
