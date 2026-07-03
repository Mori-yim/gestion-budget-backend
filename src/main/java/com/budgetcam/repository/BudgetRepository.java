package com.budgetcam.repository;

import com.budgetcam.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// ================================================================
// REPOSITORY BUDGET
// ================================================================
@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserIdAndMoisAndAnnee(Long userId, int mois, int annee);

    Optional<Budget> findByUserIdAndCategorieIdAndMoisAndAnnee(
            Long userId, Long categorieId, int mois, int annee);

    /** Budgets non encore alertés, pour vérification quotidienne */
    @Query("""
            SELECT b FROM Budget b
            WHERE b.alerteEnvoyee = false
            AND b.mois = :mois AND b.annee = :annee
            """)
    List<Budget> findBudgetsNonAlertesParMois(
            @Param("mois") int mois,
            @Param("annee") int annee
    );

    /** Nombre de budgets respectés pour un mois */
    @Query("""
            SELECT COUNT(b) FROM Budget b
            WHERE b.user.id = :userId
            AND b.mois = :mois AND b.annee = :annee
            AND b.alerteEnvoyee = false
            """)
    long countBudgetsRespectesParMois(
            @Param("userId") Long userId,
            @Param("mois") int mois,
            @Param("annee") int annee
    );
}
