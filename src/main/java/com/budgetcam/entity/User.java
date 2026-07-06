package com.budgetcam.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ================================================================
 * ENTITÉ UTILISATEUR — BUDGETCAM
 * ================================================================
 * L'utilisateur a :
 *   - Des catégories personnalisées (alimentation, transport...)
 *   - Des transactions (revenus + dépenses)
 *   - Des budgets mensuels par catégorie
 *   - Des préférences de notification par email
 */
@Entity
@Table(name = "users")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 20)
    private String phone;

    /** Devise principale de l'utilisateur (par défaut FCFA) */
    @Column(length = 10)
    @Builder.Default
    private String devise = "FCFA";

    /** Salaire mensuel (pour calculer le taux d'épargne) */
    @Column(precision = 15, scale = 2)
    private BigDecimal salaireMensuel;

    // ── Préférences email ────────────────────────────────────
    /** true = recevoir alertes quand budget dépassé */
    @Builder.Default
    private Boolean alerteBudget = true;

    /** true = recevoir rapport mensuel le 1er du mois */
    @Builder.Default
    private Boolean rapportMensuel = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Relations ────────────────────────────────────────────
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Categorie> categories = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Budget> budgets = new ArrayList<>();

    // ── UserDetails ──────────────────────────────────────────
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }

    public String getFullName() { return firstName + " " + lastName; }

    public enum Role { USER, ADMIN }
}


