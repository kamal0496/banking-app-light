package com.banking.accountservice.repository;

import com.banking.accountservice.entity.Account;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistentAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    boolean existsByEmail(String email);

    boolean existsByAccountNumber(long newAccountNumber);

    Optional<Account> findByAccountNumber(String accountNumber);

    /*@Query("SELECT a.accountNumber FROM Account a")
    List<String> findAllAccountNumbers();*/

    List<Account> findAllByOrderByAccountHolderNameAsc();
}
