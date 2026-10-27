package model;

/** A parsed transaction. For TRANSFER, accountNumber stores "source->destination". */
public record Command(TransactionType type, String accountNumber, long amount) {
    public Command {
        if (type == null) throw new IllegalArgumentException("Transaction type is required.");
        if (accountNumber == null || accountNumber.isBlank()) throw new IllegalArgumentException("Account number is required.");
        if (amount <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
    }

    public String transferSourceAccountNumber() {
        if (type != TransactionType.TRANSFER) throw new IllegalStateException("Command is not a transfer.");
        return transferParts()[0];
    }

    public String transferDestinationAccountNumber() {
        if (type != TransactionType.TRANSFER) throw new IllegalStateException("Command is not a transfer.");
        return transferParts()[1];
    }

    private String[] transferParts() {
        String[] parts = accountNumber.split("->", -1);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalStateException("Transfer account numbers are malformed.");
        }
        return parts;
    }
}
