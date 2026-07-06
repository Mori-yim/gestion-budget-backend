package com.budgetcam.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime; /**
 * ================================================================
 * ENTITÉ CATÉGORIE (personnalisée par utilisateur)
 * ================================================================
 * Exemples : Alimentation, Transport, Loyer, MTN MoMo, Santé...
 *
 * Chaque utilisateur crée ses propres catégories.
 * Certaines sont créées par défaut à l'inscription.
 */
@Entity
@Table(name = "categories")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    /** Icône emoji (ex: 🍔, 🚌, 🏠, 📱) */
    @Column(length = 10)
    @Builder.Default
    private String icone = "💳";

    /** Couleur hex pour les graphiques (ex: #3b82f6) */
    @Column(length = 7)
    @Builder.Default
    private String couleur = "#6b7280";

    /**
     * Type de catégorie :
     *   DEPENSE  → les transactions sont des sorties d'argent
     *   REVENU   → les transactions sont des entrées d'argent
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TypeCategorie type = TypeCategorie.DEPENSE;

    /** true = catégorie créée par défaut (non supprimable) */
    @Builder.Default
    private Boolean estDefaut = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum TypeCategorie { DEPENSE, REVENU }
}
