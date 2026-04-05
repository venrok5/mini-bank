package com.example.mini_bank.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.mini_bank.entity.Transaction;
import com.example.mini_bank.enums.TransactionType;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findById(Long cardId, Pageable pageable);

    Page<Transaction> findByIdAndTimestampBetween(Long cardId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<Transaction> findByIdAndType(Long cardId, TransactionType type, Pageable pageable);

    Page<Transaction> findByIdAndTypeAndTimestampBetween(Long cardId, TransactionType type, LocalDateTime from, LocalDateTime to, Pageable pageable);
    
    Page<Transaction> findByFromCardIdOrToCardId(Long fromCardId, Long toCardId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.fromCard.owner.id = :userId OR t.toCard.owner.id = :userId")
    Page<Transaction> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE DATE(t.timestamp) = :date")
    long countByDate(@Param("date") LocalDate date);
    
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    
    // fromCard.id = cardId OR toCard.id = cardId
    @Query("SELECT t FROM Transaction t WHERE t.fromCard.id = :cardId OR t.toCard.id = :cardId")
    Page<Transaction> findByCardId(@Param("cardId") Long cardId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.fromCard.id = :cardId OR t.toCard.id = :cardId) " +
           "AND t.timestamp BETWEEN :from AND :to")
    Page<Transaction> findByCardIdAndTimestampBetween(@Param("cardId") Long cardId, 
                                                     @Param("from") LocalDateTime from, 
                                                     @Param("to") LocalDateTime to, 
                                                     Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.fromCard.id = :cardId OR t.toCard.id = :cardId) " +
           "AND t.type = :type")
    Page<Transaction> findByCardIdAndType(@Param("cardId") Long cardId, 
                                         @Param("type") TransactionType type, 
                                         Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.fromCard.id = :cardId OR t.toCard.id = :cardId) " +
           "AND t.type = :type AND t.timestamp BETWEEN :from AND :to")
    Page<Transaction> findByCardIdAndTypeAndTimestampBetween(@Param("cardId") Long cardId, 
                                                            @Param("type") TransactionType type, 
                                                            @Param("from") LocalDateTime from, 
                                                            @Param("to") LocalDateTime to, 
                                                            Pageable pageable);
}