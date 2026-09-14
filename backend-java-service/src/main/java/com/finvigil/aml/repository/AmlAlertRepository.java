package com.finvigil.aml.repository;

import com.finvigil.aml.entity.AmlAlert;
import com.finvigil.common.enums.AlertStatus;
import com.finvigil.common.enums.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmlAlertRepository extends JpaRepository<AmlAlert, Long> {

    Optional<AmlAlert> findByAlertUuid(String alertUuid);

    List<AmlAlert> findByCustomer_CustomerUuidOrderByCreatedAtDesc(String customerUuid);

    List<AmlAlert> findByStatusOrderByCreatedAtDesc(AlertStatus status);

    List<AmlAlert> findByRiskLevelOrderByCreatedAtDesc(RiskLevel riskLevel);

    List<AmlAlert> findAllByOrderByCreatedAtDesc();

    long countByCustomer_CustomerUuidAndStatus(String customerUuid, AlertStatus status);

    Optional<AmlAlert> findFirstByCustomer_CustomerUuidOrderByCreatedAtDesc(String customerUuid);
}
