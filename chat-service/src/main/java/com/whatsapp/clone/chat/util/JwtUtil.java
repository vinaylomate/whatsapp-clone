package com.whatsapp.clone.chat.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {
    public static Map<String, Object> parse(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        Jws<Claims> j = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
        Claims c = j.getBody();
        Map<String, Object> m = new HashMap<>();
        m.put("sub", c.getSubject());
        m.put("email", c.get("email"));
        m.put("displayName", c.get("displayName"));
        return m;
    }
}

