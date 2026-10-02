package com.backoffice.pos.label.dto;

import com.backoffice.pos.label.LabelType;

import java.util.List;

/**
 * A label the owner attached to a product/service, with the captured value(s).
 * One value for INPUT / SINGLE_SELECT; one-or-more for MULTI_SELECT.
 *
 * {@code name}/{@code type} are the snapshot echoed back from a prior response.
 * They let an attachment whose definition was later deleted survive a re-save
 * (the label "stays" on the item) instead of failing to resolve by id.
 */
public record AttachedLabelRequest(
        Long labelId,
        String name,
        LabelType type,
        List<String> values
) {
}
