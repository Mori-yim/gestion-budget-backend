package com.budgetcam.service;

import com.budgetcam.dto.Dto;
import com.budgetcam.entity.Budget;
import com.budgetcam.entity.Categorie;
import com.budgetcam.entity.Transaction;
import com.budgetcam.entity.User;
import com.budgetcam.repository.BudgetRepository;
import com.budgetcam.repository.CategorieRepository;
import com.budgetcam.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// ================================================================
// TRANSACTION SERVICE
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategorieRepository categorieRepository;
    private final BudgetRepository budgetRepository;
    private final EmailService emailService;

    public Dto.PageResponse<Dto.TransactionResponse> getMesTransactions(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> result =
                transactionRepository.findByUserIdOrderByDateDescCreatedAtDesc(userId, pageable);
        List<Dto.TransactionResponse> content = result.getContent().stream()
                .map(Dto.TransactionResponse::fromTransaction).collect(Collectors.toList());
        return Dto.PageResponse.<Dto.TransactionResponse>builder()
                .content(content).page(result.getNumber()).size(result.getSize())
                .totalElements(result.getTotalElements()).totalPages(result.getTotalPages())
                .first(result.isFirst()).last(result.isLast()).build();
    }

    /**
     * Crée une transaction et vérifie si le budget de la catégorie est dépassé.
     * Si dépassé → envoie un email d'alerte.
     */
    @Transactional
    public Dto.TransactionResponse createTransaction(Dto.CreateTransactionRequest req, User user) {
        Categorie categorie = categorieRepository.findById(req.getCategorieId())
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée : " + req.getCategorieId()));

        if (!categorie.getUser().getId().equals(user.getId()))
            throw new SecurityException("Cette catégorie ne vous appartient pas");

        Transaction tx = Transaction.builder()
                .libelle(req.getLibelle()).montant(req.getMontant())
                .type(req.getType()).date(req.getDate())
                .notes(req.getNotes()).modePaiement(req.getModePaiement())
                .referenceMobileMoney(req.getReferenceMobileMoney())
                .categorie(categorie).user(user).build();

        Transaction saved = transactionRepository.save(tx);
        log.info("Transaction créée : {} {} FCFA pour {}",
                req.getType(), req.getMontant(), user.getEmail());

        // Vérifier dépassement budget seulement pour les DÉPENSES
        if (req.getType() == Transaction.TypeTransaction.DEPENSE) {
            verifierEtAlerterBudget(user, categorie, saved);
        }

        return Dto.TransactionResponse.fromTransaction(saved);
    }

    @Transactional
    public void deleteTransaction(Long txId, Long userId) {
        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new RuntimeException("Transaction non trouvée : " + txId));
        if (!tx.getUser().getId().equals(userId))
            throw new SecurityException("Accès refusé");
        transactionRepository.delete(tx);
    }

    /**
     * Vérifie si le budget de la catégorie est dépassé après la transaction.
     * Si oui et que l'alerte n'a pas encore été envoyée → email d'alerte.
     */
    private void verifierEtAlerterBudget(User user, Categorie categorie, Transaction tx) {
        if (!Boolean.TRUE.equals(user.getAlerteBudget())) return;

        LocalDate now = tx.getDate() != null ? tx.getDate() : LocalDate.now();
        int mois = now.getMonthValue();
        int annee = now.getYear();

        // Chercher le budget de cette catégorie pour ce mois
        Optional<Budget> budgetOpt = budgetRepository
                .findByUserIdAndCategorieIdAndMoisAndAnnee(user.getId(), categorie.getId(), mois, annee);

        if (budgetOpt.isEmpty()) return; // Pas de budget défini → pas d'alerte

        Budget budget = budgetOpt.get();
        if (Boolean.TRUE.equals(budget.getAlerteEnvoyee())) return; // Alerte déjà envoyée

        // Calculer le total dépensé ce mois dans cette catégorie
        BigDecimal totalDepense = transactionRepository.sumDepensesParCategorieEtMois(
                user.getId(), categorie.getId(), mois, annee);

        // Si dépassement → envoyer l'alerte email
        if (totalDepense.compareTo(budget.getMontantPlafond()) > 0) {
            emailService.envoyerAlerteDepassementBudget(
                    user.getEmail(), user.getFirstName(),
                    categorie.getNom(), categorie.getIcone(),
                    budget.getMontantPlafond(), totalDepense,
                    tx.getLibelle() + " — " + tx.getMontant().toPlainString() + " FCFA",
                    tx.getDate()
            );
            budget.setAlerteEnvoyee(true);
            budgetRepository.save(budget);
        }
    }
}
