package com.backoffice.pos.inventory.dto;

import java.util.List;
import java.util.Map;

/**
 * Receive (restock) stock into a variant. For a quantity-tracked variant, send
 * {@code quantity}. For a unit-tracked variant (product has capture-at-stock-in
 * labels), send {@code units} — one map of per-unit label values per physical
 * unit (e.g. [{"IMEI1":"…","IMEI2":"…","Battery health":"100%"}, …]).
 */
public record ReceiveRequest(
        Integer quantity,
        List<Map<String, String>> units,
        String reason
) {
}
