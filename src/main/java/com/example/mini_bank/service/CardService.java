package com.example.mini_bank.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.mini_bank.entity.Card;
import com.example.mini_bank.repository.CardRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CardService extends BaseService {

	private final CardRepository cardRepsitory;
	
	public Optional<Card> findById(Long id) {
		return cardRepsitory.findById(id);
	}
}
