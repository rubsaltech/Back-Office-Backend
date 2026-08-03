package com.backoffice.pos.settings;

import com.backoffice.pos.business.Business;
import com.backoffice.pos.business.BusinessStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class SettingsDtos {

    private SettingsDtos() {
    }

    public record NotificationPreferences(
            boolean notifyOrders,
            boolean notifyLowStock,
            boolean notifyReports,
            boolean notifyMarketing
    ) {
        public static NotificationPreferences from(Business b) {
            return new NotificationPreferences(b.isNotifyOrders(), b.isNotifyLowStock(),
                    b.isNotifyReports(), b.isNotifyMarketing());
        }
    }

    public record BusinessSettingsResponse(
            Long id, String name, String email, String ownerName, String ownerPhone, String logoUrl,
            String address, String city, String country, String currency, String locale,
            BusinessStatus status, NotificationPreferences notifications
    ) {
        public static BusinessSettingsResponse from(Business b) {
            return new BusinessSettingsResponse(b.getId(), b.getName(), b.getEmail(), b.getOwnerName(),
                    b.getOwnerPhone(), b.getLogoUrl(), b.getAddress(), b.getCity(), b.getCountry(),
                    b.getCurrency(), b.getLocale(), b.getStatus(), NotificationPreferences.from(b));
        }
    }

    public record BusinessSettingsUpdate(
            @NotBlank String name,
            @Email String email,
            String ownerName,
            String ownerPhone,
            String logoUrl,
            String address,
            String city,
            String country,
            String currency,
            String locale
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String newPassword
    ) {
    }
}
