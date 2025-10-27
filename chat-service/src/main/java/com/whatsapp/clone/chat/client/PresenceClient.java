package com.whatsapp.clone.chat.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "presence-service", path = "/api/presence")
public interface PresenceClient {
    @GetMapping("/{userId}")
    Map<String, Object> get(@PathVariable("userId") String userId);
}