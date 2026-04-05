package com.example.mini_bank.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.mini_bank.dto.TransactionInternalDto;
import com.example.mini_bank.dto.TransactionResponseDto;
import com.example.mini_bank.entity.Transaction;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

	// entity -> internal-dto
    @Mapping(target = "fromCardId", source = "fromCard.id")
    @Mapping(target = "toCardId", source = "toCard.id")
    TransactionInternalDto toInternalDto(Transaction tx);

    // entity -> responce-dto (+mask)
    @Mapping(target = "fromCardMasked",  
    		expression = "java(com.example.mini_bank.util.MaskingUtil.maskCardNumber(tx.getFromCard().getCardNumber() ))")
    
    @Mapping(target = "toCardMasked", 
    		expression = "java(com.example.mini_bank.util.MaskingUtil.maskCardNumber(tx.getToCard().getCardNumber() ))")
    TransactionResponseDto toResponseDto(Transaction tx);
    
    Transaction toEntity(TransactionInternalDto dto);
    
    default Transaction toEntitySafe(TransactionInternalDto dto) {
        return dto != null ? toEntity(dto) : null;
    }
}