package com.dduk.domain.accounting.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByCode(String code);

    List<Account> findByTypeOrderByCodeAsc(String type);

    List<Account> findByIsActiveTrueOrderByCodeAsc();

    @Query("SELECT a FROM Account a WHERE a.isActive = true AND a.level >= 2 ORDER BY a.code ASC")
    List<Account> findAllLeafAccounts();
}
