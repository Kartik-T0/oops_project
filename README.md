# Banking Management System

A beginner-friendly console banking app in plain Java (JDK 8+), built for a university OOP project. It uses only the standard library.

## Features
- Create Savings and Current accounts (auto numbers: ACC1001, ACC1002, ...)
- Deposit, withdraw and transfer money
- Transaction history per account
- Interest (Savings 4%) and 1% overdraft charge (Current)
- Data saved to `accounts.txt` and `transactions.txt` in the folder you run the program from

## Rules
- Savings: opening balance of at least 500, minimum balance 500, 4% interest
- Current: overdraft allowed down to -5000, no interest

## OOP Concepts Used
Encapsulation, abstraction (abstract class), interface (`Transactable`), inheritance, polymorphism, method overloading, method overriding, custom checked exceptions, `ArrayList` and `HashMap`, file handling.

## How to Run
```
javac BankingSystem.java
java BankingSystem
```

## Menu
1 Create Account, 2 Deposit, 3 Withdraw, 4 Transfer, 5 Show Accounts, 6 Show Transactions, 7 Apply Interest, 8 Exit
