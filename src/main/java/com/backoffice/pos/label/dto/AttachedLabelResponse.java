package com.backoffice.pos.label.dto;

import com.backoffice.pos.label.LabelType;

import java.util.List;

/** A label attached to a product/service, as returned to the client. */
public record AttachedLabelResponse(
        Long id,
        Long labelId,
        String name,
        LabelType type,
        List<String> values
) {
}
