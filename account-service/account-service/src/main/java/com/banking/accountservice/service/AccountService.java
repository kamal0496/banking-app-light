package com.banking.accountservice.service;

import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.entity.AccountStatus;
import com.banking.accountservice.entity.AccountType;
import com.banking.accountservice.repository.AccountRepository;
import jakarta.validation.Valid;
import jdk.jfr.Registered;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository repo;
    private static SecureRandom secureRandom = new SecureRandom();

    public AccountResponse createAccount(@Valid CreateAccountRequest request) {
        log.info("Creating Account for : {}", request.getEmail());

        if(repo.existsByEmail(request.getEmail())){
            throw new RuntimeException("Account already exists for: " + request.getEmail());
        }

        Account account = Account.builder()
                .accountHolderName(request.getAccountHolderName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .balance(request.getInitialDeposit())
                .accountStatus(AccountStatus.ACTIVE)
                .accountType(request.getAccountType())
                .accountNumber(generateAccountNumber())
                .dailyTransactionLimit(
                        request.getAccountType() == AccountType.SAVING ? new BigDecimal(50000) : new BigDecimal(80000)
                ).build();

        repo.save(account);
        log.info("Account Created for: {}", account.getAccountNumber());
        return mapToAccountResponse(account);
    }

    private AccountResponse mapToAccountResponse(Account account) {

        AccountResponse response = AccountResponse.builder()
                .accountNumber(account.getAccountNumber())
                .phone(account.getPhone())
                .email(account.getEmail())
                .accountHolderName(account.getAccountHolderName())
                .id(account.getId())
                .accountType(account.getAccountType())
                .accountStatus(account.getAccountStatus())
                .balance(account.getBalance())
                .dailyTransactionLimit(account.getDailyTransactionLimit())
                .createdAt(account.getCreatedAt())
                .build();
        return response;

    }

    private String generateAccountNumber() {

        long newAccountNumber;

        do{
            newAccountNumber = secureRandom.nextLong(1000000000000L);
        }while(repo.existsByAccountNumber(newAccountNumber));

        return String.format("%012d", newAccountNumber);

    }

    public AccountResponse getAccount(String accountNumber) {
        log.info("Get Account: {}", accountNumber);
        Account account = repo.findByAccountNumber(accountNumber).orElseThrow(() -> new RuntimeException("Account not found for: " + accountNumber));

        log.info("Get Account success for: {}", accountNumber);
        return mapToAccountResponse(account);
    }

    public @Nullable BigDecimal getBalance(String accountNumber) {
        log.info("Fetching Account Balance for: " + accountNumber);
        Account account = repo.findByAccountNumber(accountNumber).orElseThrow(() -> new RuntimeException("Account not found for: " + accountNumber));
        log.info("Successfully Account Balance for: " + accountNumber);
        return account.getBalance();
    }

    public void blockAccount(String accountNumber) {
        log.info("Blocking Account for: {}", accountNumber);
        Account account = repo.findByAccountNumber(accountNumber).orElseThrow(() -> new RuntimeException("Account not found for: " + accountNumber));
        account.setAccountStatus(AccountStatus.BLOCKED);
        repo.save(account);
        log.info("Successfully account blocked for: {}", accountNumber);
    }

    public void deductBalance(String accountNumber, BigDecimal amount){
        log.info("Deducting amount {} from account number: {}", accountNumber, amount);
        Account account = repo.findByAccountNumber(accountNumber).orElseThrow(() -> new RuntimeException("Account not found for: " + accountNumber));

        if(account.getAccountStatus() != AccountStatus.ACTIVE){
            throw new RuntimeException("Account is not active: "+accountNumber);
        }

        if(account.getBalance().compareTo(amount) < 0){
            throw new RuntimeException("Insufficient funds for account"+ accountNumber);
        }
        account.setBalance(
                account.getBalance().subtract(amount)
        );
        repo.save(account);
        log.info("Amount {} deducted successfully from account : {}", amount, account);
    }

    public void creditBalance(String accountNumber, BigDecimal amount){
        log.info("Credit amount {} from account number: {}", accountNumber, amount);
        Account account = repo.findByAccountNumber(accountNumber).orElseThrow(() -> new RuntimeException("Account not found for: " + accountNumber));
        account.setBalance(
                account.getBalance().add(amount)
        );
        repo.save(account);
        log.info("Amount {} cretied successfully to account : {}", amount, account);
    }

    public @Nullable List<AccountResponse> getAllAccounts() {
        return repo.findAllByOrderByAccountHolderNameAsc().stream().map(this::mapToAccountResponse).collect(java.util.stream.Collectors.toList());
    }

    public void unblockAccount(String accountNumber) {
        log.info("unblock Account for: {}", accountNumber);
        Account account = repo.findByAccountNumber(accountNumber).orElseThrow(() -> new RuntimeException("Account not found for: " + accountNumber));
        account.setAccountStatus(AccountStatus.ACTIVE);
        repo.save(account);
        log.info("successfully account unblocked for: {}", accountNumber);
    }
}
