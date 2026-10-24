package com.banking.virtualwallet.service;

import com.banking.virtualwallet.dto.AuthDTOs.RegisterRequest;
import com.banking.virtualwallet.entity.Account;
import com.banking.virtualwallet.entity.User;
import com.banking.virtualwallet.entity.VirtualWallet;
import com.banking.virtualwallet.repository.AccountRepository;
import com.banking.virtualwallet.repository.UserRepository;
import com.banking.virtualwallet.repository.VirtualWalletRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;

/**
 * Service managing user identity, Spring Security user details loading,
 * account registration, profile retrieval, and mobile-number payee lookup.
 */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final VirtualWalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
            AccountRepository accountRepository,
            VirtualWalletRepository walletRepository,
            @Lazy PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Spring Security UserDetailsService implementation to load user credentials for HTTP Basic auth.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole())));
    }

    /**
     * Registers a new user account, hashes password with BCrypt, provisions a master Account
     * with ₹10,000 starting balance, initializes the default General Wallet, and assigns a unique UPI ID.
     */
    @Transactional
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getEmail(),
                "ROLE_USER");
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            String phone = request.getPhone().trim();
            if (userRepository.existsByPhone(phone)) {
                throw new IllegalArgumentException("This phone number is already linked to another account");
            }
            user.setPhone(phone);
        }
        // Every account gets a UPI handle derived from its unique username.
        String baseHandle = request.getUsername().trim().toLowerCase().replaceAll("[^a-z0-9._-]", "");
        user.setUpiId(baseHandle + "@okbms");
        user = userRepository.save(user);

        // Create main Account with initial balance of ₹10,000
        BigDecimal initialBalance = new BigDecimal("10000.00");
        String accountNumber = "ACC-" + (100000 + (int) (Math.random() * 900000));
        Account account = new Account(accountNumber, initialBalance, user);
        account = accountRepository.save(account);

        // Create default General Wallet and assign initialBalance to maintain balance
        // invariant
        VirtualWallet generalWallet = new VirtualWallet(
                "General",
                "General Purpose",
                initialBalance,
                true,
                account);
        walletRepository.save(generalWallet);

        return user;
    }

    /**
     * Fetches a User entity by unique username.
     */
    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    /**
     * Returns the full profile of the signed-in user. Runs inside a
     * transaction so the lazy account relation is available.
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getProfile(String username) {
        User user = getByUsername(username);
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("username", user.getUsername());
        result.put("fullName", user.getFullName());
        result.put("email", user.getEmail());
        result.put("phone", user.getPhone());
        result.put("upiId", user.getUpiId());
        result.put("role", user.getRole());
        result.put("accountNumber", user.getAccount() != null ? user.getAccount().getAccountNumber() : null);
        return result;
    }

    /**
     * Resolves a registered mobile number to the payee profile used by the
     * "To Phone Number" transfer flow. Returns a map so lazy relations are
     * initialised inside the transaction.
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> lookupByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return java.util.Map.of("found", false);
        }
        return userRepository.findByPhone(phone.trim()).<java.util.Map<String, Object>>map(u -> {
            java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("found", true);
            result.put("fullName", u.getFullName());
            result.put("username", u.getUsername());
            result.put("upiId", u.getUpiId());
            result.put("accountNumber", u.getAccount() != null ? u.getAccount().getAccountNumber() : null);
            return result;
        })
                .orElse(java.util.Map.of("found", false));
    }
}
