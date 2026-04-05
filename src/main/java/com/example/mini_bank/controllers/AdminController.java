package com.example.mini_bank.controllers;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import com.example.mini_bank.mappers.CardMapper;

import org.hibernate.validator.constraints.Range;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.mini_bank.dto.BankStats;
import com.example.mini_bank.dto.CardDto;
import com.example.mini_bank.dto.PagedModelCardDto;
import com.example.mini_bank.dto.PagedModelTransactionResponseDto;
import com.example.mini_bank.dto.TransactionResponseDto;
import com.example.mini_bank.dto.UserResponseDto;
import com.example.mini_bank.enums.CardStatus;
import com.example.mini_bank.mappers.TransactionMapper;
import com.example.mini_bank.mappers.UserMapper;
import com.example.mini_bank.service.BaseService;
import com.example.mini_bank.service.admin.AdminService;
import com.example.mini_bank.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin", description = "Admin operations for managing users and cards")
public class AdminController extends BaseService {

    private final AdminService adminService;
    private final CardMapper cardMapper;
    private final UserMapper userMapper;
    private final TransactionMapper transactionMapper;

    @Operation(summary = "Issue a new card")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Card issued successfully",
                    content = @Content(schema = @Schema(implementation = CardDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/cards")
    public ResponseEntity<CardDto> issueCard(
            @RequestParam @Min(1) Long userId,
            @RequestParam @NotBlank @Size(min = 2, max = 100) String ownerName) {

        logAction("Issuing card for userId={} with ownerName={}", userId, ownerName);

        CardDto dto = cardMapper.toDto(adminService.issueCard(userId, ownerName));

        logAction("Card issued: cardId={} for userId={}", dto.getId(), userId);

        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Change card status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Card status changed successfully",
                    content = @Content(schema = @Schema(implementation = CardDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid status or parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/cards/{cardId}/status")
    public ResponseEntity<CardDto> changeCardStatus(
            @PathVariable @Min(1) Long cardId,
            @RequestParam @NotNull CardStatus status) {

        logAction("Changing status for cardId={} to {}", cardId, status);

        CardDto dto = cardMapper.toDto(adminService.changeCardStatus(cardId, status));

        logAction("Card status changed: cardId={} newStatus={}", cardId, status);

        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Change user status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User status changed successfully",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<UserResponseDto> changeUserStatus(
            @PathVariable @Min(1) Long userId,
            @RequestParam boolean active) {

        logAction("Changing status for userId={} to active={}", userId, active);

        UserResponseDto dto = userMapper.toExternalDto(adminService.changeUserStatus(userId, active));

        logAction("User status changed: userId={} active={}", userId, active);

        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Get all cards (paged)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cards retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PagedModelCardDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/cards")
    public ResponseEntity<Page<CardDto>> getAllCards(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Range(min = 1, max = 100) int size) {

        logAction("Fetching all cards page={} size={}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<CardDto> cards = adminService.getAllCards(pageable).map(cardMapper::toDto);

        logAction("Fetched {} cards", cards.getNumberOfElements());

        return ResponseEntity.ok(cards);
    }

    @Operation(summary = "Get user transactions")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PagedModelTransactionResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/users/{userId}/transactions")
    public ResponseEntity<Page<TransactionResponseDto>> getUserTransactions(
            @PathVariable @Min(1) Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Range(min = 1, max = 100) int size) {

        logAction("Fetching transactions for userId={} page={} size={}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionResponseDto> transactions = adminService.getUserTransactions(userId, pageable)
                .map(transactionMapper::toResponseDto);

        logAction("Fetched {} transactions for userId={}", transactions.getNumberOfElements(), userId);

        return ResponseEntity.ok(transactions);
    }

    @Operation(summary = "Get bank statistics")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = BankStats.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/stats")
    public ResponseEntity<BankStats> getBankStats() {

        logAction("Fetching bank statistics");

        BankStats stats = adminService.getBankStats();

        logAction("Bank statistics retrieved: {}", stats);

        return ResponseEntity.ok(stats);
    }
}