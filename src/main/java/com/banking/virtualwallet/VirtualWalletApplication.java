package com.banking.virtualwallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for BMS (Banking Management System).
 * Boots the unified Spring Boot service hosting both REST APIs and the static Next.js dashboard on port 3000.
 */
@SpringBootApplication
public class VirtualWalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(VirtualWalletApplication.class, args);
    }
}
