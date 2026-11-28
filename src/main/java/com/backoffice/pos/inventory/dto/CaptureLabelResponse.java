package com.backoffice.pos.inventory.dto;

import com.backoffice.pos.label.LabelType;

import java.util.List;

/**
 * A capture-at-stock-in label definition for a variant's product — what the
 * Receive screen must collect per unit (field name, how to enter it, options for
 * selects, an optional pre-filled default, and whether it's required).
 */
public record CaptureLabelResponse(
        String name,
        LabelType type,
        List<String> options,
        String defaultValue,
        boolean required
) {
}
