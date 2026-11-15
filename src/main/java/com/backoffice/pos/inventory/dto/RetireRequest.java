package com.backoffice.pos.inventory.dto;

/** Mark a serialized unit defective/retired (removed from stock). */
public record RetireRequest(String reason) {
}
