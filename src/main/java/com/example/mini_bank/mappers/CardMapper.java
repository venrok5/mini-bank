package com.example.mini_bank.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.example.mini_bank.dto.CardDto;
import com.example.mini_bank.entity.Card;

@Mapper(componentModel = "spring")
public interface CardMapper {

	@Mapping(target = "maskedNumber", 
			expression = "java(com.example.mini_bank.util.MaskingUtil.maskCardNumber(card.getCardNumber() ))")
    CardDto toDto(Card card);

	Card toEntity(CardDto dto);
	
	default Card toEntitySafe(CardDto dto) {
        return dto != null ? toEntity(dto) : null;
    }
	
	default CardDto toDtoSafe(Card card) {
	    return card != null ? toDto(card) : null;
	}
}