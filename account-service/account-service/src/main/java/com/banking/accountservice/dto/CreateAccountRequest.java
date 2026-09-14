package com.banking.accountservice.dto;

import com.banking.accountservice.entity.AccountType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateAccountRequest {

    private String accountHolderName;
    private String email;
    private String phone;
    private AccountType accountType;
    private BigDecimal initialDeposit;

}
