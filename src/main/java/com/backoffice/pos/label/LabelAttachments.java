package com.backoffice.pos.label;

import com.backoffice.pos.common.exception.ApiException;
import com.backoffice.pos.common.exception.NotFoundException;
import com.backoffice.pos.label.dto.AttachedLabelRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates labels attached to a product/service and resolves them into
 * snapshot data (name + type + cleaned values) that each catalog entity maps to
 * its own attachment entity. Keeps the attach rules in one place so products and
 * services behave identically.
 */
@Component
public class LabelAttachments {

    private final LabelRepository labels;

    public LabelAttachments(LabelRepository labels) {
        this.labels = labels;
    }

    /** One validated attachment: a label snapshot plus its cleaned values. */
    public record Resolved(Long labelId, String name, LabelType type, List<String> values, int sortOrder) {
    }

    public List<Resolved> resolve(List<AttachedLabelRequest> requested, Long businessId) {
        List<Resolved> result = new ArrayList<>();
        if (requested == null) {
            return result;
        }
        int order = 0;
        Set<Long> seen = new LinkedHashSet<>();
        for (AttachedLabelRequest req : requested) {
            if (req == null || req.labelId() == null) {
                continue;
            }
            if (!seen.add(req.labelId())) {
                continue; // ignore a label attached twice
            }
            List<String> values = clean(req.values());
            Label label = labels.findByIdAndBusinessId(req.labelId(), businessId).orElse(null);

            if (label != null) {
                validate(label, values);
                result.add(new Resolved(label.getId(), label.getName(), label.getType(), values, order++));
            } else if (req.name() != null && req.type() != null) {
                // Definition was deleted — preserve the client's snapshot as-is so
                // the label stays on the item (cannot re-validate a gone definition).
                result.add(new Resolved(req.labelId(), req.name(), req.type(), values, order++));
            } else {
                throw NotFoundException.of("Label", req.labelId());
            }
        }
        return result;
    }

    private void validate(Label label, List<String> values) {
        if (label.isRequired() && values.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Label \"" + label.getName() + "\" is required");
        }
        switch (label.getType()) {
            case INPUT -> {
                if (values.size() > 1) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "Label \"" + label.getName() + "\" takes a single value");
                }
            }
            case SINGLE_SELECT -> {
                if (values.size() > 1) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "Label \"" + label.getName() + "\" allows only one option");
                }
                ensureOptions(label, values);
            }
            case MULTI_SELECT -> ensureOptions(label, values);
        }
    }

    private void ensureOptions(Label label, List<String> values) {
        Set<String> allowed = label.getOptions().stream()
                .map(o -> o.getValue().toLowerCase())
                .collect(java.util.stream.Collectors.toSet());
        for (String v : values) {
            if (!allowed.contains(v.toLowerCase())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "\"" + v + "\" is not an option of label \"" + label.getName() + "\"");
            }
        }
    }

    private static List<String> clean(List<String> values) {
        List<String> out = new ArrayList<>();
        if (values != null) {
            for (String v : values) {
                if (StringUtils.hasText(v)) {
                    out.add(v.trim());
                }
            }
        }
        return out;
    }
}
