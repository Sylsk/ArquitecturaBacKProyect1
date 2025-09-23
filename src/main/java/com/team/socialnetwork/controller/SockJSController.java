package com.team.socialnetwork.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SockJSController {

    @GetMapping("/notifications/info")
    public ResponseEntity<Map<String, Object>> notificationsInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("websocket", true);
        info.put("origins", new String[]{"*:*"});
        info.put("cookie_needed", false);
        info.put("entropy", System.currentTimeMillis());
        return ResponseEntity.ok(info);
    }

    @GetMapping("/chat/info")
    public ResponseEntity<Map<String, Object>> chatInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("websocket", true);
        info.put("origins", new String[]{"*:*"});
        info.put("cookie_needed", false);
        info.put("entropy", System.currentTimeMillis());
        return ResponseEntity.ok(info);
    }
}