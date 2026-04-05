package com.example.mini_bank.controllers;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseController {

    protected void logRequest(String message, Object... args) {
        log.info("[REQUEST] " + message, args);
    }

    protected void logResponse(String message, Object... args) {
        log.debug("[RESPONSE] " + message, args);
    }
}