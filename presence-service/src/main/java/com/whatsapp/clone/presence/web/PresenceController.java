package com.whatsapp.clone.presence.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    private final Random rnd = new Random();

    @GetMapping("/{userId}")
    public Map<String, Object> get(@PathVariable String userId) {

        if (rnd.nextInt(5) == 0) {
            throw new RuntimeException("simulated failure");
        }
        String status = rnd.nextBoolean() ? "online" : "offline";

        Map<String, Object> resp = new HashMap<>();
        resp.put("userId", userId);
        resp.put("status", status);
        return resp;
    }
}
