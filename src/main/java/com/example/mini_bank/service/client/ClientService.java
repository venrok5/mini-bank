package com.example.mini_bank.service.client;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.mini_bank.dto.TransactionFilter;
import com.example.mini_bank.entity.Card;
import com.example.mini_bank.entity.Transaction;
import com.example.mini_bank.enums.CardStatus;
import com.example.mini_bank.enums.TransactionType;
import com.example.mini_bank.repository.CardRepository;
import com.example.mini_bank.repository.TransactionRepository;
import com.example.mini_bank.service.BaseService;
import com.example.mini_bank.service.ValidationService;
import com.example.mini_bank.service.kafka.NotificationServiceProducer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientService extends BaseService {

    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final ValidationService validationService;
    private final NotificationServiceProducer notificationService;

    public Page<Card> getUserCards(Long userId, CardStatus status, Pageable pageable) {
        logAction("Fetching user cards for userId={} status={} page={} size={}", userId, status, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Card> cards;
        
        if (status == null) {
            cards = cardRepository.findByOwnerId(userId, pageable);
        } else {
            cards = cardRepository.findByOwnerIdAndStatus(userId, status, pageable);
        }
        
        logAction("Fetched {} cards for userId={}", cards.getContent().size(), userId);
        
        return cards;
    }

    @Transactional
    public void requestCardBlock(Long userId, Long cardId) {
        logAction("Requesting block for cardId={} by userId={}", cardId, userId);
       
        Card card = validationService.requireCard(cardRepository.findById(cardId));
        validationService.validateCardOwnership(userId, card);

        card.setStatus(CardStatus.BLOCK_REQUESTED);
        cardRepository.save(card);
        
        logAction("Card block requested successfully cardId={}", cardId);
        notificationService.sendCardBlockRequestEvent(card);
    }

    public Transaction transferBetweenUsersCards(Long userId, Long fromCardId, Long toCardId, BigDecimal amount, String description) {

    	logAction("Transferring between user's cards userId={} fromCard={} toCard={} amount={}", 
                userId, fromCardId, toCardId, amount);
    	
		Card from = validationService.requireCard(cardRepository.findById(fromCardId));
		Card to = validationService.requireCard(cardRepository.findById(toCardId));
		
		validationService.validateTransfer(userId, from, to, amount);
		
		return doTransfer(userId, from, to, amount, description);
    }

	@Transactional
	protected Transaction doTransfer(Long userId, Card from, Card to,
		BigDecimal amount, String description) {
		
		from.setBalance(from.getBalance().subtract(amount));
		to.setBalance(to.getBalance().add(amount));
		
		Transaction tx = new Transaction();
		tx.setFromCard(from);
		tx.setToCard(to);
		tx.setAmount(amount);
		tx.setDescription(description);
		tx.setTimestamp(LocalDateTime.now());
		tx.setType(TransactionType.TRANSFER);
		tx.setDescription("Test transfer");
		tx.setIdempotencyKey(UUID.randomUUID().toString());
		
		transactionRepository.save(tx);
		cardRepository.save(from);
		cardRepository.save(to);
		
		logAction("Transfer completed successfully transactionId={}", tx.getId());
	    notificationService.sendTransactionEvent(tx, from.getOwner().getEmail());
		
		return tx;
	}
	
    public BigDecimal getCardBalance(Long userId, Long cardId) {
        logAction("Fetching card balance for userId={} cardId={}", userId, cardId);
        
        Card card = validationService.requireCard(cardRepository.findById(cardId));
        validationService.validateCardOwnership(userId, card);
        BigDecimal balance = card.getBalance();
        logAction("Card balance retrieved userId={} cardId={} balance={}", userId, cardId, balance);
        
        return balance;
    }

    public Page<Transaction> getCardTransactionsHistory(Long userId, Long cardId, TransactionFilter filter, Pageable pageable) {
        logAction("Fetching transaction history for userId={} cardId={} filter={} page={} size={}", 
                  userId, cardId, filter, pageable.getPageNumber(), pageable.getPageSize());

        Card card = validationService.requireCard(cardRepository.findById(cardId));
        validationService.validateCardOwnership(userId, card);

        Page<Transaction> transactions;

        boolean hasDateRange = filter.getFromDate() != null && filter.getToDate() != null;
        boolean hasType = filter.getType() != null && filter.getType() != TransactionType.ALL; // TransactionType.ALL never go to repo case NULL in BD 

        if (hasDateRange && hasType) {
            transactions = transactionRepository.findByCardIdAndTypeAndTimestampBetween(
                    cardId,
                    filter.getType(),
                    filter.getFromDate().atStartOfDay(),
                    filter.getToDate().plusDays(1).atStartOfDay(),
                    pageable
            );
        } else if (hasDateRange) {
            transactions = transactionRepository.findByCardIdAndTimestampBetween(
                    cardId,
                    filter.getFromDate().atStartOfDay(),
                    filter.getToDate().plusDays(1).atStartOfDay(),
                    pageable
            );
        } else if (hasType) {
            transactions = transactionRepository.findByCardIdAndType(cardId, filter.getType(), pageable);
        } else {
            transactions = transactionRepository.findByCardId(cardId, pageable);
        }

        logAction("Fetched {} transactions for cardId={}", transactions.getContent().size(), cardId);
        return transactions;
    }
    
    public Page<Transaction> getStatement(Long userId, Long cardId, LocalDate from, LocalDate to, Pageable pageable) {
        logAction("Fetching statement for userId={} cardId={} from={} to={} page={} size={}", userId, cardId, from, to, pageable.getPageNumber(), pageable.getPageSize());
        
        Card card = validationService.requireCard(cardRepository.findById(cardId));
        validationService.validateCardOwnership(userId, card);

        Page<Transaction> statement = transactionRepository.findByIdAndTimestampBetween(
                cardId,
                from.atStartOfDay(),
                to.atTime(23, 59, 59),
                pageable
        );
        
        logAction("Fetched {} transactions for statement cardId={}", statement.getContent().size(), cardId);
        
        return statement;
    }

}