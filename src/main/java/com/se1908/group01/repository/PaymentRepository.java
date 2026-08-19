package com.se1908.group01.repository;

import com.se1908.group01.entity.Payment;
import com.se1908.group01.entity.User;
import com.se1908.group01.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionNo(
            String transactionNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Payment p
            WHERE p.transactionNo = :transactionNo
           """)
    Optional<Payment> findByTransactionNoForUpdate(
            @Param("transactionNo") String transactionNo);

    List<Payment> findByUser(User user);

    Page<Payment> findByStatus(
            PaymentStatus status,
            Pageable pageable);

    long countByStatus(PaymentStatus status);

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.status = 'SUCCESS'
           """)
    BigDecimal getTotalRevenue();
}
