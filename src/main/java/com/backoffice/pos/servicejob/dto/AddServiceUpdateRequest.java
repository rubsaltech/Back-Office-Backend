package com.backoffice.pos.servicejob.dto;

import com.backoffice.pos.servicejob.ServiceJobStatus;
import jakarta.validation.constraints.NotNull;

/** Record a progress update (stage + note) on a service job. */
public record AddServiceUpdateRequest(
        @NotNull ServiceJobStatus status,
        String note
) {
}
