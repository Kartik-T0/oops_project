import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;

// ---------- Custom exceptions (all checked) ----------

// Thrown when an amount is zero or negative
class InvalidAmountException extends Exception {
    InvalidAmountException(String message) { super(message); }
}

// Thrown when the account does not have enough money
class InsufficientFundsException extends Exception {
    InsufficientFundsException(String message) { super(message); }
}

// Thrown when an account number does not exist
class AccountNotFoundException extends Exception {
    AccountNotFoundException(String message) { super(message); }
}

// ---------- Interface ----------

// OOP CONCEPT: Interface - a contract that every transactable class must follow
// Both methods declare all exceptions because an implementing method cannot add new checked ones
interface Transactable {
    void deposit(double amount) throws InvalidAmountException;
    void withdraw(double amount) throws InvalidAmountException, InsufficientFundsException;
    double getBalance();
}

// ---------- Account classes ----------

// Base class for all accounts.
// OOP CONCEPT: Abstraction - abstract class, cannot be created directly
abstract class Account implements Transactable {
    // OOP CONCEPT: Encapsulation - private data, accessed by methods
    private String accountNumber;
    private String holderName;
    private double balance;

    // Creates an account with a starting balance
    Account(String accountNumber, String holderName, double balance) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.balance = balance;
    }

    // OOP CONCEPT: Constructor overloading - starts with zero balance
    Account(String accountNumber, String holderName) {
        this(accountNumber, holderName, 0);
    }

    String getAccountNumber() { return accountNumber; }
    String getHolderName() { return holderName; }
    public double getBalance() { return balance; }

    // Child classes use this to change the balance
    protected void setBalance(double balance) { this.balance = balance; }

    // Stops zero or negative amounts
    protected void checkAmount(double amount) throws InvalidAmountException {
        if (amount <= 0) throw new InvalidAmountException("Amount must be greater than 0.");
    }

    // Adds money to the account
    public void deposit(double amount) throws InvalidAmountException {
        checkAmount(amount);
        balance += amount;
    }

    // OOP CONCEPT: Method overloading - same name, different parameters
    public void deposit(double amount, String note) throws InvalidAmountException {
        System.out.println("Note: " + note);
        deposit(amount);
    }

    // Default withdraw - child classes change the rules
    public void withdraw(double amount) throws InvalidAmountException, InsufficientFundsException {
        checkAmount(amount);
        if (amount > balance) throw new InsufficientFundsException("Not enough balance.");
        balance -= amount;
    }

    // OOP CONCEPT: Abstract methods - every child must write its own version
    abstract double calculateInterest();
    abstract String getAccountType();

    public String toString() {
        return getAccountType() + " | " + accountNumber + " | " + holderName + " | Balance: " + balance;
    }
}

// OOP CONCEPT: Inheritance - SavingsAccount extends Account
class SavingsAccount extends Account {
    SavingsAccount(String accountNumber, String holderName, double balance) {
        super(accountNumber, holderName, balance);
    }

    // OOP CONCEPT: Method overriding - must keep minimum balance of 500
    public void withdraw(double amount) throws InvalidAmountException, InsufficientFundsException {
        checkAmount(amount);
        if (getBalance() - amount < 500) throw new InsufficientFundsException("Savings must keep a minimum balance of 500.");
        setBalance(getBalance() - amount);
    }

    double calculateInterest() { return getBalance() * 0.04; }
    String getAccountType() { return "Savings"; }
}

// OOP CONCEPT: Inheritance - CurrentAccount extends Account
class CurrentAccount extends Account {
    CurrentAccount(String accountNumber, String holderName, double balance) {
        super(accountNumber, holderName, balance);
    }

    // OOP CONCEPT: Method overriding - allows overdraft down to -5000
    public void withdraw(double amount) throws InvalidAmountException, InsufficientFundsException {
        checkAmount(amount);
        if (getBalance() - amount < -5000) throw new InsufficientFundsException("Overdraft limit of -5000 reached.");
        setBalance(getBalance() - amount);
    }

    double calculateInterest() { return 0; }
    String getAccountType() { return "Current"; }
}

// ---------- Transaction ----------

// One record in the transaction history
class Transaction {
    private String type;
    private String accountNumber;
    private double amount;
    private String date;
    private String note;

    // New transaction - date is taken from the system clock
    Transaction(String type, String accountNumber, double amount, String note) {
        this(type, accountNumber, amount, new Date().toString(), note);
    }

    // Used when loading from file (date already known)
    Transaction(String type, String accountNumber, double amount, String date, String note) {
        this.type = type;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.date = date;
        this.note = note;
    }

