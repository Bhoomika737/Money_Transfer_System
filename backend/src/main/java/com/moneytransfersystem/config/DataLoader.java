package com.moneytransfersystem.config;

import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.enums.AccountStatus;
import com.moneytransfersystem.repository.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class DataLoader implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (accountRepository.count() == 0) {

            Account alice = Account.builder()
                    .id("ACC001")
                    .holderName("Alice")
                    .balance(new BigDecimal("5000.00"))
                    .status(AccountStatus.ACTIVE)
                    .lastUpdated(Instant.now())
                    .version(0L)
                    .password(passwordEncoder.encode("Alice@123"))
                    .build();

            Account bob = Account.builder()
                    .id("ACC002")
                    .holderName("Bob")
                    .balance(new BigDecimal("1000.00"))
                    .status(AccountStatus.ACTIVE)
                    .lastUpdated(Instant.now())
                    .version(0L)
                    .password(passwordEncoder.encode("Bob@123"))
                    .build();

            Account charlie = Account.builder()
                    .id("ACC003")
                    .holderName("Charlie")
                    .balance(new BigDecimal("10.00"))
                    .status(AccountStatus.ACTIVE)
                    .lastUpdated(Instant.now())
                    .version(0L)
                    .password(passwordEncoder.encode("Charlie@123"))
                    .build();

            Account diana = Account.builder()
                    .id("ACC004")
                    .holderName("Diana")
                    .balance(BigDecimal.ZERO)
                    .status(AccountStatus.CLOSED)
                    .lastUpdated(Instant.now())
                    .version(0L)
                    .password(passwordEncoder.encode("Diana@123"))
                    .build();

            Account eve = Account.builder()
                    .id("ACC005")
                    .holderName("Eve")
                    .balance(new BigDecimal("1000000.00"))
                    .status(AccountStatus.ACTIVE)
                    .lastUpdated(Instant.now())
                    .version(0L)
                    .password(passwordEncoder.encode("Eve@123"))
                    .build();

            accountRepository.save(alice);
            accountRepository.save(bob);
            accountRepository.save(charlie);
            accountRepository.save(diana);
            accountRepository.save(eve);

        }
    }
}
