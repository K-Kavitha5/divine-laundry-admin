package com.divinelaundry.repository;

import com.divinelaundry.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Customer c where c.id = :id")
    Optional<Customer> lockForOrder(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<Customer> findByPhone(String phone);
    boolean existsByPhone(String phone);
    List<Customer> findByActiveTrueOrderByNameAsc();
    List<Customer> findTop30ByNameContainingIgnoreCaseOrPhoneContainingOrderByNameAsc(String name, String phone);
}
