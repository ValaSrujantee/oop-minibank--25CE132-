package service;

import model.*;
import repository.AccountRepository;
import exception.*;
import util.Validator;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

public class MiniBank {
    private static final Path DATA_FILE = Path.of("data", "accounts.txt");
    private final Scanner sc = new Scanner(System.in);
    private final BankService bank = new BankService(new AccountRepository());

    public static void main(String[] args) { new MiniBank().run(); }

    private void run() {
        try { bank.load(DATA_FILE); System.out.println("Saved accounts loaded."); }
        catch (IOException | RuntimeException ex) { System.out.println("Could not load saved data: " + ex.getMessage()); }
        System.out.println(new BankInfo("MiniBank", "Mon-Sat 9:00 AM - 5:00 PM", "support@minibank.example"));
        boolean running = true;
        while (running) {
            printMenu();
            String raw = sc.nextLine().trim();
            int choice;
            try { choice = Integer.parseInt(raw); }
            catch (NumberFormatException ex) { System.out.println("Enter a number from 1 to 9."); continue; }
            MenuOption option = switch (choice) {
                case 1 -> MenuOption.OPEN_ACCOUNT; case 2 -> MenuOption.DEPOSIT;
                case 3 -> MenuOption.WITHDRAW; case 4 -> MenuOption.TRANSFER;
                case 5 -> MenuOption.STATEMENT; case 6 -> MenuOption.LIST_ACCOUNTS;
                case 7 -> MenuOption.REPORT; case 8 -> MenuOption.BATCH; case 9 -> MenuOption.EXIT;
                default -> null;
            };
            if (option == null) { System.out.println("Invalid choice. Try again."); continue; }
            try {
                switch (option) {
                    case OPEN_ACCOUNT -> openAccount();
                    case DEPOSIT -> deposit();
                    case WITHDRAW -> withdraw();
                    case TRANSFER -> transfer();
                    case STATEMENT -> statement();
                    case LIST_ACCOUNTS -> listAccounts();
                    case REPORT -> report();
                    case BATCH -> batchDemo();
                    case EXIT -> { bank.save(DATA_FILE); System.out.println("Data saved. Thank you for using MiniBank."); running = false; }
                }
            } catch (BankException ex) {
                System.out.println("Banking error: " + ex.getMessage());
                if (ex instanceof InsufficientFundsException funds) System.out.println("Shortfall: " + funds.getShortfall());
            } catch (IOException ex) { System.out.println("File error: " + ex.getMessage()); }
            finally { System.out.println("----------------------------------------"); }
        }
    }

    private void printMenu() {
        System.out.println("\n========== MINIBANK ==========");
        System.out.println("1. Open account");
        System.out.println("2. Deposit");
        System.out.println("3. Withdraw");
        System.out.println("4. Transfer");
        System.out.println("5. Account statement");
        System.out.println("6. List accounts");
        System.out.println("7. Report");
        System.out.println("8. Batch/concurrency demo");
        System.out.println("9. Save and exit");
        System.out.print("Choose: ");
    }
    private void openAccount() throws BankException {
        System.out.print("Account number: "); String id=sc.nextLine().trim();
        System.out.print("Owner name: "); String owner=sc.nextLine().trim();
        System.out.print("Type (S=savings, C=current, F=fixed): "); String type=sc.nextLine().trim();
        long balance=readLong("Opening balance: ");
        long rule=0;
        if (type.equalsIgnoreCase("S")) rule=readLong("Minimum balance: ");
        else if (type.equalsIgnoreCase("C")) rule=readLong("Overdraft limit: ");
        Account a=bank.openAccount(type,id,owner,balance,rule);
        System.out.println("Account opened: " + a);
    }
    private void deposit() throws BankException {
        System.out.print("Account number: "); String id=sc.nextLine().trim();
        bank.deposit(id,readLong("Deposit amount: ")); System.out.println("Deposit successful.");
    }
    private void withdraw() throws BankException {
        System.out.print("Account number: "); String id=sc.nextLine().trim();
        bank.withdraw(id,readLong("Withdrawal amount: ")); System.out.println("Withdrawal successful.");
    }
    private void transfer() throws BankException {
        System.out.print("From account: "); String from=sc.nextLine().trim();
        System.out.print("To account: "); String to=sc.nextLine().trim();
        bank.transfer(from,to,readLong("Transfer amount: ")); System.out.println("Transfer successful.");
    }
    private void statement() throws BankException {
        System.out.print("Account number: "); String id=sc.nextLine().trim();
        Account a=bank.statement(id);
        System.out.println(a); System.out.printf("Interest rate: %.2f%%%nEstimated yearly interest: %.2f%n",a.interestRate(),a.yearlyInterest());
    }
    private void listAccounts() {
        List<Account> accounts=bank.listAccounts();
        if (accounts.isEmpty()) System.out.println("No accounts found.");
        else accounts.forEach(System.out::println);
    }
    private void report() {
        System.out.println("========== BANK REPORT ==========");
        bank.report().forEach((key,value)->System.out.println(key + ": " + value));
    }
    private void batchDemo() {
        AccountRepository repository = new AccountRepository();
        SavingsAccount demo = new SavingsAccount("DEMO-THREAD", "Concurrency Demo", 0, 0);
        try (TransactionProcessor processor = new TransactionProcessor()) {
            for (int i=0; i<1000; i++) processor.submit(() -> {
                try { demo.deposit(1); } catch (InvalidAmountException ex) { throw new RuntimeException(ex); }
            });
        }
        System.out.println("Thread-pool demo: 1000 concurrent deposits completed. Final demo balance: " + demo.getBalance());
        System.out.println("This demonstration account is not saved to your bank data.");
        WithdrawRule anonymous = new WithdrawRule() {
            @Override public boolean allow(Account account, long amount) { return account.canWithdraw(amount); }
        };
        WithdrawRule lambda = (account, amount) -> account.canWithdraw(amount);
        System.out.println("Withdraw rule (anonymous class): " + anonymous.allow(demo,1));
        System.out.println("Withdraw rule (lambda): " + lambda.allow(demo,1));
        try (ClosableDemo resource = new ClosableDemo()) { resource.use(); }
        bank.audit();
    }
    private long readLong(String prompt) {
        while (true) {
            System.out.print(prompt); String raw=sc.nextLine().trim();
            try { return Long.parseLong(raw); }
            catch (NumberFormatException ex) { System.out.println("Please enter a whole number."); }
        }
    }
}
