package com.finvigil;

import com.finvigil.common.enums.CustomerStatus;
import com.finvigil.common.enums.TransactionStatus;
import com.finvigil.common.enums.TransactionType;
import com.finvigil.customer.entity.Customer;
import com.finvigil.customer.repository.CustomerRepository;
import com.finvigil.customer.service.AuditLogService;
import com.finvigil.messaging.TransactionEventPublisher;
import com.finvigil.messaging.dto.TransactionEvent;
import com.finvigil.transaction.dto.TransactionCreateRequest;
import com.finvigil.transaction.dto.TransactionResponse;
import com.finvigil.transaction.dto.VelocityStats;
import com.finvigil.transaction.entity.Transaction;
import com.finvigil.transaction.repository.TransactionRepository;
import com.finvigil.transaction.service.RedisVelocityService;
import com.finvigil.transaction.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private RedisVelocityService redisVelocityService;

    @Mock
    private TransactionEventPublisher transactionEventPublisher;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TransactionService transactionService;

    private Customer sampleCustomer;

    @BeforeEach
    void setUp() {
        sampleCustomer = new Customer("John Doe", "john@example.com", "+123456789", "hash123");
        sampleCustomer.setCustomerUuid("cust-uuid-12345");
        sampleCustomer.setStatus(CustomerStatus.ACTIVE);
    }

    @Test
    void testProcessTransactionSuccess() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                "cust-uuid-12345",
                BigDecimal.valueOf(1500.50),
                TransactionType.TRANSFER,
                "ACME Corp",
                "USD",
                OffsetDateTime.now()
        );

        when(customerRepository.findByCustomerUuid("cust-uuid-12345")).thenReturn(Optional.of(sampleCustomer));
        when(redisVelocityService.recordAndGetVelocity(eq("cust-uuid-12345"), eq(BigDecimal.valueOf(1500.50))))
                .thenReturn(new VelocityStats(3, BigDecimal.valueOf(3200.00)));

        Transaction savedTxn = new Transaction(
                sampleCustomer,
                request.getAmount(),
                request.getTransactionType(),
                request.getMerchant(),
                request.getCurrency(),
                request.getTransactionTimestamp()
        );
        savedTxn.setTransactionUuid("txn-uuid-999");
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTxn);

        TransactionResponse response = transactionService.processTransaction(request);

        assertNotNull(response);
        assertEquals("txn-uuid-999", response.getTransactionUuid());
        assertEquals("cust-uuid-12345", response.getCustomerUuid());
        assertEquals(BigDecimal.valueOf(1500.50), response.getAmount());
        assertEquals(TransactionType.TRANSFER, response.getTransactionType());
        assertEquals("ACME Corp", response.getMerchant());
        assertEquals(Integer.valueOf(3), response.getVelocityCount());
        assertEquals(BigDecimal.valueOf(3200.00), response.getVelocityAmount());

        // Verify messaging event publication
        ArgumentCaptor<TransactionEvent> eventCaptor = ArgumentCaptor.forClass(TransactionEvent.class);
        verify(transactionEventPublisher, times(1)).publishTransactionEvent(eventCaptor.capture());
        TransactionEvent capturedEvent = eventCaptor.getValue();
        assertEquals("txn-uuid-999", capturedEvent.getTransactionUuid());
        assertEquals("cust-uuid-12345", capturedEvent.getCustomerUuid());
        assertEquals(BigDecimal.valueOf(1500.50), capturedEvent.getAmount());
        assertEquals(Integer.valueOf(3), capturedEvent.getVelocityCount1m());

        // Verify audit logging
        verify(auditLogService, times(1)).logEvent(eq("TRANSACTION"), eq("TRANSACTION"), eq("txn-uuid-999"), anyString());
    }

    @Test
    void testGetTransactionsByCustomer() {
        when(customerRepository.findByCustomerUuid("cust-uuid-12345")).thenReturn(Optional.of(sampleCustomer));
        when(redisVelocityService.getCurrentVelocity("cust-uuid-12345")).thenReturn(new VelocityStats(1, BigDecimal.valueOf(100.00)));

        Transaction txn = new Transaction(
                sampleCustomer,
                BigDecimal.valueOf(100.00),
                TransactionType.PURCHASE,
                "Amazon",
                "USD",
                OffsetDateTime.now()
        );
        txn.setTransactionUuid("txn-1");

        when(transactionRepository.findByCustomer_CustomerUuidOrderByTransactionTimestampDesc("cust-uuid-12345"))
                .thenReturn(List.of(txn));

        List<TransactionResponse> results = transactionService.getTransactionsByCustomerIdOrUuid("cust-uuid-12345");

        assertEquals(1, results.size());
        assertEquals("txn-1", results.get(0).getTransactionUuid());
        assertEquals(Integer.valueOf(1), results.get(0).getVelocityCount());
    }
}
