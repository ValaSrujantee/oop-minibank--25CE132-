package model;
import java.io.Serial;
public class FixedDepositAccount extends Account {
    @Serial private static final long serialVersionUID = 1L;
    public FixedDepositAccount(String owner, long openingBalance) { super(owner, openingBalance); }
    public FixedDepositAccount(String number, String owner, long openingBalance) { super(number, owner, openingBalance); }
    @Override public double interestRate() { return 7.0; }
    @Override public boolean canWithdraw(long amount) { return false; }
}
