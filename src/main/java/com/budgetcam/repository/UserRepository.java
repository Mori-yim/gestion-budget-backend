package com.budgetcam.repository;

import com.budgetcam.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// ================================================================
// REPOSITORY UTILISATEUR
// ================================================================
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    /** Tous les users ayant activé les rapports mensuels */
    List<User> findByRapportMensuelTrue();
    /** Tous les users ayant activé les alertes budget */
    List<User> findByAlerteBudgetTrue();
}


