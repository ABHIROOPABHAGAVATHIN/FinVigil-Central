package com.finvigil;

import com.finvigil.aml.entity.AmlAlert;
import com.finvigil.aml.service.AmlAlertService;
import com.finvigil.common.enums.AlertStatus;
import com.finvigil.common.enums.RiskLevel;
import com.finvigil.customer.entity.Customer;
import com.finvigil.messaging.AmlEventConsumer;
import com.finvigil.messaging.dto.AmlAlertEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AmlMessagingTest {

    @Mock
    private AmlAlertService amlAlertService;

    @InjectMocks
    private AmlEventConsumer amlEventConsumer;

    @Test
    void testConsumeAmlAlertDelegatesToService() {
        AmlAlertEvent event = new AmlAlertEvent(
                "alert-msg-test-1",
                "txn-msg-test-1",
                "cust-msg-test-1",
                0.35,
                0.80,
                0.62,
                RiskLevel.HIGH,
                AlertStatus.OPEN,
                List.of("LARGE_TRANSACTION_THRESHOLD"),
                "2026-09-14T19:00:00Z"
        );

        Customer mockCustomer = new Customer();
        mockCustomer.setCustomerUuid("cust-msg-test-1");

        AmlAlert mockAlert = new AmlAlert(
                mockCustomer,
                "txn-msg-test-1",
                0.35, 0.80, 0.62,
                RiskLevel.HIGH,
                AlertStatus.OPEN,
                List.of("LARGE_TRANSACTION_THRESHOLD")
        );

        when(amlAlertService.processAlertEvent(any(AmlAlertEvent.class))).thenReturn(mockAlert);

        amlEventConsumer.consumeAmlAlert(event);

        verify(amlAlertService, times(1)).processAlertEvent(event);
    }
}
