package com.budgetcam;

import com.budgetcam.dto.Dto.*;
import com.budgetcam.entity.*;
import com.budgetcam.repository.*;
import com.budgetcam.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ExceptionHandler;
import com.budgetcam.exception.GlobalExceptionHandler;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * POINT D'ENTRÉE — BUDGETCAM
 * ================================================================
 * @EnableAsync : active l'exécution asynchrone (@Async sur EmailService)
 *               Sans ça, les emails seraient envoyés de façon synchrone
 *               et bloqueraient la réponse HTTP pendant l'envoi SMTP.
 */
@SpringBootApplication
@EnableAsync
public class    BudgetCamApplication {
    public static void main(String[] args) {
        SpringApplication.run(BudgetCamApplication.class, args);
        System.out.println("""
              
                  BudgetCam API démarrée !
                  http://localhost:8083/api/v1
                 Spring Mail configuré (voir application.properties)
               
                """);
    }
}


// CONTROLLER AUTH
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(authService.register(req),
                        "Compte créé ! Un email de bienvenue vous a été envoyé. 💰"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(req), "Connexion réussie"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(UserResponse.fromUser(user), "Profil récupéré"));
    }

    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse<UserResponse>> updatePreferences(
            @RequestBody UpdatePreferencesRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                authService.updatePreferences(user.getId(), req), "Préférences mises à jour"));
    }
}



// CONTROLLER CATÉGORIES

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
class CategorieController {
    private final CategorieService categorieService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategorieResponse>>> getMesCategories(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                categorieService.getMesCategories(user.getId()), "Catégories récupérées"));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<CategorieResponse>>> getParType(
            @PathVariable Categorie.TypeCategorie type,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                categorieService.getMesCategoriesParType(user.getId(), type), "Catégories récupérées"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategorieResponse>> create(
            @Valid @RequestBody CreateCategorieRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        categorieService.createCategorie(user.getId(), req, user), "Catégorie créée"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        categorieService.deleteCategorie(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Catégorie supprimée"));
    }
}


// ================================================================
// CONTROLLER TRANSACTIONS
// ================================================================
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
class TransactionController {
    private final TransactionService transactionService;

    /** GET /api/v1/transactions?page=0&size=20 */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getMesTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getMesTransactions(user.getId(), page, size),
                "Transactions récupérées"));
    }

    /** POST /api/v1/transactions */
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> create(
            @Valid @RequestBody CreateTransactionRequest req,
            @AuthenticationPrincipal User user) {
        TransactionResponse tx = transactionService.createTransaction(req, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(tx, "Transaction enregistrée"));
    }

    /** DELETE /api/v1/transactions/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        transactionService.deleteTransaction(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Transaction supprimée"));
    }
}


// ================================================================
// CONTROLLER BUDGETS
// ================================================================
@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
class BudgetController {
    private final BudgetService budgetService;

    /** GET /api/v1/budgets?mois=6&annee=2025 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getMesBudgets(
            @RequestParam(defaultValue = "0") int mois,
            @RequestParam(defaultValue = "0") int annee,
            @AuthenticationPrincipal User user) {
        LocalDate now = LocalDate.now();
        int m = mois > 0 ? mois : now.getMonthValue();
        int a = annee > 0 ? annee : now.getYear();
        return ResponseEntity.ok(ApiResponse.success(
                budgetService.getMesBudgets(user.getId(), m, a), "Budgets récupérés"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> create(
            @Valid @RequestBody CreateBudgetRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(budgetService.createBudget(req, user), "Budget créé"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        budgetService.deleteBudget(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Budget supprimé"));
    }
}


// ================================================================
// CONTROLLER DASHBOARD
// ================================================================
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
class DashboardController {
    private final DashboardService dashboardService;

    /** GET /api/v1/dashboard/stats?mois=6&annee=2025 */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStats>> getStats(
            @RequestParam(defaultValue = "0") int mois,
            @RequestParam(defaultValue = "0") int annee,
            @AuthenticationPrincipal User user) {
        LocalDate now = LocalDate.now();
        int m = mois > 0 ? mois : now.getMonthValue();
        int a = annee > 0 ? annee : now.getYear();
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getStats(user.getId(), m, a), "Statistiques récupérées"));
    }
}


