package com.banking.virtualwallet.config;

import com.banking.virtualwallet.entity.Account;
import com.banking.virtualwallet.entity.Transaction;
import com.banking.virtualwallet.entity.User;
import com.banking.virtualwallet.entity.VirtualWallet;
import com.banking.virtualwallet.repository.AccountRepository;
import com.banking.virtualwallet.repository.TransactionRepository;
import com.banking.virtualwallet.repository.UserRepository;
import com.banking.virtualwallet.repository.VirtualWalletRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final VirtualWalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           AccountRepository accountRepository,
                           VirtualWalletRepository walletRepository,
                           TransactionRepository transactionRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 1. Wipe previous testing data to refresh the database with clean, pristine state
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        // 2. Seed primary user (Karthik) with balanced wallets and meaningful statistical data
        initKarthik();

        // 3. Seed demo payees under single bank ("BMS Bank") with unique mobile numbers and @okbms handles
        initDemoUser("priya", "Priya Krishnan", "priya@banking.com", "9876504321", "priya@okbms", "BMS0002");
        initDemoUser("ananya", "Ananya Iyer", "ananya@banking.com", "9812345670", "ananya@okbms", "BMS0003");
        initDemoUser("rohan", "Rohan Verma", "rohan@banking.com", "9812345671", "rohan@okbms", "BMS0004");
        initDemoUser("meera", "Meera Nair", "meera@banking.com", "9812345672", "meera@okbms", "BMS0005");
        initDemoUser("arjun", "Arjun Mehta", "arjun@banking.com", "9812345673", "arjun@okbms", "BMS0006");
        initDemoUser("sneha", "Sneha Gupta", "sneha@banking.com", "9812345674", "sneha@okbms", "BMS0007");
    }

    private void initKarthik() {
        User user = new User(
                "karthik",
                passwordEncoder.encode("password123"),
                "R.V.S.V. Karthik",
                "karthik@banking.com",
                "ROLE_USER"
        );
        user.setPhone("9876543210");
        user.setUpiId("karthik@okbms");
        user = userRepository.save(user);

        BigDecimal totalAccountBalance = new BigDecimal("10000.00");
        Account account = new Account("ACC-100234", totalAccountBalance, user);
        account = accountRepository.save(account);

        VirtualWallet generalWallet = new VirtualWallet("General", "General Purpose", new BigDecimal("2500.00"), true, account);
        VirtualWallet savingsWallet = new VirtualWallet("Savings", "Emergency & Savings", new BigDecimal("2500.00"), false, account);
        VirtualWallet foodWallet = new VirtualWallet("Food", "Groceries & Dining", new BigDecimal("2000.00"), false, account);
        VirtualWallet travelWallet = new VirtualWallet("Travel", "Commute & Transit", new BigDecimal("1500.00"), false, account);
        VirtualWallet shoppingWallet = new VirtualWallet("Shopping", "Lifestyle & Retail", new BigDecimal("1500.00"), false, account);

        generalWallet = walletRepository.save(generalWallet);
        savingsWallet = walletRepository.save(savingsWallet);
        foodWallet = walletRepository.save(foodWallet);
        travelWallet = walletRepository.save(travelWallet);
        shoppingWallet = walletRepository.save(shoppingWallet);

        seedHistoricalTransactions(account, generalWallet, foodWallet, travelWallet, shoppingWallet);
    }

    private void initDemoUser(String username, String fullName, String email, String phone, String upiId, String accountNumber) {
        User user = new User(
                username,
                passwordEncoder.encode("password123"),
                fullName,
                email,
                "ROLE_USER"
        );
        user.setPhone(phone);
        user.setUpiId(upiId);
        user = userRepository.save(user);

        BigDecimal totalAccountBalance = new BigDecimal("10000.00");
        Account account = new Account(accountNumber, totalAccountBalance, user);
        account = accountRepository.save(account);

        VirtualWallet generalWallet = new VirtualWallet("General", "General Purpose", totalAccountBalance, true, account);
        walletRepository.save(generalWallet);
    }

    private void seedHistoricalTransactions(Account account, VirtualWallet generalWallet, VirtualWallet foodWallet, VirtualWallet travelWallet, VirtualWallet shoppingWallet) {
        LocalDateTime baseTime = LocalDateTime.now().minusDays(15);

        // 1. Inflows / Credits (for realistic dashboard statistics)
        Transaction salaryTx = new Transaction();
        salaryTx.setReferenceNumber("DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        salaryTx.setAmount(new BigDecimal("5000.00"));
        salaryTx.setDescription("Monthly Stipend & Salary Deposit");
        salaryTx.setTimestamp(baseTime.plusHours(6));
        salaryTx.setPrimaryWallet(generalWallet);
        salaryTx.setPrimaryAmountDeducted(BigDecimal.ZERO);
        salaryTx.setStatus("COMPLETED");
        salaryTx.setType("DEPOSIT");
        salaryTx.setDirection("CREDIT");
        salaryTx.setAccount(account);
        transactionRepository.save(salaryTx);

        Transaction priyaCredit = new Transaction();
        priyaCredit.setReferenceNumber("TRF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        priyaCredit.setAmount(new BigDecimal("1000.00"));
        priyaCredit.setDescription("Received from Priya Krishnan (Commute Reimbursement)");
        priyaCredit.setTimestamp(baseTime.plusHours(120));
        priyaCredit.setPrimaryWallet(generalWallet);
        priyaCredit.setPrimaryAmountDeducted(BigDecimal.ZERO);
        priyaCredit.setStatus("COMPLETED");
        priyaCredit.setType("RECEIVED");
        priyaCredit.setDirection("CREDIT");
        priyaCredit.setAccount(account);
        transactionRepository.save(priyaCredit);

        Transaction ananyaCredit = new Transaction();
        ananyaCredit.setReferenceNumber("TRF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        ananyaCredit.setAmount(new BigDecimal("500.00"));
        ananyaCredit.setDescription("Received from Ananya Iyer (Lunch Split)");
        ananyaCredit.setTimestamp(baseTime.plusHours(260));
        ananyaCredit.setPrimaryWallet(generalWallet);
        ananyaCredit.setPrimaryAmountDeducted(BigDecimal.ZERO);
        ananyaCredit.setStatus("COMPLETED");
        ananyaCredit.setType("RECEIVED");
        ananyaCredit.setDirection("CREDIT");
        ananyaCredit.setAccount(account);
        transactionRepository.save(ananyaCredit);

        // 2. Seed Food Wallet Transactions (19 entries matching the P&S Analytics slides):
        // 5 in [100, 200), 8 in [200, 300), 4 in [300, 400), 2 in [400, 500)
        double[] foodAmounts = {
            120.0, 145.0, 160.0, 180.0, 195.0,                   // 5 in [100, 200)
            210.0, 220.0, 235.0, 250.0, 260.0, 275.0, 280.0, 290.0, // 8 in [200, 300)
            310.0, 330.0, 350.0, 385.0,                          // 4 in [300, 400)
            420.0, 480.0                                         // 2 in [400, 500)
        };

        String[] foodDescriptions = {
            "Morning Coffee & Snacks", "Campus Canteen Lunch", "Juice Corner", "Evening Bakery", "Subway Sandwich",
            "Cafeteria Special Meal", "Dosa Corner", "Dinner with Classmates", "Pizza Slice Combo", "Biryani Plate",
            "Healthy Salad Bowl", "Ice Cream & Waffles", "Chinese Takeout", "Cafe Latte & Brownie", "South Indian Thali",
            "Weekend Brunch", "Barbeque Roll", "Gourmet Burger Combo", "Dinner Buffet"
        };

        for (int i = 0; i < foodAmounts.length; i++) {
            Transaction tx = new Transaction();
            tx.setReferenceNumber("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            tx.setAmount(BigDecimal.valueOf(foodAmounts[i]));
            tx.setDescription(foodDescriptions[i]);
            tx.setTimestamp(baseTime.plusHours(12 + i * 16));
            tx.setPrimaryWallet(foodWallet);
            tx.setPrimaryAmountDeducted(BigDecimal.valueOf(foodAmounts[i]));
            tx.setStatus("COMPLETED");
            tx.setType("PAYMENT");
            tx.setDirection("DEBIT");
            tx.setAccount(account);
            transactionRepository.save(tx);
        }

        // 3. Seed Travel Transactions
        double[] travelAmounts = { 80.0, 150.0, 220.0, 350.0, 450.0, 120.0 };
        String[] travelDescriptions = {
            "Metro Rapid Transit Ticket", "Bus Pass Monthly Renewal", "Auto Commute to Campus",
            "Uber Ride to Railway Station", "Airport Shuttle Transit", "Metro SmartCard Top-up"
        };
        for (int i = 0; i < travelAmounts.length; i++) {
            Transaction tx = new Transaction();
            tx.setReferenceNumber("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            tx.setAmount(BigDecimal.valueOf(travelAmounts[i]));
            tx.setDescription(travelDescriptions[i]);
            tx.setTimestamp(baseTime.plusHours(24 + i * 36));
            tx.setPrimaryWallet(travelWallet);
            tx.setPrimaryAmountDeducted(BigDecimal.valueOf(travelAmounts[i]));
            tx.setStatus("COMPLETED");
            tx.setType("PAYMENT");
            tx.setDirection("DEBIT");
            tx.setAccount(account);
            transactionRepository.save(tx);
        }

        // 4. Seed Shopping Transactions
        double[] shopAmounts = { 300.0, 450.0, 850.0 };
        String[] shopDescriptions = {
            "Electronics & USB Accessories", "Technical Books & Stationery", "Casual Apparel & Footwear"
        };
        for (int i = 0; i < shopAmounts.length; i++) {
            Transaction tx = new Transaction();
            tx.setReferenceNumber("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            tx.setAmount(BigDecimal.valueOf(shopAmounts[i]));
            tx.setDescription(shopDescriptions[i]);
            tx.setTimestamp(baseTime.plusHours(40 + i * 48));
            tx.setPrimaryWallet(shoppingWallet);
            tx.setPrimaryAmountDeducted(BigDecimal.valueOf(shopAmounts[i]));
            tx.setStatus("COMPLETED");
            tx.setType("PAYMENT");
            tx.setDirection("DEBIT");
            tx.setAccount(account);
            transactionRepository.save(tx);
        }

        // 5. Seed Split Overflow Demonstration Transaction
        Transaction splitTx = new Transaction();
        splitTx.setReferenceNumber("TXN-OVERFLOW1");
        splitTx.setAmount(new BigDecimal("700.00"));
        splitTx.setDescription("Weekend Dinner Feast (Split Overflow Payment)");
        splitTx.setTimestamp(LocalDateTime.now().minusHours(3));
        splitTx.setPrimaryWallet(foodWallet);
        splitTx.setPrimaryAmountDeducted(new BigDecimal("500.00"));
        splitTx.setOverflowWallet(generalWallet);
        splitTx.setOverflowAmountDeducted(new BigDecimal("200.00"));
        splitTx.setStatus("COMPLETED");
        splitTx.setType("PAYMENT");
        splitTx.setDirection("DEBIT");
        splitTx.setAccount(account);
        transactionRepository.save(splitTx);
    }
}
