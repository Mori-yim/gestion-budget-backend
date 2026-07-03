package com.budgetcam.repository;

import com.budgetcam.entity.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// ================================================================
// REPOSITORY CATÉGORIE
// ================================================================
@Repository
public interface CategorieRepository extends JpaRepository<Categorie, Long> {
    List<Categorie> findByUserIdOrderByNomAsc(Long userId);
    List<Categorie> findByUserIdAndType(Long userId, Categorie.TypeCategorie type);
    boolean existsByNomIgnoreCaseAndUserId(String nom, Long userId);
}