// ================================================================
// DATA LOADER
// ================================================================
@Configuration
@RequiredArgsConstructor
@Slf4j
class DataLoader {
    @Bean
    CommandLineRunner loadData(
            UserRepository userRepo,
            TransactionRepository txRepo,
            BudgetRepository budgetRepo,
            CategorieRepository catRepo,
            PasswordEncoder encoder,
            AuthService authService) {
        return args -> {
            if (userRepo.count() > 0) { log.info("Données déjà présentes — skip"); return; }
            log.info("🚀 Chargement données BudgetCam...");

            // Créer un compte de démo via AuthService (crée les catégories par défaut)
            // Note : l'email de bienvenue sera tenté mais peut échouer si SMTP non configuré
            User demo;
            try {
                var req = new RegisterRequest();
                req.setFirstName("Jean"); req.setLastName("Kamga");
                req.setEmail("jean.kamga@budgetcam.cm");
                req.setPassword("Demo123!"); req.setPhone("+237677123456");
                req.setSalaireMensuel(new BigDecimal("350000"));
                authService.register(req);
                demo = userRepo.findByEmail("jean.kamga@budgetcam.cm").orElseThrow();
            } catch (Exception e) {
                log.warn("Erreur création compte démo (mail?) : {}", e.getMessage());
                return;
            }

            // Récupérer les catégories créées par défaut
            var cats = catRepo.findByUserIdOrderByNomAsc(demo.getId());
            var catAlim = cats.stream().filter(c -> c.getNom().contains("Alimentation")).findFirst().orElse(null);
            var catTransport = cats.stream().filter(c -> c.getNom().contains("Transport")).findFirst().orElse(null);
            var catLoyer = cats.stream().filter(c -> c.getNom().contains("Loyer")).findFirst().orElse(null);
            var catMTN = cats.stream().filter(c -> c.getNom().contains("MTN")).findFirst().orElse(null);
            var catSalaire = cats.stream().filter(c -> c.getNom().contains("Salaire")).findFirst().orElse(null);
            var catFreelance = cats.stream().filter(c -> c.getNom().contains("Freelance")).findFirst().orElse(null);

            LocalDate now = LocalDate.now();

            // Transactions du mois courant
            if (catSalaire != null)
                saveTx(txRepo, demo, "Salaire juin 2025", new BigDecimal("350000"),
                        Transaction.TypeTransaction.REVENU, now.withDayOfMonth(1), catSalaire, Transaction.ModePaiement.VIREMENT);
            if (catFreelance != null)
                saveTx(txRepo, demo, "Projet web client Douala", new BigDecimal("75000"),
                        Transaction.TypeTransaction.REVENU, now.withDayOfMonth(5), catFreelance, Transaction.ModePaiement.MTN_MOBILE_MONEY);
            if (catLoyer != null)
                saveTx(txRepo, demo, "Loyer appartement Makepe", new BigDecimal("85000"),
                        Transaction.TypeTransaction.DEPENSE, now.withDayOfMonth(2), catLoyer, Transaction.ModePaiement.VIREMENT);
            if (catAlim != null) {
                saveTx(txRepo, demo, "Courses Supermarché Casino", new BigDecimal("32500"),
                        Transaction.TypeTransaction.DEPENSE, now.withDayOfMonth(8), catAlim, Transaction.ModePaiement.ESPECES);
                saveTx(txRepo, demo, "Marché Mokolo", new BigDecimal("18000"),
                        Transaction.TypeTransaction.DEPENSE, now.withDayOfMonth(15), catAlim, Transaction.ModePaiement.ESPECES);
                saveTx(txRepo, demo, "Restaurant midi", new BigDecimal("4500"),
                        Transaction.TypeTransaction.DEPENSE, now.withDayOfMonth(20), catAlim, Transaction.ModePaiement.ORANGE_MONEY);
            }
            if (catTransport != null) {
                saveTx(txRepo, demo, "Taxi Douala-Yaoundé", new BigDecimal("5000"),
                        Transaction.TypeTransaction.DEPENSE, now.withDayOfMonth(10), catTransport, Transaction.ModePaiement.ESPECES);
                saveTx(txRepo, demo, "Carburant moto", new BigDecimal("8000"),
                        Transaction.TypeTransaction.DEPENSE, now.withDayOfMonth(18), catTransport, Transaction.ModePaiement.ESPECES);
            }
            if (catMTN != null)
                saveTx(txRepo, demo, "Forfait MTN 5GB", new BigDecimal("3500"),
                        Transaction.TypeTransaction.DEPENSE, now.withDayOfMonth(12), catMTN, Transaction.ModePaiement.MTN_MOBILE_MONEY);

            // Budgets du mois
            if (catAlim != null) saveBudget(budgetRepo, demo, catAlim, new BigDecimal("80000"), now.getMonthValue(), now.getYear());
            if (catTransport != null) saveBudget(budgetRepo, demo, catTransport, new BigDecimal("25000"), now.getMonthValue(), now.getYear());
            if (catLoyer != null) saveBudget(budgetRepo, demo, catLoyer, new BigDecimal("90000"), now.getMonthValue(), now.getYear());
            if (catMTN != null) saveBudget(budgetRepo, demo, catMTN, new BigDecimal("10000"), now.getMonthValue(), now.getYear());

            log.info("""
                    ================================================
                    💰 BUDGETCAM — Données chargées !
                    
                    👤 DÉMO : jean.kamga@budgetcam.cm / Demo123!
                    
                    📧 SPRING MAIL configuré :
                       Modifier application.properties pour activer
                       l'envoi réel (Mailtrap recommandé en dev)
                    ================================================
                    """);
        };
    }

    private void saveTx(TransactionRepository repo, User user, String libelle,
                         BigDecimal montant, Transaction.TypeTransaction type,
                         LocalDate date, Categorie cat, Transaction.ModePaiement mode) {
        repo.save(Transaction.builder().libelle(libelle).montant(montant).type(type)
                .date(date).categorie(cat).user(user).modePaiement(mode).build());
    }

    private void saveBudget(BudgetRepository repo, User user, Categorie cat,
                             BigDecimal plafond, int mois, int annee) {
        repo.save(Budget.builder().user(user).categorie(cat)
                .mois(mois).annee(annee).montantPlafond(plafond).build());
    }
}
