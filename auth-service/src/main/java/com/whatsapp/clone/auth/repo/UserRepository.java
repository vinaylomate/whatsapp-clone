package com.whatsapp.clone.auth.repo;

import com.whatsapp.clone.auth.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, java.util.UUID> {
    Optional<AppUser> findByEmail(String email);
}