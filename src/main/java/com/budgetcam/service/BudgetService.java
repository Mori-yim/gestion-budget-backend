package com.budgetcam.service;

import com.budgetcam.dto.Dto.*;
import com.budgetcam.entity.*;
import com.budgetcam.repository.*;
import com.budgetcam.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


// ================================================================
// BUDGET SERVICE

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategorieRepository categorieRepository;
    private final TransactionRepository transactionRepository;

    public List<BudgetResponse> getMesBudgets(Long userId, int mois, int annee) {
        return budgetRepository.findByUserIdAndMoisAndAnnee(userId, mois, annee)
                .stream().map(b -> buildBudgetResponse(b, userId))
                .collect(Collectors.toList());
    }

    @Transactional
    public BudgetResponse createBudget(CreateBudgetRequest req, User user) {
        Categorie cat = categorieRepository.findById(req.getCategorieId())
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
        if (!cat.getUser().getId().equals(user.getId()))
            throw new SecurityException("Cette catégorie ne vous appartient pas");

        // Si un budget existe déjà pour ce mois/catégorie → mettre à jour
        Optional<Budget> existing = budgetRepository.findByUserIdAndCategorieIdAndMoisAndAnnee(
                user.getId(), req.getCategorieId(), req.getMois(), req.getAnnee());

        Budget budget;
        if (existing.isPresent()) {
            budget = existing.get();
            budget.setMontantPlafond(req.getMontantPlafond());
            budget.setAlerteEnvoyee(false); // Réinitialiser l'alerte
        } else {
            budget = Budget.builder()
                    .user(user).categorie(cat)
                    .mois(req.getMois()).annee(req.getAnnee())
                    .montantPlafond(req.getMontantPlafond()).build();
        }
        return buildBudgetResponse(budgetRepository.save(budget), user.getId());
    }

    @Transactional
    public void deleteBudget(Long budgetId, Long userId) {
        Budget b = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget non trouvé"));
        if (!b.getUser().getId().equals(userId)) throw new SecurityException("Accès refusé");
        budgetRepository.delete(b);
    }

    private BudgetResponse buildBudgetResponse(Budget b, Long userId) {
        BigDecimal totalDepense = transactionRepository.sumDepensesParCategorieEtMois(
                userId, b.getCategorie().getId(), b.getMois(), b.getAnnee());
        BigDecimal reste = b.getMontantPlafond().subtract(totalDepense);
        int pct = b.getMontantPlafond().compareTo(BigDecimal.ZERO) > 0
                ? totalDepense.multiply(BigDecimal.valueOf(100))
                             .divide(b.getMontantPlafond(), 0, RoundingMode.HALF_UP).intValue()
                : 0;

        return BudgetResponse.builder()
                .id(b.getId()).categorie(CategorieResponse.fromCategorie(b.getCategorie()))
                .mois(b.getMois()).annee(b.getAnnee())
                .montantPlafond(b.getMontantPlafond()).totalDepense(totalDepense)
                .resteADepenser(reste).pourcentageUtilise(pct)
                .depasse(totalDepense.compareTo(b.getMontantPlafond()) > 0)
                .alerteEnvoyee(b.getAlerteEnvoyee()).build();
    }
}


