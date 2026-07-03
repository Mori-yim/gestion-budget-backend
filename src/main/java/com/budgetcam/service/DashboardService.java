package com.budgetcam.service;

import com.budgetcam.dto.Dto;
import com.budgetcam.repository.BudgetRepository;
import com.budgetcam.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

// ================================================================
// DASHBOARD SERVICE
// ================================================================
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetService budgetService;

    public Dto.DashboardStats getStats(Long userId, int mois, int annee) {

        // KPIs
        BigDecimal revenus  = transactionRepository.sumRevenusParMois(userId, mois, annee);
        BigDecimal depenses = transactionRepository.sumDepensesParMois(userId, mois, annee);
        BigDecimal solde    = revenus.subtract(depenses);
        long nbTx           = transactionRepository.countByUserAndMois(userId, mois, annee);

        // Taux d'épargne
        BigDecimal tauxEpargne = revenus.compareTo(BigDecimal.ZERO) > 0
                ? solde.multiply(BigDecimal.valueOf(100)).divide(revenus, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Nom du mois
        LocalDate date = LocalDate.of(annee, mois, 1);
        String moisLabel = date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH));
        moisLabel = moisLabel.substring(0, 1).toUpperCase() + moisLabel.substring(1);

        // PieChart : dépenses par catégorie
        List<Map<String, Object>> depensesParCat = buildDepensesParCategorie(userId, mois, annee);

        // LineChart : évolution sur 6 mois
        List<Map<String, Object>> evolution = buildEvolution(userId);

        // BarChart : stats Mobile Money camerounais
        List<Map<String, Object>> statsModePaiement = buildStatsModePaiement(userId, mois, annee);

        // Budgets du mois
        List<Dto.BudgetResponse> budgets = budgetService.getMesBudgets(userId, mois, annee);

        // Transactions récentes
        List<Dto.TransactionResponse> recentes = transactionRepository
                .findByUserAndMoisAnnee(userId, mois, annee)
                .stream().limit(5).map(Dto.TransactionResponse::fromTransaction)
                .collect(Collectors.toList());

        return Dto.DashboardStats.builder()
                .mois(mois).annee(annee).moisLabel(moisLabel)
                .totalRevenus(revenus).totalDepenses(depenses)
                .soldeMois(solde).tauxEpargne(tauxEpargne)
                .nbTransactions(nbTx)
                .depensesParCategorie(depensesParCat)
                .evolutionParMois(evolution)
                .statsParModePaiement(statsModePaiement)
                .budgets(budgets).transactionsRecentes(recentes)
                .build();
    }

    private List<Map<String, Object>> buildDepensesParCategorie(Long userId, int mois, int annee) {
        return transactionRepository.depensesParCategorie(userId, mois, annee)
                .stream().map(row -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",     row[0]);
                    m.put("name",   row[1]);  // "name" attendu par Recharts PieChart
                    m.put("icone",  row[2]);
                    m.put("value",  row[4]);  // "value" attendu par Recharts PieChart
                    m.put("fill",   row[3]);  // couleur hex
                    m.put("count",  row[5]);
                    return m;
                }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildEvolution(Long userId) {
        LocalDate depuis = LocalDate.now().minusMonths(5).withDayOfMonth(1);
        List<Object[]> raw = transactionRepository.evolutionParMois(userId, depuis);

        // Initialiser 6 derniers mois à 0
        Map<String, Map<String, Object>> parMois = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusMonths(i).withDayOfMonth(1);
            String label = d.format(DateTimeFormatter.ofPattern("MMM yy", Locale.FRENCH));
            label = label.substring(0, 1).toUpperCase() + label.substring(1);
            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("mois", label);
            pt.put("revenus",  BigDecimal.ZERO);
            pt.put("depenses", BigDecimal.ZERO);
            parMois.put(d.format(DateTimeFormatter.ofPattern("MM-yyyy")), pt);
        }

        for (Object[] row : raw) {
            try {
                int m = ((Number) row[0]).intValue();
                int a = ((Number) row[1]).intValue();
                String key = String.format("%02d-%d", m, a);
                if (parMois.containsKey(key)) {
                    String type = row[2].toString();
                    BigDecimal val = (BigDecimal) row[3];
                    if ("REVENU".equals(type)) parMois.get(key).put("revenus", val);
                    else parMois.get(key).put("depenses", val);
                }
            } catch (Exception ignored) {}
        }
        return new ArrayList<>(parMois.values());
    }

    private List<Map<String, Object>> buildStatsModePaiement(Long userId, int mois, int annee) {
        return transactionRepository.statsParModePaiement(userId, mois, annee)
                .stream().map(row -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("mode",   row[0] != null ? row[0].toString() : "AUTRE");
                    m.put("count",  row[1]);
                    m.put("total",  row[2]);
                    return m;
                }).collect(Collectors.toList());
    }
}
