package com.example.mini_bank.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.PastOrPresent;

import com.example.mini_bank.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionFilter {
	
	@JsonFormat(pattern = "yyyy-MM-dd")
	@PastOrPresent(message = "From date must be in the past or present")
    private LocalDate fromDate = LocalDate.now().minusDays(30); // last 30 days by default
	
	@JsonFormat(pattern = "yyyy-MM-dd")
    @PastOrPresent(message = "To date must be in the past or present")
    private LocalDate toDate = LocalDate.now();;
	
    private TransactionType type = TransactionType.ALL;
    
    private BigDecimal minAmount = BigDecimal.ZERO;;
    private BigDecimal maxAmount;
    
    @AssertTrue(message = "From date must be before to date")
    public boolean isDateRangeValid() {
        if (fromDate == null || toDate == null) {
            return true;
        }
        return !fromDate.isAfter(toDate);
    }
}