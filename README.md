# MiniBank — OOPJ Part B (Practicals 1–14)

**Student:** Srujantee · **Student ID:** 25ce132

A Java console banking coursework project that implements the MiniBank milestones in the CEUE203 OOPJ practical list. It uses simulated local data only; it does **not** connect to a real bank or payment network.

## Requirements
- JDK 17 or newer
- Optional: Maven 3.9+ to run the JUnit tests and create a packaged JAR
- VS Code with Extension Pack for Java is recommended

## Open in VS Code
1. Extract the ZIP.
2. Open the extracted `MiniBank_Project` folder in VS Code.
3. Open Terminal → New Terminal.
4. Run the commands below from the project root.

## Compile and run without Maven (PowerShell)
```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac -d out '@sources.txt'
java -cp out service.MiniBank
```

## Build and run with Maven
```powershell
mvn test
mvn package
java -jar target/minibank-1.0.0.jar
```
The Maven commands need Maven installed and internet access the first time to download JUnit/build plugins. The plain `javac` commands do not need Maven.

## Menu
1. Open account — Savings (`S`), Current (`C`) or Fixed Deposit (`F`)
2. Deposit
3. Withdraw
4. Transfer
5. Account statement and interest estimate
6. List accounts
7. Report
8. Batch transactions using the shared thread-safe account store
9. Save and exit

New accounts receive generated account numbers. The included sample account uses ID `25ce132` and owner `Srujantee`.

## Data file
The app loads `data/accounts.ser` when available, otherwise it imports `data/accounts.txt`. On exit it writes both the serialized state and the readable text file. Transactions append to `data/logs/transactions.log`; the report menu writes `data/end-of-day-report.txt`.

To run the practical demonstrations without entering the interactive menu:

```powershell
java -jar minibank.jar --practical-demo
```

## Practical concepts included
- P1: `MiniBank` menu, `BankInfo` record, `MenuOption` enum, switch expression
- P2–P3: generated account/customer IDs, encapsulated account operations, readable `toString`, `equals`, `hashCode`, nested `Address`, cloning
- P4: compiled regex validators, parsed transaction commands, formatted account statements
- P5: abstract `Account`, three account subtypes, inheritance and polymorphism
- P6: `Transactable`, `InterestBearing`, `WithdrawRule`, `Premium`, packages and JAR manifest
- P7: runtime `@Id`, `@Positive`, `@MaxLength` annotations and reflective validation/audit helpers
- P8: checked exception hierarchy, friendly error handling, try/catch/finally, try-with-resources
- P9: `AccountWorker`, joined worker threads, and synchronized balance updates
- P10: `ExecutorService` transaction processor, producer/consumer buffer, and consistent account lock ordering for transfers
- P11: object serialization, append-only transaction logs, and NIO summary reports
- P12: `AccountStore`, transaction history, balance sorting, `Comparable`, and a concurrent account map
- P13: generic `Repository<T>` with reflective `@Id`, `saveAll`, and bounded numeric average
- P13–P14: integrated service/menu, JUnit tests, packaging and README

## Notes / scope
- Account balances are whole-number units (`long`) to keep the coursework simple.
- This is an educational simulation. Fixed deposits are locked against withdrawals; savings accounts respect their minimum balance; current accounts support a configured overdraft.
- JUnit tests are in `src/test/java`. Run them with `mvn test`.
- Run `java -jar minibank.jar --practical-demo` to see the milestone examples. Ask your faculty whether they require separate Git commits and tags for each practical milestone.
