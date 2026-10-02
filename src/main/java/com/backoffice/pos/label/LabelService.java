package com.backoffice.pos.label;

import com.backoffice.pos.common.exception.ApiException;
import com.backoffice.pos.common.exception.ConflictException;
import com.backoffice.pos.common.exception.NotFoundException;
import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.label.dto.LabelRequest;
import com.backoffice.pos.label.dto.LabelResponse;
import com.backoffice.pos.tenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class LabelService {

    private final LabelRepository labels;

    public LabelService(LabelRepository labels) {
        this.labels = labels;
    }

    @Transactional(readOnly = true)
    public PageResponse<LabelResponse> list(String query, Pageable pageable) {
        Long businessId = TenantContext.requireBusinessId();
        Page<Label> page = StringUtils.hasText(query)
                ? labels.findByBusinessIdAndNameContainingIgnoreCase(businessId, query, pageable)
                : labels.findByBusinessId(businessId, pageable);
        return PageResponse.of(page, LabelResponse::from);
    }

    /** Unpaged list for the product/service attach pickers. */
    @Transactional(readOnly = true)
    public List<LabelResponse> all() {
        return labels.findByBusinessIdOrderByNameAsc(TenantContext.requireBusinessId())
                .stream().map(LabelResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public LabelResponse get(Long id) {
        return LabelResponse.from(load(id));
    }

    @Transactional
    public LabelResponse create(LabelRequest req) {
        Long businessId = TenantContext.requireBusinessId();
        if (labels.existsByBusinessIdAndName(businessId, req.name())) {
            throw new ConflictException("Label already exists: " + req.name());
        }
        Label l = new Label();
        l.setBusinessId(businessId);
        apply(l, req);
        return LabelResponse.from(labels.save(l));
    }

    @Transactional
    public LabelResponse update(Long id, LabelRequest req) {
        Label l = load(id);
        apply(l, req);
        return LabelResponse.from(labels.save(l));
    }

    @Transactional
    public void delete(Long id) {
        labels.delete(load(id));
    }

    // ---- helpers ----

    private Label load(Long id) {
        return labels.findByIdAndBusinessId(id, TenantContext.requireBusinessId())
                .orElseThrow(() -> NotFoundException.of("Label", id));
    }

    private void apply(Label l, LabelRequest req) {
        l.setName(req.name().trim());
        l.setType(req.type());
        l.setRequired(req.required());
        if (req.status() != null) {
            l.setStatus(req.status());
        }
        rebuildOptions(l, req);
    }

    private void rebuildOptions(Label l, LabelRequest req) {
        l.clearOptions();
        boolean isSelect = l.getType() == LabelType.SINGLE_SELECT || l.getType() == LabelType.MULTI_SELECT;
        if (!isSelect) {
            return; // INPUT labels carry no predefined options.
        }
        int order = 0;
        if (req.options() != null) {
            for (LabelRequest.Option o : req.options()) {
                if (o == null || !StringUtils.hasText(o.value())) {
                    continue;
                }
                LabelOption option = new LabelOption();
                option.setValue(o.value().trim());
                option.setSortOrder(o.sortOrder() == null ? order : o.sortOrder());
                l.addOption(option);
                order++;
            }
        }
        if (l.getOptions().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A select label needs at least one option");
        }
    }
}
