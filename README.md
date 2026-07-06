# gestion-budget-backend
# 💰 BudgetCam Backend — Spring Boot + Spring Mail

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?style=flat&logo=spring)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-007396?style=flat&logo=java)](https://openjdk.org/)
[![Spring Mail](https://img.shields.io/badge/Spring%20Mail-SMTP-EA4335?style=flat&logo=gmail)](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/mail/javamail/JavaMailSender.html)
[![Thymeleaf](https://img.shields.io/badge/Thymeleaf-Templates-005F0F?style=flat)](https://www.thymeleaf.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-316192?style=flat&logo=postgresql)](https://www.postgresql.org/)
[![Deploy](https://img.shields.io/badge/Deploy-Railway-0B0D0E?style=flat&logo=railway)](https://railway.app/)

API REST complète pour la plateforme **BudgetCam** — gestion de budget personnel pour le Cameroun (FCFA, MTN MoMo, Orange Money). Intègre **Spring Mail** + **Thymeleaf** pour l'envoi d'emails HTML automatiques.

---

## 📋 Table des Matières

- [À propos](#-à-propos)
- [Technologies](#-technologies)
- [Spring Mail — Guide](#-spring-mail--guide)
- [Les 3 emails automatiques](#-les-3-emails-automatiques)
- [Installation](#-installation)
- [Configuration SMTP](#-configuration-smtp)
- [API Endpoints](#-api-endpoints)
- [Modèle de données](#-modèle-de-données)
- [Auteur](#-auteur)

---

## 📖 À propos

BudgetCam Backend gère :
- **Transactions** : revenus et dépenses en FCFA avec modes de paiement camerounais
- **Budgets mensuels** : plafond par catégorie + alerte email si dépassé
- **14 catégories** créées automatiquement à l'inscription (Alimentation, MTN MoMo, Transport...)
- **Dashboard** : stats pour PieChart, LineChart 6 mois, BarChart Mobile Money
- **Navigation temporelle** : stats de n'importe quel mois passé

**Nouveauté principale :** Envoi d'emails HTML via **Spring Mail** + templates **Thymeleaf** + tâches planifiées **@Scheduled**.

---

## 🛠️ Technologies

| Technologie | Version | Usage |
|------------|---------|-------|
| Spring Boot | 3.2.5 | Framework principal |
| Spring Mail | 3.x | Envoi emails SMTP |
| Thymeleaf | 3.x | Templates HTML emails |
| Spring Security | 6.x | JWT + auth |
| Spring Data JPA | 3.x | ORM + @Query JPQL |
| PostgreSQL | 16 | Base de données |
| `@Scheduled` | Spring | Tâches planifiées (cron) |
| `@Async` | Spring | Envoi emails non-bloquant |
| JJWT | 0.11.5 | JWT HS256 |
| Lombok | Latest | Boilerplate |

---

## 📧 Spring Mail — Guide

### Dépendance Maven

```xml
<!-- Spring Mail : envoi SMTP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>

<!-- Thymeleaf : templates HTML pour les emails -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

### Fonctionnement

```
1. Inscription/Dépense → Service appelé
       ↓
2. Context Thymeleaf créé (variables firstName, montant...)
       ↓
3. templateEngine.process("email/bienvenue", ctx) → String HTML
       ↓
4. MimeMessageHelper configure From, To, Subject, HTML=true
       ↓
5. JavaMailSender.send() → SMTP → boîte email
       ↓ (@Async = non-bloquant, dans un thread séparé)
6. API répond immédiatement à l'utilisateur
```

### Code EmailService

```java
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Async  // Envoi en arrière-plan (non-bloquant)
    public void envoyerEmailBienvenue(String email, String firstName) {
        try {
            Context ctx = new Context(Locale.FRENCH);
            ctx.setVariable("firstName", firstName);
            ctx.setVariable("frontendUrl", frontendUrl);

            String html = templateEngine.process("email/bienvenue", ctx);
            envoyerEmailHtml(email, "Bienvenue sur BudgetCam !", html);

        } catch (Exception e) {
            log.error("Erreur email : {}", e.getMessage());
            // L'erreur n'est pas propagée → l'inscription réussit quand même
        }
    }

    private void envoyerEmailHtml(String to, String subject, String html)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setFrom("noreply@budgetcam.cm", "BudgetCam");
        helper.setSubject(subject);
        helper.setText(html, true); // true = HTML (pas texte brut)
        mailSender.send(message);
    }
}
```

### @Scheduled — Tâches planifiées

```java
@Component
@EnableScheduling
public class RapportScheduler {

    // Le 1er de chaque mois à 8h00
    // Format : secondes minutes heures jour mois jourSemaine
    @Scheduled(cron = "0 0 8 1 * *")
    public void envoyerRapportsMensuels() {
        // Envoie le rapport du mois précédent à tous les users
        // avec rapportMensuel = true
    }

    // Tous les jours à 9h00
    @Scheduled(cron = "0 0 9 * * *")
    public void verifierDepassementsBudget() {
        // Vérifie si des budgets sont dépassés
        // Envoie une alerte si alerteEnvoyee = false
    }
}
```

---

## 📨 Les 3 emails automatiques

### 1. Email de Bienvenue
- **Quand :** à l'inscription (`POST /api/v1/auth/register`)
- **Mode :** `@Async` non-bloquant
- **Template :** `templates/email/bienvenue.html`
- **Contenu :** message d'accueil, fonctionnalités, bouton CTA, date d'inscription

### 2. Alerte Dépassement Budget
- **Quand :** quand une dépense fait passer le total au-dessus du plafond mensuel
- **Mode :** `@Async` + vérification `@Scheduled` quotidienne à 9h00
- **Template :** `templates/email/alerte-budget.html`
- **Contenu :** catégorie, barre de progression, montants, conseils
- **Dédoublonnage :** `alerteEnvoyee = true` après envoi (1 seule alerte/budget/mois)

### 3. Rapport Mensuel
- **Quand :** le 1er de chaque mois à 8h00 (`@Scheduled`)
- **Mode :** `@Async` pour chaque utilisateur
- **Template :** `templates/email/rapport-mensuel.html`
- **Contenu :** revenus, dépenses, solde, tableau catégories (`th:each`), conseil
- **Filtre :** seulement users avec `rapportMensuel = true` ET transactions > 0

---

## 🚀 Installation

```bash
git clone https://github.com/Mori-yim/budgetcam-backend.git
cd budgetcam-backend

# Créer la BDD
psql -U postgres -c "CREATE DATABASE budgetcam_db;"

# Configurer SMTP (voir section suivante)

# Lancer sur le port 8083
mvn spring-boot:run

# Données démo créées automatiquement
# (1 compte + catégories + transactions + budgets)
```

---

## ⚙️ Configuration SMTP

```properties
# application.properties

# OPTION 1 : Mailtrap (test - recommandé)
# Créer un compte gratuit sur mailtrap.io
spring.mail.host=${MAIL_HOST:sandbox.smtp.mailtrap.io}
spring.mail.port=${MAIL_PORT:2525}
spring.mail.username=${MAIL_USERNAME:votre-username}
spring.mail.password=${MAIL_PASSWORD:votre-password}

# OPTION 2 : Gmail (mot de passe d'application requis)
# spring.mail.host=smtp.gmail.com
# spring.mail.port=587
# spring.mail.username=votre@gmail.com
# spring.mail.password=xxxx-xxxx-xxxx-xxxx  # 16 caractères

# Propriétés communes
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.default-encoding=UTF-8

# Adresse expéditeur
app.mail.from=${MAIL_FROM:noreply@budgetcam.cm}
app.mail.from-name=BudgetCam

# URL frontend (dans les liens des emails)
app.frontend-url=${FRONTEND_URL:http://localhost:5176}
```

---

## 📡 API Endpoints

### Base URL : `http://localhost:8083/api/v1`

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `POST` | `/auth/register` | Public | Inscription + email bienvenue (@Async) |
| `POST` | `/auth/login` | Public | Connexion → JWT |
| `GET` | `/auth/me` | Connecté | Profil |
| `PUT` | `/auth/preferences` | Connecté | Activer/désactiver alertes et rapports |
| `GET` | `/categories` | Connecté | Mes catégories (14 par défaut à l'inscription) |
| `GET` | `/categories/type/{type}` | Connecté | Catégories DEPENSE ou REVENU |
| `POST` | `/categories` | Connecté | Créer catégorie personnalisée |
| `GET` | `/transactions` | Connecté | Mes transactions paginées `?page=&size=` |
| `POST` | `/transactions` | Connecté | Créer + vérifier budget → alerte si dépassé |
| `DELETE` | `/transactions/{id}` | Propriétaire | Supprimer transaction |
| `GET` | `/budgets` | Connecté | Budgets du mois `?mois=&annee=` |
| `POST` | `/budgets` | Connecté | Créer budget mensuel |
| `DELETE` | `/budgets/{id}` | Propriétaire | Supprimer budget |
| `GET` | `/dashboard/stats` | Connecté | Stats `?mois=&annee=` → PieChart, LineChart, BarChart |

---

## 🗄️ Modèle de données

| Entité | Table | Champs clés |
|--------|-------|-------------|
| `User` | `users` | alerteBudget (bool), rapportMensuel (bool), salaireMensuel |
| `Categorie` | `categories` | type (DEPENSE/REVENU), icone, couleur, estDefaut (bool) |
| `Transaction` | `transactions` | montant, type (REVENU/DEPENSE), date, modePaiement (ENUM) |
| `Budget` | `budgets` | montantPlafond, alerteEnvoyee (bool), [UNIQUE: user+cat+mois+annee] |

### Modes de paiement camerounais
```
MTN_MOBILE_MONEY | ORANGE_MONEY | EXPRESS_UNION
ESPECES | CARTE_BANCAIRE | VIREMENT | AUTRE
```

---

## 👥 Compte de démonstration

| Email | Mot de passe |
|-------|-------------|
| jean.kamga@budgetcam.cm | Demo123! |

---

## ☁️ Déploiement Railway

```bash
# Variables d'environnement Railway :
DATABASE_URL=jdbc:postgresql://db.<ref>.supabase.co:5432/postgres
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=<password>
JWT_SECRET=<openssl rand -base64 64>
CORS_ORIGINS=https://budgetcam.vercel.app
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=votre@gmail.com
MAIL_PASSWORD=<mot-de-passe-application>
FRONTEND_URL=https://budgetcam.vercel.app
```

---

## 👨‍💻 Auteur

**Mori (YIMFACK MORINO)**
- 🎓 Licence DAP — Université de Douala
- 🐙 GitHub : [@Mori-yim](https://github.com/Mori-yim)
