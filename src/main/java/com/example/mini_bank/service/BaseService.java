package com.example.mini_bank.service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseService {

    protected void logAction(String message, Object... args) {
        log.info("[ACTION] " + message, args);
    }

    protected void logDebug(String message, Object... args) {
        log.debug("[DEBUG] " + message, args);
    }

    protected void logError(String message, Object... args) {
        log.error("[ERROR] " + message, args);
    }
}