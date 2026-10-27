package util;

import model.Account;

public final class StatementFormatter {
    private StatementFormatter() { }

    public static String buildStatement(Account account) {
        if (account == null) throw new IllegalArgumentException("Account is required.");
        StringBuilder statement = new StringBuilder();
        statement.append("MINIBANK ACCOUNT STATEMENT\n")
                .append("Account number: ").append(account.getAccountNumber()).append('\n')
                .append("Owner: ").append(account.getOwnerName()).append('\n')
                .append("Balance: ").append(account.getBalance()).append(" rupees\n")
                .append("Interest rate: ").append(account.interestRate()).append("%\n")
                .append("Estimated yearly interest: ").append(String.format("%.2f", account.yearlyInterest())).append(" rupees");
        return statement.toString();
    }
}
