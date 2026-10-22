# MiniBank — OOPJ Part B (Practicals 1–14)

A beginner-friendly Java console banking project that brings together the MiniBank milestones in the CEUE203 OOPJ practical list. It uses simulated local data only; it does **not** connect to a real bank or payment network.

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
javac -d out (Get-ChildItem -Recurse src/main/java -Filter *.java | ForEach-Object { $_.FullName })
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
8. Thread-pool/concurrency and interface demonstrations
9. Save and exit

## Data file
The app saves accounts to `data/accounts.txt` in the current working directory when you choose exit. Keep a backup before editing this file manually.

## Practical concepts included
- P1: `MiniBank` menu, `BankInfo` record, `MenuOption` enum, switch expression
- P2–P3: account/customer objects, readable `toString`, `equals`, `hashCode`, nested `Address`, cloning
- P4: collections and repository-based account lookup/listing
- P5: abstract `Account`, three account subtypes, inheritance and polymorphism
- P6: `Transactable`, `InterestBearing`, `WithdrawRule`, `Premium`, packages and JAR manifest
- P7: runtime annotations and reflective validation/audit helpers
- P8: checked exception hierarchy, friendly error handling, try/catch/finally, try-with-resources
- P9: generic `Repository<T>` interface and `Optional<T>` lookup
- P10: `ExecutorService` transaction processor, wait/notify buffer helper, consistent account lock ordering for transfers
- P11: local persistence and summary report
- P12: thread-safe account repository and synchronized balance updates
- P13–P14: integrated service/menu, JUnit tests, packaging and README

## Notes / scope
- Account balances are whole-number units (`long`) to keep the coursework simple.
- This is an educational simulation. Fixed deposits are locked against withdrawals; savings accounts respect their minimum balance; current accounts support a configured overdraft.
- JUnit tests are in `src/test/java`. Run them with `mvn test`.
- Some practical lists describe demonstrations separately; this integrated project combines the concepts in one runnable application. Ask your faculty if they require a separate Git commit/tag for each practical milestone.
