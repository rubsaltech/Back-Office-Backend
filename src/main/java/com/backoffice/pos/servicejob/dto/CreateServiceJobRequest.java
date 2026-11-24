package com.backoffice.pos.servicejob.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Start a new service/repair job for a chosen catalog service. A token is
 * generated server-side; the service's name and base charge are snapshotted.
 */
public record CreateServiceJobRequest(
        String customerName,
        String customerPhone,
        String device,
        @NotNull Long serviceId,
        String note
) {
}
