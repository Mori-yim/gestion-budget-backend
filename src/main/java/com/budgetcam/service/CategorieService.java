package com.budgetcam.service;

import com.budgetcam.dto.Dto;
import com.budgetcam.entity.Categorie;
import com.budgetcam.entity.User;
import com.budgetcam.repository.CategorieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// ================================================================
// CATEGORIE SERVICE

@Service
@RequiredArgsConstructor
@Slf4j
public class CategorieService {

    private final CategorieRepository categorieRepository;

    public List<Dto.CategorieResponse> getMesCategories(Long userId) {
        return categorieRepository.findByUserIdOrderByNomAsc(userId)
                .stream().map(Dto.CategorieResponse::fromCategorie).collect(Collectors.toList());
    }

    public List<Dto.CategorieResponse> getMesCategoriesParType(Long userId, Categorie.TypeCategorie type) {
        return categorieRepository.findByUserIdAndType(userId, type)
                .stream().map(Dto.CategorieResponse::fromCategorie).collect(Collectors.toList());
    }

    @Transactional
    public Dto.CategorieResponse createCategorie(Long userId, Dto.CreateCategorieRequest req, User user) {
        if (categorieRepository.existsByNomIgnoreCaseAndUserId(req.getNom(), userId))
            throw new IllegalArgumentException("Catégorie déjà existante : " + req.getNom());

        Categorie cat = Categorie.builder()
                .nom(req.getNom())
                .icone(req.getIcone() != null ? req.getIcone() : "💳")
                .couleur(req.getCouleur() != null ? req.getCouleur() : "#6b7280")
                .type(req.getType()).estDefaut(false).user(user).build();
        return Dto.CategorieResponse.fromCategorie(categorieRepository.save(cat));
    }

    @Transactional
    public void deleteCategorie(Long categorieId, Long userId) {
        Categorie cat = categorieRepository.findById(categorieId)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée : " + categorieId));
        if (!cat.getUser().getId().equals(userId))
            throw new SecurityException("Accès refusé");
        if (Boolean.TRUE.equals(cat.getEstDefaut()))
            throw new IllegalStateException("Impossible de supprimer une catégorie par défaut");
        categorieRepository.delete(cat);
    }
}
