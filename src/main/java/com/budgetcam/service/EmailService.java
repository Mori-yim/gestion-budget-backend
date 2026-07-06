package com.budgetcam.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * EMAIL SERVICE — BUDGETCAM
 * ================================================================
 *
 * Ce service gère l'envoi de TOUS les emails de l'application.
 *
 * COMPOSANTS UTILISÉS :
 *
 * 1. JavaMailSender (Spring Boot Starter Mail)
 *    → Interface principale pour envoyer des emails
 *    → Configurée avec les paramètres SMTP de application.properties
 *    → Gère automatiquement la connexion SMTP, l'auth, le TLS
 *
 * 2. TemplateEngine (Thymeleaf)
 *    → Prend un template HTML (templates/email/*.html)
 *    → Remplace les variables ${firstName}, ${montant}, etc.
 *    → Retourne une String HTML prête à envoyer
 *
 * 3. MimeMessage / MimeMessageHelper
 *    → Permet d'envoyer des emails HTML (pas juste du texte)
 *    → Gère le From, To, Subject, Content-Type
 *
 * @Async : les emails sont envoyés en arrière-plan
 *    → L'API répond immédiatement sans attendre la fin de l'envoi
 *    → Si l'envoi échoue, l'API n'est pas impactée
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    /**
     * JavaMailSender : injecté automatiquement par Spring Boot.
     * Spring Boot lit les propriétés spring.mail.* et crée
     * automatiquement ce bean configuré.
     */
    private final JavaMailSender mailSender;

    /**
     * TemplateEngine : moteur de templates Thymeleaf.
     * Transforme les fichiers HTML en templates dynamiques.
     */
    private final TemplateEngine templateEngine;

    // Valeurs depuis application.properties
    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // ============================================================
    // EMAIL 1 : BIENVENUE (envoyé à l'inscription)
    // ============================================================

    /**
     * Envoie un email de bienvenue au nouvel utilisateur.
     *
     * @Async : envoyé en arrière-plan pour ne pas bloquer l'inscription.
     * Si l'envoi échoue, l'utilisateur est quand même créé.
     *
     * @param email     adresse du destinataire
     * @param firstName prénom (pour personnaliser le message)
     */
    @Async
    public void envoyerEmailBienvenue(String email, String firstName) {
        try {
            // 1. Créer le contexte Thymeleaf avec les variables
            Context ctx = new Context(Locale.FRENCH);
            ctx.setVariable("firstName", firstName);
            ctx.setVariable("email", email);
            ctx.setVariable("frontendUrl", frontendUrl);
            ctx.setVariable("dateInscription",
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH)));

            // 2. Rendre le template HTML avec les variables injectées
            // "email/bienvenue" → cherche templates/email/bienvenue.html
            String htmlContent = templateEngine.process("email/bienvenue", ctx);

            // 3. Envoyer l'email HTML
            envoyerEmailHtml(email, "🎉 Bienvenue sur BudgetCam !", htmlContent);

            log.info("✅ Email de bienvenue envoyé à : {}", email);

        } catch (Exception e) {
            // On log l'erreur mais on ne la propage pas
            // L'inscription réussit même si l'email échoue
            log.error("❌ Erreur envoi email bienvenue à {} : {}", email, e.getMessage());
        }
    }

    // ============================================================
    // EMAIL 2 : ALERTE BUDGET DÉPASSÉ
    // ============================================================

    /**
     * Envoie une alerte quand un budget de catégorie est dépassé.
     *
     * Paramètres du contexte Thymeleaf :
     *   - firstName      : prénom du user
     *   - categorieNom   : ex "Alimentation"
     *   - categorieIcone : ex "🍔"
     *   - budgetMensuel  : le plafond fixé (ex: 50000)
     *   - totalDepense   : ce qui a été dépensé (ex: 65000)
     *   - depassement    : totalDepense - budgetMensuel (ex: 15000)
     *   - pourcentage    : (totalDepense / budgetMensuel) * 100 (ex: 130)
     *   - derniereTx     : libellé de la dernière transaction
     *   - dateDerniereTx : date formatée de la dernière transaction
     */
    @Async
    public void envoyerAlerteDepassementBudget(
            String email,
            String firstName,
            String categorieNom,
            String categorieIcone,
            BigDecimal budgetMensuel,
            BigDecimal totalDepense,
            String derniereTx,
            LocalDate dateDerniereTx
    ) {
        try {
            // Calculer le dépassement et le pourcentage
            BigDecimal depassement = totalDepense.subtract(budgetMensuel);
            int pourcentage = budgetMensuel.compareTo(BigDecimal.ZERO) > 0
                    ? totalDepense.multiply(BigDecimal.valueOf(100))
                                  .divide(budgetMensuel, 0, java.math.RoundingMode.HALF_UP)
                                  .intValue()
                    : 0;

            // Construire le contexte Thymeleaf
            Context ctx = new Context(Locale.FRENCH);
            ctx.setVariable("firstName", firstName);
            ctx.setVariable("categorieNom", categorieNom);
            ctx.setVariable("categorieIcone", categorieIcone != null ? categorieIcone : "💳");
            ctx.setVariable("budgetMensuel", budgetMensuel.toPlainString()
                    .replaceAll("\\B(?=(\\d{3})+(?!\\d))", " "));
            ctx.setVariable("totalDepense", totalDepense.toPlainString()
                    .replaceAll("\\B(?=(\\d{3})+(?!\\d))", " "));
            ctx.setVariable("depassement", depassement.toPlainString()
                    .replaceAll("\\B(?=(\\d{3})+(?!\\d))", " "));
            ctx.setVariable("pourcentage", pourcentage);
            ctx.setVariable("derniereTx", derniereTx);
            ctx.setVariable("dateDerniereTx", dateDerniereTx != null
                    ? dateDerniereTx.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH))
                    : "");
            ctx.setVariable("frontendUrl", frontendUrl);

            // Rendre le template HTML
            String htmlContent = templateEngine.process("email/alerte-budget", ctx);

            // Sujet de l'email avec le nom de la catégorie
            String sujet = "⚠️ Budget " + categorieNom + " dépassé — BudgetCam";

            envoyerEmailHtml(email, sujet, htmlContent);

            log.info("✅ Alerte budget envoyée à {} pour catégorie : {}", email, categorieNom);

        } catch (Exception e) {
            log.error("❌ Erreur alerte budget pour {} : {}", email, e.getMessage());
        }
    }

    // ============================================================
    // EMAIL 3 : RAPPORT MENSUEL AUTOMATIQUE
    // ============================================================

    /**
     * Envoie le rapport mensuel de finances.
     * Appelé automatiquement le 1er de chaque mois par le Scheduler.
     *
     * @param email          adresse du destinataire
     * @param firstName      prénom
     * @param moisAnnee      ex: "Mai 2025"
     * @param totalRevenus   total des revenus du mois
     * @param totalDepenses  total des dépenses du mois
     * @param nbTransactions nombre de transactions
     * @param budgetsRespectés nombre de budgets non dépassés
     * @param totalBudgets   nombre total de budgets
     * @param categoriesStats liste des stats par catégorie pour Thymeleaf
     * @param conseil        conseil personnalisé du mois
     */
    @Async
    public void envoyerRapportMensuel(
            String email,
            String firstName,
            String moisAnnee,
            BigDecimal totalRevenus,
            BigDecimal totalDepenses,
            long nbTransactions,
            long budgetsRespectés,
            long totalBudgets,
            List<Map<String, Object>> categoriesStats,
            String conseil
    ) {
        try {
            BigDecimal soldeMois = totalRevenus.subtract(totalDepenses);

            // Formater les montants avec espaces (style camerounais)
            Context ctx = new Context(Locale.FRENCH);
            ctx.setVariable("firstName", firstName);
            ctx.setVariable("moisAnnee", moisAnnee);
            ctx.setVariable("totalRevenus", formaterMontant(totalRevenus));
            ctx.setVariable("totalDepenses", formaterMontant(totalDepenses));
            ctx.setVariable("soldeMois", soldeMois);
            ctx.setVariable("nbTransactions", nbTransactions);
            ctx.setVariable("budgetsRespectés", budgetsRespectés);
            ctx.setVariable("totalBudgets", totalBudgets);
            ctx.setVariable("categoriesStats", categoriesStats);
            ctx.setVariable("conseil", conseil);
            ctx.setVariable("frontendUrl", frontendUrl);

            String htmlContent = templateEngine.process("email/rapport-mensuel", ctx);
            String sujet = "📊 Votre rapport financier " + moisAnnee + " — BudgetCam";

            envoyerEmailHtml(email, sujet, htmlContent);

            log.info("✅ Rapport mensuel {} envoyé à : {}", moisAnnee, email);

        } catch (Exception e) {
            log.error("❌ Erreur rapport mensuel pour {} : {}", email, e.getMessage());
        }
    }

    // ============================================================
    // MÉTHODE PRIVÉE : Envoyer un email HTML
    // ============================================================

    /**
     * Méthode utilitaire qui envoie réellement l'email via JavaMailSender.
     *
     * MimeMessage : email avec HTML, pièces jointes, etc. (vs SimpleMailMessage = texte brut)
     * MimeMessageHelper : simplifier la configuration du MimeMessage
     *   - setTo : destinataire
     *   - setFrom : expéditeur affiché (ex: "BudgetCam <noreply@budgetcam.cm>")
     *   - setSubject : objet de l'email
     *   - setText(content, true) : true = HTML (false = texte brut)
     *
     * @param to          adresse email du destinataire
     * @param subject     objet de l'email
     * @param htmlContent contenu HTML rendu par Thymeleaf
     */
    private void envoyerEmailHtml(String to, String subject, String htmlContent)
            throws MessagingException, UnsupportedEncodingException {

        // 1. Créer un MimeMessage (supporte HTML)
        MimeMessage message = mailSender.createMimeMessage();

        // 2. Configurer avec MimeMessageHelper
        // true = multipart (pour HTML + éventuelles pièces jointes)
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        // Format : "Nom <email>"
        helper.setFrom(fromEmail, fromName);
        helper.setSubject(subject);
        // true = le contenu est du HTML (false = texte brut)
        helper.setText(htmlContent, true);

        // 3. Envoyer via JavaMailSender (utilise la config SMTP)
        mailSender.send(message);
    }

    /**
     * Formate un montant avec séparateur de milliers (style camerounais)
     * Ex: 350000 → "350 000"
     */
    private String formaterMontant(BigDecimal montant) {
        if (montant == null) return "0";
        return montant.toPlainString()
                .replaceAll("\\.\\d+", "") // supprimer décimales
                .replaceAll("\\B(?=(\\d{3})+(?!\\d))", " "); // ajouter espaces
    }
}
