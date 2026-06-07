package com.insync.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    // stores pwd as BCrypt hash
    @Column(name = "password_hash", nullable = false, length = 255)
    private String password;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // runs before JPA insert so created at timestamp is enforced
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // USER DETAILS INTERFACE
    @Override
    public String getUsername() {
        return this.email;
    }
    // returns empty list as everyone as same role, expand here for admin roles
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(){
        return List.of();
    }

    // change if you wanna lock/expire accounts
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
