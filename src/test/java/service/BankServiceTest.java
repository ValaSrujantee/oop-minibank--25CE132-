package service;

import model.Account;
import exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;
import repository.AccountRepository;
import static org.junit.jupiter.api.Assertions.*;

class BankServiceTest {
    @Test void depositIncreasesBalance() throws Exception {
        BankService bank = new BankService(new AccountRepository());
        Account account = bank.openAccount("S", "T100", "Test User", 100, 0);
        bank.deposit("T100", 50);
        assertEquals(150, account.getBalance());
    }
    @Test void withdrawalBeyondAllowedBalanceThrows() throws Exception {
        BankService bank = new BankService(new AccountRepository());
        bank.openAccount("S", "T200", "Test User", 100, 0);
        assertThrows(InsufficientFundsException.class, () -> bank.withdraw("T200", 200));
    }
    @Test void concurrentDepositsKeepCorrectBalance() throws Exception {
        BankService bank = new BankService(new AccountRepository());
        Account account = bank.openAccount("S", "T300", "Test User", 0, 0);
        try (TransactionProcessor processor = new TransactionProcessor()) {
            for (int i=0; i<1000; i++) processor.submit(() -> {
                try { account.deposit(1); } catch (Exception ex) { throw new RuntimeException(ex); }
            });
        }
        assertEquals(1000, account.getBalance());
    }
}
