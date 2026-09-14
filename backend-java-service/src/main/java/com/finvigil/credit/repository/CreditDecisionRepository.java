package com.finvigil.credit.repository;

import com.finvigil.credit.entity.CreditDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreditDecisionRepository extends JpaRepository<CreditDecision, Long> {

    Optional<CreditDecision> findByDecisionUuid(String decisionUuid);

    Optional<CreditDecision> findByApplication_ApplicationUuid(String applicationUuid);

    Optional<CreditDecision> findByApplication_Id(Long applicationId);

    Optional<CreditDecision> findFirstByApplication_Customer_CustomerUuidOrderByCreatedAtDesc(String customerUuid);
}
