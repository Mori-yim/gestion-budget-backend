package com.budgetcam.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime; /**
 * ================================================================
 * ENTITÉ TRANSACTION
 * ================================================================
 * Enregistre chaque opération financière :
 *   - Revenus : salaire, freelance, transfert reçu...
 *   - Dépenses : alimentation, loyer, MTN MoMo, Orange Money...
 *
 * IMMUABLE : une transaction ne se modifie pas.
 * Pour corriger, on crée une transaction inverse.
 */
@Entity
@Table(name = "transactions")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String libelle;            // Ex: "Courses Supermarché Casino"

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;        // Toujours positif

    /**
     * Type de transaction
     *   REVENU  → augmente le solde
     *   DEPENSE → diminue le solde
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeTransaction type;

    /** Date de la transaction (peut être dans le passé) */
    @Column(nullable = false)
    private LocalDate date;

    /** Notes supplémentaires */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Mode de paiement utilisé
     * Très important pour le contexte camerounais !
     */
    @Enumerated(EnumType.STRING)
    private ModePaiement modePaiement;

    /** Référence de la transaction Mobile Money (optionnelle) */
    private String referenceMobileMoney;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_id")
    private Categorie categorie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum TypeTransaction { REVENU, DEPENSE }

    /**
     * Modes de paiement camerounais :
     * Les plus utilisés au Cameroun en 2025
     */
    public enum ModePaiement {
        MTN_MOBILE_MONEY,    // Le plus populaire
        ORANGE_MONEY,        // 2ème plus populaire
        EXPRESS_UNION,       // Transferts interurbains
        ESPECES,             // Cash
        CARTE_BANCAIRE,      // Moins courant
        VIREMENT,            // Banque à banque
        AUTRE
    }
}
