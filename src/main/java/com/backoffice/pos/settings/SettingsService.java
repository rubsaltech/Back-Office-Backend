package com.backoffice.pos.settings;

import com.backoffice.pos.business.Business;
import com.backoffice.pos.business.BusinessRepository;
import com.backoffice.pos.business.BusinessStatus;
import com.backoffice.pos.common.exception.ApiException;
import com.backoffice.pos.common.exception.ConflictException;
import com.backoffice.pos.common.exception.NotFoundException;
import com.backoffice.pos.security.CurrentUser;
import com.backoffice.pos.settings.SettingsDtos.BusinessSettingsResponse;
import com.backoffice.pos.settings.SettingsDtos.BusinessSettingsUpdate;
import com.backoffice.pos.settings.SettingsDtos.ChangePasswordRequest;
import com.backoffice.pos.settings.SettingsDtos.NotificationPreferences;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SettingsService {

    private final BusinessRepository businesses;
    private final PasswordEncoder passwordEncoder;

    public SettingsService(BusinessRepository businesses, PasswordEncoder passwordEncoder) {
        this.businesses = businesses;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public BusinessSettingsResponse get() {
        return BusinessSettingsResponse.from(currentBusiness());
    }

    @Transactional
    public BusinessSettingsResponse update(BusinessSettingsUpdate req) {
        Business b = currentBusiness();
        if (StringUtils.hasText(req.email()) && !req.email().equalsIgnoreCase(b.getEmail())
                && businesses.existsByEmailIgnoreCase(req.email())) {
            throw new ConflictException("Another business already uses this email");
        }
        b.setName(req.name());
        if (StringUtils.hasText(req.email())) {
            b.setEmail(req.email());
        }
        b.setOwnerName(req.ownerName());
        b.setOwnerPhone(req.ownerPhone());
        b.setLogoUrl(req.logoUrl());
        b.setAddress(req.address());
        b.setCity(req.city());
        b.setCountry(req.country());
        if (StringUtils.hasText(req.currency())) {
            b.setCurrency(req.currency());
        }
        if (StringUtils.hasText(req.locale())) {
            b.setLocale(req.locale());
        }
        return BusinessSettingsResponse.from(businesses.save(b));
    }

    @Transactional
    public void changePassword(ChangePasswordRequest req) {
        Business b = currentBusiness();
        if (!passwordEncoder.matches(req.currentPassword(), b.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        b.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        businesses.save(b);
    }

    @Transactional(readOnly = true)
    public NotificationPreferences getNotifications() {
        return NotificationPreferences.from(currentBusiness());
    }

    @Transactional
    public NotificationPreferences updateNotifications(NotificationPreferences prefs) {
        Business b = currentBusiness();
        b.setNotifyOrders(prefs.notifyOrders());
        b.setNotifyLowStock(prefs.notifyLowStock());
        b.setNotifyReports(prefs.notifyReports());
        b.setNotifyMarketing(prefs.notifyMarketing());
        return NotificationPreferences.from(businesses.save(b));
    }

    @Transactional
    public void deactivate() {
        Business b = currentBusiness();
        b.setStatus(BusinessStatus.DEACTIVATED);
        businesses.save(b);
    }

    private Business currentBusiness() {
        Long businessId = CurrentUser.get().businessId();
        return businesses.findById(businessId)
                .orElseThrow(() -> NotFoundException.of("Business", businessId));
    }
}
