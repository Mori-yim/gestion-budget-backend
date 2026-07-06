package com.budgetcam.repository;

import com.budgetcam.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// ================================================================
// REPOSITORY TRANSACTION

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /** Transactions d'un user paginées */
    Page<Transaction> findByUserIdOrderByDateDescCreatedAtDesc(Long userId, Pageable pageable);

    /** Transactions d'un mois donné (pour les stats du dashboard) */
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.user.id = :userId
            AND EXTRACT(MONTH FROM t.date) = :mois
            AND EXTRACT(YEAR FROM t.date) = :annee
            ORDER BY t.date DESC
            """)
    List<Transaction> findByUserAndMoisAnnee(
            @Param("userId") Long userId,
            @Param("mois") int mois,
            @Param("annee") int annee
    );

    /** Transactions par catégorie et mois (pour vérifier dépassement budget) */
    @Query("""
            SELECT COALESCE(SUM(t.montant), 0)
            FROM Transaction t
            WHERE t.user.id = :userId
            AND t.categorie.id = :categorieId
            AND t.type = 'DEPENSE'
            AND EXTRACT(MONTH FROM t.date) = :mois
            AND EXTRACT(YEAR FROM t.date) = :annee
            """)
    BigDecimal sumDepensesParCategorieEtMois(
            @Param("userId") Long userId,
            @Param("categorieId") Long categorieId,
            @Param("mois") int mois,
            @Param("annee") int annee
    );

    /** Total revenus d'un mois */
    @Query("""
            SELECT COALESCE(SUM(t.montant), 0) FROM Transaction t
            WHERE t.user.id = :userId AND t.type = 'REVENU'
            AND EXTRACT(MONTH FROM t.date) = :mois
            AND EXTRACT(YEAR FROM t.date) = :annee
            """)
    BigDecimal sumRevenusParMois(
            @Param("userId") Long userId,
            @Param("mois") int mois,
            @Param("annee") int annee
    );

    /** Total dépenses d'un mois */
    @Query("""
            SELECT COALESCE(SUM(t.montant), 0) FROM Transaction t
            WHERE t.user.id = :userId AND t.type = 'DEPENSE'
            AND EXTRACT(MONTH FROM t.date) = :mois
            AND EXTRACT(YEAR FROM t.date) = :annee
            """)
    BigDecimal sumDepensesParMois(
            @Param("userId") Long userId,
            @Param("mois") int mois,
            @Param("annee") int annee
    );

    /** Dépenses groupées par catégorie pour un mois (PieChart) */
    @Query("""
            SELECT t.categorie.id, t.categorie.nom, t.categorie.icone,
                   t.categorie.couleur, SUM(t.montant), COUNT(t)
            FROM Transaction t
            WHERE t.user.id = :userId AND t.type = 'DEPENSE'
            AND EXTRACT(MONTH FROM t.date) = :mois
            AND EXTRACT(YEAR FROM t.date) = :annee
            GROUP BY t.categorie.id, t.categorie.nom, t.categorie.icone, t.categorie.couleur
            ORDER BY SUM(t.montant) DESC
            """)
    List<Object[]> depensesParCategorie(
            @Param("userId") Long userId,
            @Param("mois") int mois,
            @Param("annee") int annee
    );

    /** Évolution revenus/dépenses sur 6 mois (LineChart) */
    @Query("""
            SELECT EXTRACT(MONTH FROM t.date), EXTRACT(YEAR FROM t.date),
                   t.type, SUM(t.montant)
            FROM Transaction t
            WHERE t.user.id = :userId AND t.date >= :depuis
            GROUP BY EXTRACT(MONTH FROM t.date), EXTRACT(YEAR FROM t.date), t.type
            ORDER BY EXTRACT(YEAR FROM t.date) ASC, EXTRACT(MONTH FROM t.date) ASC
            """)
    List<Object[]> evolutionParMois(
            @Param("userId") Long userId,
            @Param("depuis") LocalDate depuis
    );

    /** Transactions par mode de paiement (pour stats camerounaises) */
    @Query("""
            SELECT t.modePaiement, COUNT(t), SUM(t.montant)
            FROM Transaction t
            WHERE t.user.id = :userId AND t.type = 'DEPENSE'
            AND EXTRACT(MONTH FROM t.date) = :mois
            AND EXTRACT(YEAR FROM t.date) = :annee
            AND t.modePaiement IS NOT NULL
            GROUP BY t.modePaiement
            """)
    List<Object[]> statsParModePaiement(
            @Param("userId") Long userId,
            @Param("mois") int mois,
            @Param("annee") int annee
    );

    /** Nombre de transactions d'un mois */
    @Query("""
            SELECT COUNT(t) FROM Transaction t
            WHERE t.user.id = :userId
            AND EXTRACT(MONTH FROM t.date) = :mois
            AND EXTRACT(YEAR FROM t.date) = :annee
            """)
    long countByUserAndMois(
            @Param("userId") Long userId,
            @Param("mois") int mois,
            @Param("annee") int annee
    );
}
