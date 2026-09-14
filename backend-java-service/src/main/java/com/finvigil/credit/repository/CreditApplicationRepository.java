package com.finvigil.credit.repository;

import com.finvigil.credit.entity.CreditApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditApplicationRepository extends JpaRepository<CreditApplication, Long> {

    Optional<CreditApplication> findByApplicationUuid(String applicationUuid);

    List<CreditApplication> findByCustomer_CustomerUuidOrderByCreatedAtDesc(String customerUuid);

    List<CreditApplication> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);

    Optional<CreditApplication> findFirstByCustomer_CustomerUuidOrderByCreatedAtDesc(String customerUuid);

    Optional<CreditApplication> findFirstByCustomer_IdOrderByCreatedAtDesc(Long customerId);
}
