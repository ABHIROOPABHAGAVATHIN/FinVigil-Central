package com.finvigil.employee.repository;

import com.finvigil.common.enums.EmployeeRole;
import com.finvigil.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    Optional<Employee> findByEmployeeUuid(String employeeUuid);

    boolean existsByEmail(String email);

    long countByRole(EmployeeRole role);
}
