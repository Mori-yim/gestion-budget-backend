package com.budgetcam.dto;

import com.budgetcam.entity.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class Dto {

    // ── AUTH

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank private String firstName;
        @NotBlank private String lastName;
        @NotBlank @Email private String email;
        @NotBlank @Size(min = 6) private String password;
        private String phone;
        private BigDecimal salaireMensuel;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank @Email private String email;
        @NotBlank private String password;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AuthResponse {
        private String token;
        private String tokenType;
        private UserResponse user;

        public static AuthResponse of(String token, User u) {
            return AuthResponse.builder().token(token).tokenType("Bearer")
                    .user(UserResponse.fromUser(u)).build();
        }
    }

    // ── USER

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UserResponse {
        private Long id;
        private String firstName;
        private String lastName;
        private String fullName;
        private String email;
        private String phone;
        private String devise;
        private BigDecimal salaireMensuel;
        private Boolean alerteBudget;
        private Boolean rapportMensuel;
        private String role;
        private String createdAt;

        public static UserResponse fromUser(User u) {
            return UserResponse.builder()
                    .id(u.getId()).firstName(u.getFirstName()).lastName(u.getLastName())
                    .fullName(u.getFullName()).email(u.getEmail()).phone(u.getPhone())
                    .devise(u.getDevise()).salaireMensuel(u.getSalaireMensuel())
                    .alerteBudget(u.getAlerteBudget()).rapportMensuel(u.getRapportMensuel())
                    .role(u.getRole().name()).createdAt(u.getCreatedAt().toString()).build();
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class UpdatePreferencesRequest {
        private Boolean alerteBudget;
        private Boolean rapportMensuel;
        private BigDecimal salaireMensuel;
    }

    // ── CATÉGORIE

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CreateCategorieRequest {
        @NotBlank private String nom;
        private String icone;
        private String couleur;
        @NotNull private Categorie.TypeCategorie type;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CategorieResponse {
        private Long id;
        private String nom;
        private String icone;
        private String couleur;
        private String type;
        private Boolean estDefaut;

        public static CategorieResponse fromCategorie(Categorie c) {
            return CategorieResponse.builder()
                    .id(c.getId()).nom(c.getNom()).icone(c.getIcone())
                    .couleur(c.getCouleur()).type(c.getType().name())
                    .estDefaut(c.getEstDefaut()).build();
        }
    }

    // ── TRANSACTION

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CreateTransactionRequest {
        @NotBlank private String libelle;
        @NotNull @DecimalMin("0.01") private BigDecimal montant;
        @NotNull private Transaction.TypeTransaction type;
        @NotNull @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate date;
        private String notes;
        private Transaction.ModePaiement modePaiement;
        private String referenceMobileMoney;
        @NotNull private Long categorieId;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TransactionResponse {
        private Long id;
        private String libelle;
        private BigDecimal montant;
        private String type;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate date;
        private String notes;
        private String modePaiement;
        private String referenceMobileMoney;
        private CategorieResponse categorie;
        private String createdAt;

        public static TransactionResponse fromTransaction(Transaction t) {
            return TransactionResponse.builder()
                    .id(t.getId()).libelle(t.getLibelle()).montant(t.getMontant())
                    .type(t.getType().name()).date(t.getDate()).notes(t.getNotes())
                    .modePaiement(t.getModePaiement() != null ? t.getModePaiement().name() : null)
                    .referenceMobileMoney(t.getReferenceMobileMoney())
                    .categorie(t.getCategorie() != null ? CategorieResponse.fromCategorie(t.getCategorie()) : null)
                    .createdAt(t.getCreatedAt().toString()).build();
        }
    }

    // ── BUDGET

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CreateBudgetRequest {
        @NotNull private Long categorieId;
        @NotNull @Min(1) @Max(12) private Integer mois;
        @NotNull private Integer annee;
        @NotNull @DecimalMin("0.01") private BigDecimal montantPlafond;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BudgetResponse {
        private Long id;
        private CategorieResponse categorie;
        private Integer mois;
        private Integer annee;
        private BigDecimal montantPlafond;
        private BigDecimal totalDepense;      // Calculé dynamiquement
        private BigDecimal resteADepenser;    // montantPlafond - totalDepense
        private int pourcentageUtilise;       // 0-100+
        private boolean depasse;
        private Boolean alerteEnvoyee;
    }

    // ── DASHBOARD

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DashboardStats {
        // Mois sélectionné
        private int mois;
        private int annee;
        private String moisLabel;

        // KPIs
        private BigDecimal totalRevenus;
        private BigDecimal totalDepenses;
        private BigDecimal soldeMois;
        private BigDecimal soldeGlobal;         // Cumul depuis le début
        private long nbTransactions;
        private BigDecimal tauxEpargne;          // (revenus - dépenses) / revenus * 100

        // Graphiques
        private List<Map<String, Object>> depensesParCategorie;  // PieChart
        private List<Map<String, Object>> evolutionParMois;      // LineChart 6 mois
        private List<Map<String, Object>> statsParModePaiement;  // BarChart Mobile Money

        // Budgets du mois
        private List<BudgetResponse> budgets;

        // Transactions récentes
        private List<TransactionResponse> transactionsRecentes;
    }

    // ── PAGINATION

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PageResponse<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;
    }

    // ── API RESPONSE
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> success(T data, String message) {
            return ApiResponse.<T>builder().success(true).message(message).data(data).build();
        }
        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder().success(false).message(message).build();
        }
    }
}
