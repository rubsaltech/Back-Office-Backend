package com.backoffice.pos.customer.dto;

import com.backoffice.pos.catalog.CatalogStatus;
import jakarta.validation.constraints.NotBlank;

/** Create or update a customer's profile (not their balance). */
public record CustomerRequest(
        @NotBlank String name,
        String email,
        String phone,
        String address,
        String city,
        String country,
        String note,
        CatalogStatus status
) {
}
