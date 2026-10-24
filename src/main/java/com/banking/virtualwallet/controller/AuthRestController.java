package com.banking.virtualwallet.controller;

import com.banking.virtualwallet.dto.AuthDTOs.*;
import com.banking.virtualwallet.entity.User;
import com.banking.virtualwallet.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthRestController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthRestController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user, hashes password, creates master account with ₹10,000 and default General wallet.
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user, master account with ₹10,000 balance, and default General Wallet")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.registerUser(request);
        return ResponseEntity
                .ok(new AuthResponse("Registration successful", user.getUsername(), user.getRole(), user.getId()));
    }

    /**
     * Authenticates user credentials using BCrypt password comparison.
     */
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user credentials")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        UserDetails userDetails = userService.loadUserByUsername(request.getUsername());
        if (!passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        User user = userService.getByUsername(request.getUsername());
        return ResponseEntity
                .ok(new AuthResponse("Login successful", user.getUsername(), user.getRole(), user.getId()));
    }

    /**
     * Retrieves the profile information of the currently authenticated user.
     */
    @GetMapping("/me")
    @Operation(summary = "Current user profile", description = "Returns the profile of the authenticated user (full name, email, phone, UPI ID, account number).")
    public ResponseEntity<java.util.Map<String, Object>> me(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(userService.getProfile(userDetails.getUsername()));
    }

    /**
     * Resolves a payee's full name, UPI handle, and account number from a registered 10-digit mobile number.
     */
    @GetMapping("/lookup")
    @Operation(summary = "Look up a payee by mobile number", description = "Resolves a registered 10-digit mobile number to the recipient's full name, UPI ID and account number for the 'To Phone Number' transfer flow.")
    public ResponseEntity<java.util.Map<String, Object>> lookupByPhone(@RequestParam("phone") String phone) {
        return ResponseEntity.ok(userService.lookupByPhone(phone));
    }
}
