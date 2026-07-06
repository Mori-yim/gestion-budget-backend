package com.budgetcam.scheduler;

import com.budgetcam.entity.Budget;
import com.budgetcam.entity.User;
import com.budgetcam.repository.BudgetRepository;
import com.budgetcam.repository.TransactionRepository;
import com.budgetcam.repository.UserRepository;
import com.budgetcam.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ================================================================
 * SCHEDULER — TÂCHES AUTOMATIQUES PLANIFIÉES
 * ================================================================
 *
 * @EnableScheduling : active le mécanisme de scheduling de Spring
 * @Scheduled        : annote une méthode pour l'exécuter automatiquement
 *
 * FORMATS DE CRON SPRING :
 *   "0 0 8 1 * *"  → le 1er de chaque mois à 8h00
 *   "0 0 8 * * *"  → tous les jours à 8h00
 *   "0 0/30 * * * *" → toutes les 30 minutes
 *   "0 * * * * *"  → toutes les minutes (pour les tests)
 *
 * FORMAT : secondes minutes heures jour mois jourSemaine
 *
 * Ce scheduler gère deux tâches :
 *   1. Vérification quotidienne des dépassements de budget
 *      → Envoie une alerte email si budget dépassé
 *   2. Envoi du rapport mensuel le 1er de chaque mois
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class RapportScheduler {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final EmailService emailService;

    // ============================================================
    // TÂCHE 1 : Vérification quotidienne des budgets
    // ============================================================

    /**
     * S'exécute tous les jours à 9h00.
     * Vérifie tous les budgets du mois en cours.
     * Si dépensé > plafond ET alerte pas encore envoyée → email d'alerte.
     *
     * Cron : "0 0 9 * * *"
     *   0 secondes, 0 minutes, 9 heures, tous les jours, tous les mois, tous les jours semaine
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void verifierDepassementsBudget() {
        log.info("🔔 Scheduler : vérification des budgets en cours...");

        LocalDate now = LocalDate.now();
        int moisCourant = now.getMonthValue();
        int anneeCourante = now.getYear();

        // Récupérer tous les budgets non alertés du mois courant
        List<Budget> budgets = budgetRepository
                .findBudgetsNonAlertesParMois(moisCourant, anneeCourante);

        int alertesEnvoyees = 0;

        for (Budget budget : budgets) {
            User user = budget.getUser();

            // Ne pas envoyer si l'utilisateur a désactivé les alertes
            if (!Boolean.TRUE.equals(user.getAlerteBudget())) continue;

            // Calculer le total dépensé dans cette catégorie ce mois
            BigDecimal totalDepense = transactionRepository.sumDepensesParCategorieEtMois(
                    user.getId(),
                    budget.getCategorie().getId(),
                    moisCourant,
                    anneeCourante
            );

            // Vérifier si le plafond est dépassé
            if (totalDepense.compareTo(budget.getMontantPlafond()) > 0) {

                // Récupérer la dernière transaction pour l'afficher dans l'email
                var transactions = transactionRepository
                        .findByUserAndMoisAnnee(user.getId(), moisCourant, anneeCourante);

                String derniereTx = "Transaction récente";
                LocalDate dateDerniereTx = now;

                if (!transactions.isEmpty()) {
                    var tx = transactions.get(0);
                    derniereTx = tx.getLibelle() + " — " + tx.getMontant().toPlainString() + " FCFA";
                    dateDerniereTx = tx.getDate();
                }

                // Envoyer l'email d'alerte
                emailService.envoyerAlerteDepassementBudget(
                        user.getEmail(),
                        user.getFirstName(),
                        budget.getCategorie().getNom(),
                        budget.getCategorie().getIcone(),
                        budget.getMontantPlafond(),
                        totalDepense,
                        derniereTx,
                        dateDerniereTx
                );

                // Marquer l'alerte comme envoyée pour éviter les doublons
                budget.setAlerteEnvoyee(true);
                budgetRepository.save(budget);

                alertesEnvoyees++;
            }
        }

        log.info("✅ Vérification terminée : {} alerte(s) envoyée(s)", alertesEnvoyees);
    }

    // ============================================================
    // TÂCHE 2 : Rapport mensuel automatique
    // ============================================================

    /**
     * S'exécute le 1er de chaque mois à 8h00.
     * Envoie un rapport des finances du mois PRÉCÉDENT à chaque user.
     *
     * Cron : "0 0 8 1 * *"
     *   0s, 0min, 8h, le 1er jour, tous les mois, tous les jours semaine
     */
    @Scheduled(cron = "0 0 8 1 * *")
    public void envoyerRapportsMensuels() {
        log.info("📊 Scheduler : envoi des rapports mensuels...");

        // On envoie le rapport du mois PRÉCÉDENT
        LocalDate moisPrecedent = LocalDate.now().minusMonths(1);
        int mois = moisPrecedent.getMonthValue();
        int annee = moisPrecedent.getYear();

        // Nom du mois en français (ex: "Mai 2025")
        String moisAnnee = moisPrecedent.format(
                DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH));
        // Majuscule sur le 1er caractère
        moisAnnee = moisAnnee.substring(0, 1).toUpperCase() + moisAnnee.substring(1);

        // Tous les users ayant activé les rapports mensuels
        List<User> users = userRepository.findByRapportMensuelTrue();
        int rapportsEnvoyes = 0;

        for (User user : users) {
            try {
                // Calculer les stats du mois précédent
                BigDecimal totalRevenus = transactionRepository
                        .sumRevenusParMois(user.getId(), mois, annee);
                BigDecimal totalDepenses = transactionRepository
                        .sumDepensesParMois(user.getId(), mois, annee);
                long nbTransactions = transactionRepository
                        .countByUserAndMois(user.getId(), mois, annee);

                // Pas de rapport si aucune transaction
                if (nbTransactions == 0) continue;

                // Stats de budgets
                List<Budget> budgetsUser = budgetRepository
                        .findByUserIdAndMoisAndAnnee(user.getId(), mois, annee);
                long budgetsRespectés = budgetsUser.stream()
                        .filter(b -> !b.getAlerteEnvoyee()).count();

                // Dépenses par catégorie pour le tableau du rapport
                List<Object[]> rawStats = transactionRepository
                        .depensesParCategorie(user.getId(), mois, annee);

                List<Map<String, Object>> categoriesStats = new ArrayList<>();
                for (Object[] row : rawStats) {
                    BigDecimal totalCat = (BigDecimal) row[4];
                    BigDecimal budget = getBudgetCategorie(user.getId(), (Long) row[0], mois, annee);
                    int pctBudget = budget.compareTo(BigDecimal.ZERO) > 0
                            ? totalCat.multiply(BigDecimal.valueOf(100))
                                     .divide(budget, 0, java.math.RoundingMode.HALF_UP)
                                     .intValue()
                            : 0;

                    Map<String, Object> catStat = new HashMap<>();
                    catStat.put("nom",              row[1]);
                    catStat.put("icone",            row[2]);
                    catStat.put("total",            formaterMontant(totalCat));
                    catStat.put("nbTransactions",   row[5]);
                    catStat.put("pourcentageBudget", pctBudget);
                    categoriesStats.add(catStat);
                }

                // Générer un conseil personnalisé
                String conseil = genererConseil(totalRevenus, totalDepenses, categoriesStats);

                // Envoyer le rapport
                emailService.envoyerRapportMensuel(
                        user.getEmail(), user.getFirstName(),
                        moisAnnee, totalRevenus, totalDepenses,
                        nbTransactions, budgetsRespectés,
                        budgetsUser.size(), categoriesStats, conseil
                );
                rapportsEnvoyes++;

            } catch (Exception e) {
                log.error("❌ Erreur rapport mensuel pour {} : {}", user.getEmail(), e.getMessage());
            }
        }

        log.info("✅ {} rapport(s) mensuel(s) envoyé(s) pour {}", rapportsEnvoyes, moisAnnee);
    }

    // ============================================================
    // MÉTHODES UTILITAIRES PRIVÉES
    // ============================================================

    private BigDecimal getBudgetCategorie(Long userId, Long categorieId, int mois, int annee) {
        return budgetRepository
                .findByUserIdAndCategorieIdAndMoisAndAnnee(userId, categorieId, mois, annee)
                .map(Budget::getMontantPlafond)
                .orElse(BigDecimal.ZERO);
    }

    private String formaterMontant(BigDecimal montant) {
        if (montant == null) return "0";
        return montant.toPlainString()
                .replaceAll("\\.\\d+", "")
                .replaceAll("\\B(?=(\\d{3})+(?!\\d))", " ");
    }

    /**
     * Génère un conseil financier personnalisé basé sur les stats du mois.
     */
    private String genererConseil(
            BigDecimal revenus, BigDecimal depenses,
            List<Map<String, Object>> categoriesStats
    ) {
        double tauxEpargne = revenus.compareTo(BigDecimal.ZERO) > 0
                ? 1 - depenses.doubleValue() / revenus.doubleValue()
                : 0;

        if (tauxEpargne > 0.3) {
            return "Excellent ! Votre taux d'épargne est de " +
                   (int)(tauxEpargne * 100) + "%. Pensez à investir cet excédent.";
        } else if (tauxEpargne > 0.1) {
            return "Bon mois ! Essayez d'augmenter votre épargne à 20-30% de vos revenus.";
        } else if (tauxEpargne > 0) {
            return "Votre taux d'épargne est faible. Identifiez les dépenses non essentielles à réduire.";
        } else {
            return "Vos dépenses ont dépassé vos revenus ce mois. Revoyez votre budget pour le mois prochain.";
        }
    }
}
