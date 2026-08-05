package com.backoffice.pos.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/** Lightweight public liveness endpoint used by the keep-alive pinger (no DB, no auth). */
@RestController
public class PingController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of("status", "ok", "time", Instant.now().toString());
    }
}
