package com.banking.virtualwallet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Objects for authentication and user registration.
 */
public class AuthDTOs {

    /** Request payload for authenticating existing users. */
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Password is required")
        private String password;

        public LoginRequest() {}
        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /** Request payload for self-registering a new bank user. */
    public static class RegisterRequest {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50)
        private String username;

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100)
        private String password;

        @NotBlank(message = "Full name is required")
        private String fullName;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;

        /** Optional mobile number (10 digits, starts 6-9) used for payee lookup. */
        @jakarta.validation.constraints.Pattern(
            regexp = "^$|^[6-9][0-9]{9}$",
            message = "Phone must be a valid 10-digit Indian mobile number")
        private String phone;

        public RegisterRequest() {}

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    /** Response payload returning authentication status and user details. */
    public static class AuthResponse {
        private String message;
        private String username;
        private String role;
        private Long userId;

        public AuthResponse(String message, String username, String role, Long userId) {
            this.message = message;
            this.username = username;
            this.role = role;
            this.userId = userId;
        }

        public String getMessage() { return message; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
        public Long getUserId() { return userId; }
    }
}