    String getType() { return type; }
    String getAccountNumber() { return accountNumber; }
    double getAmount() { return amount; }
    String getDate() { return date; }
    String getNote() { return note; }

    public String toString() {
        return date + " | " + type + " | " + accountNumber + " | " + amount + " | " + note;
    }
}

// ---------- Bank ----------

// Holds all accounts and transactions and does the real work
class Bank {
    // Accounts stored by account number, history stored in order
    private HashMap<String, Account> accounts = new HashMap<String, Account>();
    private ArrayList<Transaction> transactions = new ArrayList<Transaction>();
    private int nextNumber = 1001;

    // Files are saved in the folder the program is run from
    private static final String ACCOUNTS_FILE = "accounts.txt";
    private static final String TRANSACTIONS_FILE = "transactions.txt";

    // Removes commas so the CSV files stay valid
    private String clean(String text) {
        return text.replace(",", " ");
    }

    // Creates a new account with an auto-generated number like ACC1001
    String createAccount(int type, String name, double openingBalance) throws InvalidAmountException {
        if (openingBalance < 0) throw new InvalidAmountException("Opening balance cannot be negative.");
        if (type == 1 && openingBalance < 500) throw new InvalidAmountException("Savings needs an opening balance of at least 500.");

        String number = "ACC" + nextNumber;
        nextNumber++;
        name = clean(name);

        // OOP CONCEPT: Polymorphism - Account variable holds a child object
        Account acc;
        if (type == 1) acc = new SavingsAccount(number, name, openingBalance);
        else acc = new CurrentAccount(number, name, openingBalance);

        accounts.put(number, acc);
        saveToFile();
        return number;
    }

    // Finds an account or throws an exception
    Account findAccount(String number) throws AccountNotFoundException {
        Account acc = accounts.get(number);
        if (acc == null) throw new AccountNotFoundException("Account " + number + " not found.");
        return acc;
    }

    // Deposits money and records it
    void deposit(String number, double amount) throws AccountNotFoundException, InvalidAmountException {
        Account acc = findAccount(number);
        acc.deposit(amount);
        transactions.add(new Transaction("DEPOSIT", number, amount, "Cash deposit"));
        saveToFile();
    }

    // Withdraws money and records it
    void withdraw(String number, double amount)
            throws AccountNotFoundException, InvalidAmountException, InsufficientFundsException {
        Account acc = findAccount(number);
        acc.withdraw(amount);
        transactions.add(new Transaction("WITHDRAW", number, amount, "Cash withdrawal"));
        saveToFile();
    }

    // OOP CONCEPT: Method overloading - transfer without a note
    void transfer(String from, String to, double amount)
            throws AccountNotFoundException, InvalidAmountException, InsufficientFundsException {
        transfer(from, to, amount, "Transfer");
    }

    // Moves money between two accounts and records two transactions
    void transfer(String from, String to, double amount, String note)
            throws AccountNotFoundException, InvalidAmountException, InsufficientFundsException {
        Account source = findAccount(from);
        Account target = findAccount(to);
        note = clean(note);

        source.withdraw(amount);   // if this fails, nothing changes
        target.deposit(amount);

        transactions.add(new Transaction("TRANSFER_OUT", from, amount, note + " to " + to));
        transactions.add(new Transaction("TRANSFER_IN", to, amount, note + " from " + from));
        saveToFile();
    }

    // Prints every account
    void showAllAccounts() {
        if (accounts.isEmpty()) {
            System.out.println("No accounts yet.");
            return;
        }
        for (Account acc : accounts.values()) {
            System.out.println(acc);
        }
    }

    // Prints the history of one account
    void showTransactions(String number) throws AccountNotFoundException {
        findAccount(number);
        boolean found = false;
        for (Transaction t : transactions) {
            if (t.getAccountNumber().equals(number)) {
                System.out.println(t);
                found = true;
            }
        }
        if (!found) System.out.println("No transactions yet.");
    }

    // OOP CONCEPT: Polymorphism - the loop uses Account type, but each object runs its own calculateInterest()
    void applyInterestToAll() {
        for (Account acc : accounts.values()) {
            try {
                double interest = acc.calculateInterest();
                if (interest > 0) {
                    acc.deposit(interest);
                    transactions.add(new Transaction("INTEREST", acc.getAccountNumber(), interest, "Interest added"));
                }
                // Overdrawn accounts pay 1% of the overdraft through withdraw()
                if (acc.getBalance() < 0) {
                    double fee = -acc.getBalance() * 0.01;
                    acc.withdraw(fee);
                    transactions.add(new Transaction("FEE", acc.getAccountNumber(), fee, "Overdraft charge 1%"));
                }
            } catch (InvalidAmountException e) {
                System.out.println(acc.getAccountNumber() + ": " + e.getMessage());
            } catch (InsufficientFundsException e) {
                System.out.println(acc.getAccountNumber() + ": " + e.getMessage());
            }
        }
        saveToFile();
        System.out.println("Interest applied.");
    }

