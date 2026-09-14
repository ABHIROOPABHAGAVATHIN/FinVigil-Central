package com.finvigil.transaction.service;

import com.finvigil.common.enums.AuditAction;
import com.finvigil.common.enums.TransactionStatus;
import com.finvigil.customer.entity.Customer;
import com.finvigil.customer.repository.CustomerRepository;
import com.finvigil.customer.service.AuditLogService;
import com.finvigil.exception.ResourceNotFoundException;
import com.finvigil.messaging.TransactionEventPublisher;
import com.finvigil.messaging.dto.TransactionEvent;
import com.finvigil.transaction.dto.TransactionCreateRequest;
import com.finvigil.transaction.dto.TransactionResponse;
import com.finvigil.transaction.dto.VelocityStats;
import com.finvigil.transaction.entity.Transaction;
import com.finvigil.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final RedisVelocityService redisVelocityService;
    private final TransactionEventPublisher transactionEventPublisher;
    private final AuditLogService auditLogService;

    public TransactionService(TransactionRepository transactionRepository,
                              CustomerRepository customerRepository,
                              RedisVelocityService redisVelocityService,
                              TransactionEventPublisher transactionEventPublisher,
                              AuditLogService auditLogService) {
        this.transactionRepository = transactionRepository;
        this.customerRepository = customerRepository;
        this.redisVelocityService = redisVelocityService;
        this.transactionEventPublisher = transactionEventPublisher;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public TransactionResponse processTransaction(TransactionCreateRequest request) {
        Customer customer = findCustomerByIdOrUuid(request.getCustomerId());

        // 1. Calculate & record real-time velocity in Redis sliding window
        VelocityStats velocityStats = redisVelocityService.recordAndGetVelocity(
                customer.getCustomerUuid(),
                request.getAmount()
        );

        // 2. Persist transaction in database
        Transaction transaction = new Transaction(
                customer,
                request.getAmount(),
                request.getTransactionType(),
                request.getMerchant(),
                request.getCurrency(),
                request.getTransactionTimestamp() != null ? request.getTransactionTimestamp() : OffsetDateTime.now()
        );
        Transaction savedTransaction = transactionRepository.save(transaction);

        // 3. Publish real-time transaction event for AML monitoring
        TransactionEvent event = new TransactionEvent(
                savedTransaction.getTransactionUuid(),
                customer.getCustomerUuid(),
                savedTransaction.getAmount(),
                savedTransaction.getTransactionType(),
                savedTransaction.getMerchant(),
                savedTransaction.getTransactionTimestamp(),
                savedTransaction.getCurrency(),
                velocityStats.getCount(),
                velocityStats.getTotalAmount()
        );
        transactionEventPublisher.publishTransactionEvent(event);

        // 4. Record audit log
        auditLogService.logEvent(
                "TRANSACTION",
                "TRANSACTION",
                savedTransaction.getTransactionUuid(),
                AuditAction.TRANSACTION_COMPLETED.name()
        );

        return mapToResponse(savedTransaction, velocityStats);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByIdOrUuid(String idOrUuid) {
        Transaction transaction = findTransactionByIdOrUuid(idOrUuid);
        VelocityStats stats = redisVelocityService.getCurrentVelocity(transaction.getCustomer().getCustomerUuid());
        return mapToResponse(transaction, stats);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByCustomerIdOrUuid(String customerIdOrUuid) {
        Customer customer = findCustomerByIdOrUuid(customerIdOrUuid);
        List<Transaction> transactions = transactionRepository
                .findByCustomer_CustomerUuidOrderByTransactionTimestampDesc(customer.getCustomerUuid());

        VelocityStats currentStats = redisVelocityService.getCurrentVelocity(customer.getCustomerUuid());

        return transactions.stream()
                .map(txn -> mapToResponse(txn, currentStats))
                .collect(Collectors.toList());
    }

    private Customer findCustomerByIdOrUuid(String customerIdOrUuid) {
        if (customerIdOrUuid.matches("^\\d+$")) {
            Long id = Long.parseLong(customerIdOrUuid);
            return customerRepository.findById(id)
                    .orElseGet(() -> customerRepository.findByCustomerUuid(customerIdOrUuid)
                            .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerIdOrUuid)));
        } else {
            return customerRepository.findByCustomerUuid(customerIdOrUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", "customerUuid", customerIdOrUuid));
        }
    }

    private Transaction findTransactionByIdOrUuid(String idOrUuid) {
        if (idOrUuid.matches("^\\d+$")) {
            Long id = Long.parseLong(idOrUuid);
            return transactionRepository.findById(id)
                    .orElseGet(() -> transactionRepository.findByTransactionUuid(idOrUuid)
                            .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", idOrUuid)));
        } else {
            return transactionRepository.findByTransactionUuid(idOrUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction", "transactionUuid", idOrUuid));
        }
    }

    public TransactionResponse mapToResponse(Transaction txn, VelocityStats velocityStats) {
        return new TransactionResponse(
                txn.getTransactionUuid(),
                txn.getCustomer().getCustomerUuid(),
                txn.getAmount(),
                txn.getTransactionType(),
                txn.getMerchant(),
                txn.getTransactionTimestamp(),
                txn.getCurrency(),
                txn.getStatus(),
                velocityStats != null ? velocityStats.getCount() : null,
                velocityStats != null ? velocityStats.getTotalAmount() : null,
                txn.getCreatedAt()
        );
    }
}
