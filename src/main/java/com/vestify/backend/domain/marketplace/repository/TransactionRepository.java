package com.vestify.backend.domain.marketplace.repository;

import com.vestify.backend.domain.marketplace.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Kullanıcının "Sattığı" ürünlerin geçmişi
    Page<Transaction> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);

    // Kullanıcının "Satın Aldığı" ürünlerin geçmişi
    Page<Transaction> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);
}