package com.budgetcam.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime; /**
 * ================================================================
 * ENTITÉ BUDGET MENSUEL
 * ================================================================
 * Définit un plafond de dépenses par catégorie et par mois.
 *
 * Exemple :
 *   Budget Alimentation - Juin 2025 - 80 000 FCFA
 *   → Si dépenses alimentation > 80 000 FCFA → email d'alerte
 * ================================================================
 */
@Entity
@Table(name = "budgets",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "categorie_id", "mois", "annee"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_id", nullable = false)
    private Categorie categorie;

    /** Mois concerné (1-12) */
    @Column(nullable = false)
    private Integer mois;

    /** Année concernée */
    @Column(nullable = false)
    private Integer annee;

    /** Plafond de dépenses en FCFA */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montantPlafond;

    /**
     * true si l'alerte email a déjà été envoyée pour ce budget.
     * Évite d'envoyer plusieurs alertes pour le même dépassement.
     */
    @Builder.Default
    private Boolean alerteEnvoyee = false;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Vérifie si ce budget est pour le mois courant */
    public boolean estMoisCourant() {
        LocalDate now = LocalDate.now();
        return mois == now.getMonthValue() && annee == now.getYear();
    }
}
