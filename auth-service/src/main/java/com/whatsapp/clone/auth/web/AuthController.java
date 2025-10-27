package com.whatsapp.clone.auth.web;

import com.whatsapp.clone.auth.domain.AppUser;
import com.whatsapp.clone.auth.repo.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository repo;
    private final PasswordEncoder enc;

    public AuthController(UserRepository r, PasswordEncoder e) {
        this.repo = r;
        this.enc = e;
    }

    private SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> b, @RequestHeader(value = "JWT_SECRET", required = false) String sEnv) {
        String email = b.get("email"), pw = b.get("password"), name = b.getOrDefault("displayName", email);
        if (email == null || pw == null)
            return ResponseEntity.badRequest().body(Map.of("error", "email and password required"));
        if (repo.findByEmail(email).isPresent())
            return ResponseEntity.status(409).body(Map.of("error", "email exists"));
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setDisplayName(name);
        u.setPasswordHash(enc.encode(pw));
        repo.save(u);
        return ResponseEntity.ok(Map.of("id", u.getId(), "email", u.getEmail(), "displayName", u.getDisplayName()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> b, @RequestHeader(value = "JWT_SECRET", required = false) String sEnv) {
        String email = b.get("email"), pw = b.get("password");
        String secret = (sEnv != null && !sEnv.isBlank()) ? sEnv : System.getenv().getOrDefault("JWT_SECRET", "devsecret");
        var u = repo.findByEmail(email).orElse(null);
        if (u == null || !enc.matches(pw, u.getPasswordHash()))
            return ResponseEntity.status(401).body(Map.of("error", "invalid credentials"));
        String jwt = Jwts.builder().setSubject(u.getId().toString()).claim("email", u.getEmail()).claim("displayName", u.getDisplayName()).setIssuedAt(new Date()).setExpiration(Date.from(Instant.now().plusSeconds(3600 * 24 * 7))).signWith(key(secret), SignatureAlgorithm.HS256).compact();
        return ResponseEntity.ok(Map.of("token", jwt, "user", Map.of("id", u.getId().toString(), "email", u.getEmail(), "displayName", u.getDisplayName())));
    }
}
