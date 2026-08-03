package com.backoffice.pos.settings;

import com.backoffice.pos.settings.SettingsDtos.BusinessSettingsResponse;
import com.backoffice.pos.settings.SettingsDtos.BusinessSettingsUpdate;
import com.backoffice.pos.settings.SettingsDtos.ChangePasswordRequest;
import com.backoffice.pos.settings.SettingsDtos.NotificationPreferences;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Business/owner settings. Available to any authenticated principal of the business. */
@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

    private final SettingsService service;

    public SettingsController(SettingsService service) {
        this.service = service;
    }

    @GetMapping
    public BusinessSettingsResponse get() {
        return service.get();
    }

    @PutMapping
    public BusinessSettingsResponse update(@Valid @RequestBody BusinessSettingsUpdate request) {
        return service.update(request);
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(request);
    }

    @GetMapping("/notifications")
    public NotificationPreferences getNotifications() {
        return service.getNotifications();
    }

    @PutMapping("/notifications")
    public NotificationPreferences updateNotifications(@RequestBody NotificationPreferences request) {
        return service.updateNotifications(request);
    }

    @PostMapping("/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate() {
        service.deactivate();
    }
}
