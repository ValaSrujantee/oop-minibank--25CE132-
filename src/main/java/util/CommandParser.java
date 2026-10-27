package util;

import model.Command;
import model.TransactionType;
import java.util.Locale;

public final class CommandParser {
    private CommandParser() { }

    public static Command parse(String line) {
        if (line == null || line.isBlank()) throw new IllegalArgumentException("Enter a transaction command.");
        String[] parts = line.trim().split("\\s+");
        TransactionType type;
        try {
            type = TransactionType.valueOf(parts[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Command must be DEPOSIT, WITHDRAW, or TRANSFER.");
        }

        if (type == TransactionType.TRANSFER) {
            if (parts.length != 4) throw new IllegalArgumentException("Use: TRANSFER <fromAccount> <toAccount> <amount>.");
            return new Command(type, parts[1] + "->" + parts[2], parsePositiveAmount(parts[3]));
        }
        if (parts.length != 3) throw new IllegalArgumentException("Use: " + type + " <account> <amount>.");
        return new Command(type, parts[1], parsePositiveAmount(parts[2]));
    }

    private static long parsePositiveAmount(String raw) {
        try {
            long amount = Long.parseLong(raw);
            if (amount <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
            return amount;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Amount must be a whole number.", ex);
        }
    }
}
