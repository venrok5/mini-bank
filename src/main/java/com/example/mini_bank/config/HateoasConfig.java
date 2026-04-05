package com.example.mini_bank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import com.example.mini_bank.controllers.AdminController;
import com.example.mini_bank.controllers.AuthController;
import com.example.mini_bank.controllers.UserController;
import com.example.mini_bank.dto.BankStats;
import com.example.mini_bank.dto.CardDto;
import com.example.mini_bank.dto.TransactionFilter;
import com.example.mini_bank.dto.TransactionResponseDto;
import com.example.mini_bank.dto.UserResponseDto;
import com.example.mini_bank.security.jwt.JwtResponseDto;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;


import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

// adding links to JwtResponseDto, UserResponseDto, CardDto in PagedModel automaticly
// wrapping EntityModel

@Configuration
public class HateoasConfig {

    // === Processors for EntityModel (одиночные объекты) ===

    @Bean
    public RepresentationModelProcessor<EntityModel<CardDto>> cardEntityProcessor() {
        return model -> {
            CardDto card = model.getContent();
            if (card != null) {
                model.add(linkTo(methodOn(UserController.class)
                        .getCards(null, null, 0, 10))
                        .withRel("all-cards"));
            }
            return model;
        };
    }

    @Bean
    public RepresentationModelProcessor<EntityModel<UserResponseDto>> userEntityProcessor() {
        return model -> {
            UserResponseDto user = model.getContent();
            if (user != null) {
                model.add(linkTo(methodOn(UserController.class)
                        .getUser(user.getId()))
                        .withSelfRel());
            }
            return model;
        };
    }

    @Bean
    public RepresentationModelProcessor<EntityModel<TransactionResponseDto>> transactionEntityProcessor() {
        return model -> {
            model.add(linkTo(methodOn(UserController.class)
                    .getTransactionsHistory(null, null, new TransactionFilter(), 0, 10))
                    .withRel("transactions"));
            return model;
        };
    }

    @Bean
    public RepresentationModelProcessor<EntityModel<JwtResponseDto>> jwtEntityProcessor() {
        return model -> {
            model.add(linkTo(methodOn(AuthController.class))
                    .withSelfRel());
            return model;
        };
    }

    @Bean
    public RepresentationModelProcessor<EntityModel<BankStats>> bankStatsEntityProcessor() {
        return model -> {
            model.add(linkTo(methodOn(AdminController.class)
                    .getBankStats())
                    .withSelfRel());
            return model;
        };
    }

    // === Processors for PagedModel (коллекции) ===

    @Bean
    public RepresentationModelProcessor<PagedModel<CardDto>> cardPagedProcessor() {
        return model -> {
            model.add(linkTo(methodOn(UserController.class)
                    .getCards(null, null, 0, 10))
                    .withRel("first-page"));
            return model;
        };
    }

    @Bean
    public RepresentationModelProcessor<PagedModel<TransactionResponseDto>> transactionPagedProcessor() {
        return model -> {
            model.add(linkTo(methodOn(UserController.class)
                    .getTransactionsHistory(null, null, new TransactionFilter(), 0, 10))
                    .withRel("first-page"));
            return model;
        };
    }

    @Bean
    public RepresentationModelProcessor<PagedModel<UserResponseDto>> userPagedProcessor() {
        return model -> {
            model.add(linkTo(methodOn(AdminController.class)
                    .getAllCards(0, 10))
                    .withRel("cards"));
            return model;
        };
    }
}