package com.finvigil.transaction.repository;

import com.finvigil.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionUuid(String transactionUuid);

    List<Transaction> findByCustomer_CustomerUuidOrderByTransactionTimestampDesc(String customerUuid);

    List<Transaction> findByCustomer_IdOrderByTransactionTimestampDesc(Long customerId);

    long countByCustomer_IdAndTransactionTimestampAfter(Long customerId, OffsetDateTime since);

    long countByCustomer_CustomerUuidAndTransactionTimestampAfter(String customerUuid, OffsetDateTime since);
}
