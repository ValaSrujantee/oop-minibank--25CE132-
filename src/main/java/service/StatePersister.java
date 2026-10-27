package service;

import model.Account;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class StatePersister {
    private StatePersister() { }

    public static void save(Account[] accounts, Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(file))) {
            output.writeObject(accounts);
        }
    }

    public static Account[] load(Path file) throws IOException {
        if (!Files.exists(file)) return new Account[0];
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(file))) {
            Object saved = input.readObject();
            if (!(saved instanceof Account[] accounts)) throw new IOException("Saved state has an unexpected format.");
            return accounts;
        } catch (ClassNotFoundException ex) {
            throw new IOException("Could not read saved account classes.", ex);
        }
    }

    public static Account[] toArray(List<Account> accounts) {
        return new ArrayList<>(accounts).toArray(Account[]::new);
    }
}
