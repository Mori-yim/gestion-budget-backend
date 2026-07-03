package com.budgetcam.service;

import com.budgetcam.dto.Dto;
import com.budgetcam.entity.Categorie;
import com.budgetcam.entity.User;
import com.budgetcam.repository.CategorieRepository;
import com.budgetcam.repository.UserRepository;
import com.budgetcam.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// ================================================================
// AUTH SERVICE
// ================================================================
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final CategorieRepository categorieRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;
    private final EmailService emailService;

    /**
     * Inscription : crée le compte, les catégories par défaut, envoie l'email de bienvenue.
     */
    @Transactional
    public Dto.AuthResponse register(Dto.RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail()))
            throw new IllegalArgumentException("Email déjà utilisé : " + req.getEmail());

        User user = User.builder()
                .firstName(req.getFirstName()).lastName(req.getLastName())
                .email(req.getEmail()).password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone()).salaireMensuel(req.getSalaireMensuel())
                .build();
        User saved = userRepository.save(user);

        // Créer les catégories par défaut camerounaises
        creerCategoriesParDefaut(saved);

        // Envoyer l'email de bienvenue (@Async → non-bloquant)
        emailService.envoyerEmailBienvenue(saved.getEmail(), saved.getFirstName());

        log.info("Nouvel utilisateur inscrit : {}", saved.getEmail());
        return Dto.AuthResponse.of(jwtService.generateToken(saved), saved);
    }

    public Dto.AuthResponse login(Dto.LoginRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        User user = userRepository.findByEmail(req.getEmail()).orElseThrow();
        return Dto.AuthResponse.of(jwtService.generateToken(user), user);
    }

    @Transactional
    public Dto.UserResponse updatePreferences(Long userId, Dto.UpdatePreferencesRequest req) {
        User user = userRepository.findById(userId).orElseThrow();
        if (req.getAlerteBudget() != null) user.setAlerteBudget(req.getAlerteBudget());
        if (req.getRapportMensuel() != null) user.setRapportMensuel(req.getRapportMensuel());
        if (req.getSalaireMensuel() != null) user.setSalaireMensuel(req.getSalaireMensuel());
        return Dto.UserResponse.fromUser(userRepository.save(user));
    }

    /**
     * Crée les catégories de dépenses et revenus par défaut pour un nouvel utilisateur.
     * Adaptées au contexte camerounais.
     */
    private void creerCategoriesParDefaut(User user) {
        List<Categorie> defaults = List.of(
                // DÉPENSES
                cat("Alimentation", "🍔", "#f97316", Categorie.TypeCategorie.DEPENSE, user),
                cat("Transport", "🚌", "#3b82f6", Categorie.TypeCategorie.DEPENSE, user),
                cat("Loyer / Logement", "🏠", "#8b5cf6", Categorie.TypeCategorie.DEPENSE, user),
                cat("Santé", "💊", "#ef4444", Categorie.TypeCategorie.DEPENSE, user),
                cat("Éducation", "📚", "#6366f1", Categorie.TypeCategorie.DEPENSE, user),
                cat("MTN Mobile Money", "📱", "#eab308", Categorie.TypeCategorie.DEPENSE, user),
                cat("Orange Money", "🟠", "#f97316", Categorie.TypeCategorie.DEPENSE, user),
                cat("Vêtements", "👕", "#ec4899", Categorie.TypeCategorie.DEPENSE, user),
                cat("Loisirs", "🎮", "#14b8a6", Categorie.TypeCategorie.DEPENSE, user),
                cat("Autres dépenses", "💳", "#6b7280", Categorie.TypeCategorie.DEPENSE, user),
                // REVENUS
                cat("Salaire", "💼", "#22c55e", Categorie.TypeCategorie.REVENU, user),
                cat("Freelance", "💻", "#10b981", Categorie.TypeCategorie.REVENU, user),
                cat("Transfert reçu", "📥", "#06b6d4", Categorie.TypeCategorie.REVENU, user),
                cat("Autres revenus", "💰", "#84cc16", Categorie.TypeCategorie.REVENU, user)
        );
        categorieRepository.saveAll(defaults);
    }

    private Categorie cat(String nom, String icone, String couleur,
                           Categorie.TypeCategorie type, User user) {
        return Categorie.builder().nom(nom).icone(icone).couleur(couleur)
                .type(type).estDefaut(true).user(user).build();
    }
}
