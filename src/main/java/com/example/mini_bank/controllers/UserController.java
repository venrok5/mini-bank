package com.example.mini_bank.controllers;
import lombok.RequiredArgsConstructor;

import org.hibernate.validator.constraints.Range;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.example.mini_bank.dto.*;
import com.example.mini_bank.entity.Card;
import com.example.mini_bank.enums.CardStatus;
import com.example.mini_bank.exception.ErrorResponse;
import com.example.mini_bank.mappers.CardMapper;
import com.example.mini_bank.mappers.TransactionMapper;
import com.example.mini_bank.mappers.UserMapper;
import com.example.mini_bank.service.BaseService;
import com.example.mini_bank.service.CardService;
import com.example.mini_bank.service.UserService;
import com.example.mini_bank.service.ValidationService;
import com.example.mini_bank.service.client.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.math.BigDecimal;
import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
@Validated
@Tag(name = "Client", description = "Client operations for cards and transactions")
public class UserController extends BaseService {

    private final ClientService clientService;
    private final CardMapper cardMapper;
    private final TransactionMapper transactionMapper;
    private final UserMapper userMapper;
    private final UserService userService;
    private final ValidationService validationService;
    private final CardService cardService;

    @Operation(summary = "Get user cards")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cards retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PagedModelCardDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/cards")
    public ResponseEntity<Page<CardDto>> getCards(
            @RequestParam @Min(1) Long userId,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Range(min = 1, max = 100) int size) {

        logAction("Fetching cards for userId={}, status={}, page={}, size={}", userId, status, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<CardDto> cards = clientService.getUserCards(userId, status, pageable)
                .map(cardMapper::toDto);

        logAction("Fetched {} cards for userId={}", cards.getNumberOfElements(), userId);
        
        return ResponseEntity.ok(cards);
    }

    @Operation(summary = "Get user by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid user ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDto> getUser(@PathVariable @Min(1) Long userId) {
        logAction("Fetching user by userId={}", userId);
        UserResponseDto dto = userMapper.toExternalDto(userService.getUserById(userId));
        logAction("User retrieved: userId={}", dto.getId());
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Block user card")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Card blocked successfully",
                    content = @Content(schema = @Schema(implementation = CardDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "User or card not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/cards/{cardId}/block")
    public ResponseEntity<CardDto> blockCard(
            @RequestParam @Min(1) Long userId,
            @PathVariable @Min(1) Long cardId) {

        logAction("Blocking cardId={} for userId={}", cardId, userId);

        Card card = validationService.requireCard(cardService.findById(cardId));
        
        validationService.validateCardOwnership(userId, card);

        clientService.requestCardBlock(userId, cardId);
        
        CardDto dto = cardMapper.toDtoSafe(card);
        
        logAction("Card blocked: cardId={} for userId={}", cardId, userId);

        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Transfer between own cards")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transfer completed successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid transfer data or insufficient funds",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Cards not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponseDto> transferBetweenOwnCards(
            @RequestParam @Min(1) Long userId,
            @Valid @RequestBody TransactionCreateRequestDto request) {

        logAction("Transferring amount={} fromCardId={} toCardId={} for userId={}",
                request.getAmount(), request.getFromCardId(), request.getToCardId(), userId);

        TransactionResponseDto dto = transactionMapper.toResponseDto(
                clientService.transferBetweenUsersCards(userId,
                        request.getFromCardId(),
                        request.getToCardId(),
                        request.getAmount(),
                        request.getDescription())
        );

        logAction("Transfer completed fromCardId={} toCardId={} for userId={}",
                request.getFromCardId(), request.getToCardId(), userId);

        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Get card balance")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Balance retrieved successfully",
                    content = @Content(schema = @Schema(implementation = BigDecimal.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/cards/{cardId}/balance")
    public ResponseEntity<BigDecimal> getBalance(
            @RequestParam @Min(1) Long userId,
            @PathVariable @Min(1) Long cardId) {

        logAction("Fetching balance for cardId={} of userId={}", cardId, userId);

        BigDecimal balance = clientService.getCardBalance(userId, cardId);

        logAction("Balance for cardId={} of userId={} is {}", cardId, userId, balance);

        return ResponseEntity.ok(balance);
    }

    @Operation(summary = "Get card transactions history")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PagedModelTransactionResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters or filter",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/cards/{cardId}/transactions")
    public ResponseEntity<Page<TransactionResponseDto>> getTransactionsHistory(
            @RequestParam @Min(1) Long userId,
            @PathVariable @Min(1) Long cardId,
            @Valid @RequestBody TransactionFilter filter,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Range(min = 1, max = 100) int size) {

        logAction("Fetching transactions history for cardId={} userId={} page={} size={}",
                cardId, userId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        
        Page<TransactionResponseDto> transactions = clientService.getCardTransactionsHistory(userId, cardId, filter, pageable)
                .map(transactionMapper::toResponseDto);

        logAction("Fetched {} transactions for cardId={} userId={}", transactions.getNumberOfElements(), cardId, userId);

        return ResponseEntity.ok(transactions);
    }

    @Operation(summary = "Get card statement")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statement retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PagedModelTransactionResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid date range or parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/cards/{cardId}/statement")
    public ResponseEntity<Page<TransactionResponseDto>> getStatement(
            @RequestParam @Min(1) Long userId,
            @PathVariable @Min(1) Long cardId,
            @RequestParam @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String from,
            @RequestParam @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Range(min = 1, max = 100) int size) {

        logAction("Fetching statement for cardId={} userId={} from={} to={} page={} size={}",
                cardId, userId, from, to, page, size);

        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);

        if (fromDate.isAfter(toDate)) {
            logError("Invalid date range from={} to={} for cardId={} userId={}", from, to, cardId, userId);
            throw new IllegalArgumentException("From date must be before to date");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionResponseDto> transactions = clientService.getStatement(userId, cardId, fromDate, toDate, pageable)
                .map(transactionMapper::toResponseDto);

        logAction("Fetched {} transactions for statement cardId={} userId={}", transactions.getNumberOfElements(), cardId, userId);

        return ResponseEntity.ok(transactions);
    }
}