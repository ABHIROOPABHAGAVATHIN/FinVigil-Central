package com.finvigil.auth.service;

import com.finvigil.auth.dto.AuthResponse;
import com.finvigil.auth.dto.LoginRequest;
import com.finvigil.auth.dto.RegisterRequest;
import com.finvigil.common.enums.AuditAction;
import com.finvigil.customer.entity.Customer;
import com.finvigil.customer.repository.CustomerRepository;
import com.finvigil.customer.service.AuditLogService;
import com.finvigil.exception.AppException;
import com.finvigil.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    public AuthService(CustomerRepository customerRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuditLogService auditLogService) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (customerRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new AppException("Email already registered: " + request.getEmail(),
                    HttpStatus.CONFLICT, "DUPLICATE_EMAIL");
        }

        Customer customer = new Customer();
        customer.setName(request.getName().trim());
        customer.setEmail(request.getEmail().toLowerCase().trim());
        customer.setPhone(request.getPhone().trim());
        customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        Customer savedCustomer = customerRepository.save(customer);

        auditLogService.logEvent(
                "AUTH",
                "CUSTOMER",
                savedCustomer.getCustomerUuid(),
                AuditAction.CUSTOMER_REGISTERED.name()
        );

        String token = jwtService.generateToken(savedCustomer.getEmail(), savedCustomer.getCustomerUuid());
        return new AuthResponse(
                token,
                savedCustomer.getCustomerUuid(),
                savedCustomer.getEmail(),
                savedCustomer.getName(),
                jwtExpirationMs
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        auditLogService.logEvent(
                "AUTH",
                "CUSTOMER",
                customer.getCustomerUuid(),
                AuditAction.CUSTOMER_LOGIN.name()
        );

        String token = jwtService.generateToken(customer.getEmail(), customer.getCustomerUuid());
        return new AuthResponse(
                token,
                customer.getCustomerUuid(),
                customer.getEmail(),
                customer.getName(),
                jwtExpirationMs
        );
    }
}
