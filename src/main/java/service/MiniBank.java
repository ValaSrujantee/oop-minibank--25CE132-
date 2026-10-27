package service;

import exception.BankException;
import exception.InsufficientFundsException;
import model.Account;
import model.BankInfo;
import model.Command;
import model.MenuOption;
import model.SavingsAccount;
import model.TransactionType;
import model.WithdrawRule;
import repository.AccountRepository;
import util.CommandParser;
import util.StatementFormatter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class MiniBank {
    private static final Path DATA_FILE = Path.of("data", "accounts.txt");
    private static final Path STATE_FILE = Path.of("data", "accounts.ser");
    private static final Path LOG_DIRECTORY = Path.of("data", "logs");
    private static final Path REPORT_FILE = Path.of("data", "end-of-day-report.txt");
    private final Scanner sc = new Scanner(System.in);
    private final BankService bank = new BankService(new AccountRepository());

    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--practical-demo")) {
            PracticalDemo.run();
            return;
        }
        new MiniBank().run();
    }

    private void run() {
        try { bank.loadState(STATE_FILE, DATA_FILE); System.out.println("Saved accounts loaded."); }
        catch (IOException | RuntimeException ex) { System.out.println("Could not load saved data: " + ex.getMessage()); }
        System.out.println(new BankInfo("MiniBank", "Ahmedabad"));
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
                    case BATCH -> batchTransactions();
                    case EXIT -> {
                        bank.saveState(STATE_FILE, DATA_FILE);
                        System.out.println("Data saved. Thank you for using MiniBank.");
                        running = false;
                    }
                }
            } catch (BankException ex) {
                System.out.println("Banking error: " + ex.getMessage());
                if (ex instanceof InsufficientFundsException funds) System.out.println("Shortfall: " + funds.getShortfall());
            } catch (IOException ex) { System.out.println("File error: " + ex.getMessage()); }
            catch (IllegalArgumentException ex) { System.out.println("Input error: " + ex.getMessage()); }
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
        System.out.println("8. Batch transactions");
        System.out.println("9. Save and exit");
        System.out.print("Choose: ");
    }

    private void openAccount() throws BankException {
        System.out.print("Owner name: "); String owner=sc.nextLine().trim();
        System.out.print("Type (S=savings, C=current, F=fixed): "); String type=sc.nextLine().trim();
        long balance=readLong("Opening balance: ");
        long rule=0;
        if (type.equalsIgnoreCase("S")) rule=readLong("Minimum balance: ");
        else if (type.equalsIgnoreCase("C")) rule=readLong("Overdraft limit: ");
        Account account=bank.openAccount(type,owner,balance,rule);
        System.out.println("Account opened: " + account);
    }

    private void deposit() throws BankException {
        System.out.print("Account number: "); String id=sc.nextLine().trim();
        bank.execute(CommandParser.parse("DEPOSIT " + id + " " + readLong("Deposit amount: ")));
        System.out.println("Deposit successful.");
    }

    private void withdraw() throws BankException {
        System.out.print("Account number: "); String id=sc.nextLine().trim();
        bank.execute(CommandParser.parse("WITHDRAW " + id + " " + readLong("Withdrawal amount: ")));
        System.out.println("Withdrawal successful.");
    }

    private void transfer() throws BankException {
        System.out.print("From account: "); String from=sc.nextLine().trim();
        System.out.print("To account: "); String to=sc.nextLine().trim();
        bank.execute(CommandParser.parse("TRANSFER " + from + " " + to + " " + readLong("Transfer amount: ")));
        System.out.println("Transfer successful.");
    }

    private void statement() throws BankException {
        System.out.print("Account number: "); String id=sc.nextLine().trim();
        System.out.println(StatementFormatter.buildStatement(bank.statement(id)));
    }

    private void listAccounts() {
        List<Account> accounts=bank.listAccountsSortedByBalance();
        if (accounts.isEmpty()) System.out.println("No accounts found.");
        else {
            System.out.println("Accounts sorted by balance:");
            accounts.forEach(System.out::println);
        }
    }

    private void report() {
        System.out.println("========== BANK REPORT ==========");
        bank.report().forEach((key,value)->System.out.println(key + ": " + value));
        try { System.out.println(bank.generateEndOfDayReport(LOG_DIRECTORY, REPORT_FILE)); }
        catch (IOException ex) { System.out.println("Could not generate transaction report: " + ex.getMessage()); }
    }

    private void batchTransactions() throws BankException {
        int count = (int) readPositiveLong("Number of transactions in this batch (1-10000): ");
        List<Command> commands = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            System.out.print("Transaction " + (i + 1) + " (DEPOSIT account amount | WITHDRAW account amount | TRANSFER from to amount): ");
            try { commands.add(CommandParser.parse(sc.nextLine())); }
            catch (IllegalArgumentException ex) {
                System.out.println(ex.getMessage());
                i--;
            }
        }
        bank.processBatch(commands);
        System.out.println("Batch completed using the shared thread-safe account store.");
    }

    private long readLong(String prompt) {
        while (true) {
            System.out.print(prompt); String raw=sc.nextLine().trim();
            try { return Long.parseLong(raw); }
            catch (NumberFormatException ex) { System.out.println("Please enter a whole number."); }
        }
    }

    private long readPositiveLong(String prompt) {
        while (true) {
            long value=readLong(prompt);
            if (value > 0 && value <= 10_000) return value;
            System.out.println("Enter a value from 1 to 10000.");
        }
    }
}