    // Saves accounts and transactions as comma-separated lines
    void saveToFile() {
        try {
            FileWriter aw = new FileWriter(ACCOUNTS_FILE);
            for (Account acc : accounts.values()) {
                aw.write(acc.getAccountType() + "," + acc.getAccountNumber() + ","
                        + acc.getHolderName() + "," + acc.getBalance() + "\n");
            }
            aw.close();

            FileWriter tw = new FileWriter(TRANSACTIONS_FILE);
            for (Transaction t : transactions) {
                tw.write(t.getType() + "," + t.getAccountNumber() + "," + t.getAmount() + ","
                        + t.getDate() + "," + t.getNote() + "\n");
            }
            tw.close();
        } catch (IOException e) {
            System.out.println("Could not save: " + e.getMessage());
        }
    }

    // Loads data at start. A missing file just means we start empty.
    void loadFromFile() {
        try {
            BufferedReader ar = new BufferedReader(new FileReader(ACCOUNTS_FILE));
            String line;
            while ((line = ar.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length < 4) continue;
                double balance = Double.parseDouble(p[3]);
                Account acc;
                if (p[0].equals("Savings")) acc = new SavingsAccount(p[1], p[2], balance);
                else acc = new CurrentAccount(p[1], p[2], balance);
                accounts.put(p[1], acc);

                // Keep the next number higher than any loaded number
                int num = Integer.parseInt(p[1].substring(3));
                if (num >= nextNumber) nextNumber = num + 1;
            }
            ar.close();
        } catch (IOException e) {
            // File missing - start empty
        } catch (NumberFormatException e) {
            System.out.println("accounts.txt has bad data, some accounts skipped.");
        }

        try {
            BufferedReader tr = new BufferedReader(new FileReader(TRANSACTIONS_FILE));
            String line;
            while ((line = tr.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length < 5) continue;
                transactions.add(new Transaction(p[0], p[1], Double.parseDouble(p[2]), p[3], p[4]));
            }
            tr.close();
        } catch (IOException e) {
            // File missing - start empty
        } catch (NumberFormatException e) {
            System.out.println("transactions.txt has bad data, some lines skipped.");
        }
    }
}

// ---------- Main program ----------

public class BankingSystem {
    static Scanner sc = new Scanner(System.in);

    // Asks a question and returns the typed line (names with spaces work)
    static String ask(String question) {
        System.out.print(question);
        return sc.nextLine().trim();
    }

    // Asks for a number; NumberFormatException is caught in main
    static double askNumber(String question) {
        return Double.parseDouble(ask(question));
    }

    public static void main(String[] args) {
        Bank bank = new Bank();
        bank.loadFromFile();
        int choice = 0;

        while (choice != 8) {
            System.out.println("\n1 Create Account  2 Deposit  3 Withdraw  4 Transfer");
            System.out.println("5 Show Accounts  6 Show Transactions  7 Apply Interest  8 Exit");

            try {
                choice = (int) askNumber("Choice: ");

                if (choice == 1) {
                    String name = ask("Name: ");
                    int type = (int) askNumber("Type (1 Savings, 2 Current): ");
                    double bal = askNumber("Opening balance: ");
                    System.out.println("Created: " + bank.createAccount(type, name, bal));

                } else if (choice == 2) {
                    String acc = ask("Account number: ");
                    bank.deposit(acc, askNumber("Amount: "));
                    System.out.println("Deposit done.");

                } else if (choice == 3) {
                    String acc = ask("Account number: ");
                    bank.withdraw(acc, askNumber("Amount: "));
                    System.out.println("Withdraw done.");

                } else if (choice == 4) {
                    String from = ask("From account: ");
                    String to = ask("To account: ");
                    double amt = askNumber("Amount: ");
                    String note = ask("Note (press Enter to skip): ");
                    if (note.equals("")) bank.transfer(from, to, amt);
                    else bank.transfer(from, to, amt, note);
                    System.out.println("Transfer done.");

                } else if (choice == 5) {
                    bank.showAllAccounts();

                } else if (choice == 6) {
                    bank.showTransactions(ask("Account number: "));

                } else if (choice == 7) {
                    bank.applyInterestToAll();

                } else if (choice != 8) {
                    System.out.println("Please choose 1 to 8.");
                }

            } catch (InvalidAmountException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (InsufficientFundsException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (AccountNotFoundException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println("Error: please type a valid number.");
            }
        }
        System.out.println("Goodbye!");
    }
}
