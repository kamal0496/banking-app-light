package com.banking.accountservice.controller;

import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@Slf4j
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody  CreateAccountRequest request){

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(request));
    }

    @GetMapping("{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber){

        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    @GetMapping("{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String accountNumber){

        return ResponseEntity.ok(accountService.getBalance(accountNumber));
    }

    @PutMapping("{accountNumber}/block")
    public ResponseEntity<String> blockAccount(@PathVariable String accountNumber){

        accountService.blockAccount(accountNumber);
        return ResponseEntity.ok("Account blocked");
    }

    @PutMapping("{accountNumber}/unblock")
    public ResponseEntity<String> unblockAccount(@PathVariable String accountNumber){

        accountService.unblockAccount(accountNumber);
        return ResponseEntity.ok("Account unblocked successfully");
    }

    @PutMapping("{accountNumber}/deduct")
    public ResponseEntity<String> deductBalance(@PathVariable String accountNumber, @RequestParam BigDecimal amount){

        accountService.deductBalance(accountNumber, amount);
        return ResponseEntity.ok("Balance Deducted Successfully");
    }

    @PutMapping("{accountNumber}/credit")
    public ResponseEntity<String> creditBalance(@PathVariable String accountNumber, @RequestParam BigDecimal amount){

        accountService.creditBalance(accountNumber, amount);
        return ResponseEntity.ok("Balance Credited Successfully");
    }
}
