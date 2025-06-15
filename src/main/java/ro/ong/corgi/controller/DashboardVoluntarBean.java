package ro.ong.corgi.controller;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import ro.ong.corgi.model.*;
import ro.ong.corgi.model.Enums.Rol;
import ro.ong.corgi.model.Enums.StatusProiect;
import ro.ong.corgi.model.Enums.TaskStatus;
import ro.ong.corgi.service.DocumentGenerationService;
import ro.ong.corgi.service.EmailService;
import ro.ong.corgi.service.ProiectService;
import ro.ong.corgi.service.TaskService;
import ro.ong.corgi.service.VoluntarService;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Named
@ViewScoped
@Getter
@Setter
public class DashboardVoluntarBean implements Serializable {

    private static final long serialVersionUID = 1L;

    // Servicii injectate
    @Inject private VoluntarService voluntarService;
    @Inject private ProiectService proiectService;
    @Inject private TaskService taskService;
    @Inject private DocumentGenerationService documentGenerationService;
    @Inject private EmailService emailService;
    @Inject private FacesContext facesContext;


    private Voluntar currentVoluntar;
    private List<Proiect> proiecteleVoluntarului = new ArrayList<>();
    private List<Task> taskurileVoluntarului = new ArrayList<>();
    private List<Proiect> proiecteDisponibile = new ArrayList<>();


    @PostConstruct
    public void init() {
        User loggedInUser = (User) facesContext.getExternalContext().getSessionMap().get("loggedInUser");
        if (loggedInUser != null && loggedInUser.getRol() == Rol.VOLUNTAR) {
            this.currentVoluntar = voluntarService.cautaDupaUser(loggedInUser);
            if (this.currentVoluntar != null) {
                loadDashboardData();
            } else {
                handleError("Profilul de voluntar nu a fost găsit pentru utilizatorul: " + loggedInUser.getUsername());
            }
        } else {
            redirectToLogin();
        }
    }

    private void handleError(String message) {
        System.err.println("DashboardVoluntarBean: " + message);
        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenție", message));
    }

    private void redirectToLogin() {
        try {
            facesContext.getExternalContext().redirect(facesContext.getExternalContext().getRequestContextPath() + "/xhtml/login.xhtml");
        } catch (IOException e) {
            System.err.println("Eroare la redirect către login: " + e.getMessage());
        }
    }

    // Înlocuiește metoda existentă cu aceasta
    private void loadDashboardData() {
        if (this.currentVoluntar != null && this.currentVoluntar.getId() != null) {
            // Re-interogăm datele pentru a fi siguri că avem cele mai noi informații
            this.currentVoluntar = voluntarService.cautaDupaId(this.currentVoluntar.getId());
            this.proiecteleVoluntarului = proiectService.gasesteProiecteDupaVoluntarId(this.currentVoluntar.getId());
            this.taskurileVoluntarului = taskService.findByVoluntar(this.currentVoluntar.getId());

            // --- LINII NOI ADĂUGATE ---
            // Încărcăm toate proiectele cu înscrieri deschise
            this.proiecteDisponibile = proiectService.gasesteProiecteDupaStatus(StatusProiect.INSCRIERI_DESCHISE);
        }
    }
    public void genereazaSiTrimiteRaport() {
        if (currentVoluntar == null) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Datele voluntarului nu sunt disponibile."));
            return;
        }

        try {
            // 1. Colectarea și gruparea datelor
            List<Proiect> proiectele = proiectService.gasesteProiecteDupaVoluntarId(currentVoluntar.getId());
            Map<Proiect, List<Task>> activitatiGrupate = new LinkedHashMap<>();

            for (Proiect p : proiectele) {
                List<Task> taskuriFinalizate = taskService.findByProiect(p.getId())
                        .stream()
                        .filter(t -> t.getVoluntar().getId().equals(currentVoluntar.getId()) && t.getStatus() == TaskStatus.DONE)
                        .collect(Collectors.toList());

                if (!taskuriFinalizate.isEmpty()) {
                    activitatiGrupate.put(p, taskuriFinalizate);
                }
            }

            if (activitatiGrupate.isEmpty()) {
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Info", "Nu există activități finalizate pentru a genera un raport."));
                return;
            }

            // 2. Apelarea serviciului de generare PDF
            byte[] pdfData = documentGenerationService.genereazaRaportActivitatePdf(currentVoluntar, activitatiGrupate);

            // 3. Apelarea serviciului de email
            String emailDestinatar = currentVoluntar.getUser().getEmail();
            String subiect = "Raportul tău de activitate - Asociația Corgi";
            String numeFisier = "Raport_Activitate_" + currentVoluntar.getNume() + "_" + currentVoluntar.getPrenume() + ".pdf";

            emailService.sendEmailWithAttachment(emailDestinatar, subiect, pdfData, numeFisier);

            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes!", "Raportul de activitate a fost generat și trimis pe adresa ta de email."));

        } catch (Exception e) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_FATAL, "Eroare la generare", "A apărut o eroare neașteptată: " + e.getMessage()));
            e.printStackTrace(); // Esențial pentru a vedea eroarea în log-urile serverului
        }
    }

    // --- Metode existente ---
    public String goToModificaProfil() {
        return "/xhtml/modificaProfilVoluntar.xhtml?faces-redirect=true";
    }

    public void onTaskStatusChange(Task task) {
        if (task == null) {
            return;
        }
        try {
            if (task.getStatus() == TaskStatus.DONE) {
                User loggedInUser = (User) facesContext.getExternalContext().getSessionMap().get("loggedInUser");
                taskService.completeTask(task.getId(), loggedInUser);
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Felicitări!", "Task-ul '" + task.getTitlu() + "' a fost finalizat și punctele au fost adăugate."));
            } else {
                taskService.actualizeazaTask(task);
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes", "Statusul pentru task-ul '" + task.getTitlu() + "' a fost actualizat."));
            }
            loadDashboardData(); // Reîncarcă totul pentru a actualiza și punctajul
        } catch(Exception e) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu s-a putut actualiza statusul task-ului."));
        }
    }

    public TaskStatus[] getTaskStatusValues() {
        return TaskStatus.values();
    }

    public void genereazaCertificat() {
        if (currentVoluntar == null) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Datele voluntarului nu sunt disponibile."));
            return;
        }
        try {
            Organizatie orgEmitenta = (currentVoluntar.getDepartament() != null) ? currentVoluntar.getDepartament().getOrganizatie() : null;
            byte[] pdfData = documentGenerationService.genereazaCertificatPdf(currentVoluntar, orgEmitenta);
            String emailDestinatar = currentVoluntar.getUser().getEmail();
            String subiect = "Certificatul tău de Voluntariat - Asociația Corgi";
            String numeFisier = "Certificat_Voluntariat_" + currentVoluntar.getNume() + "_" + currentVoluntar.getPrenume() + ".pdf";
            emailService.sendEmailWithAttachment(emailDestinatar, subiect, pdfData, numeFisier);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes!", "Certificatul a fost generat și trimis pe email."));
        } catch (Exception e) {
            System.err.println("A apărut o eroare în fluxul de generare/trimitere certificat: " + e.getMessage());
            e.printStackTrace();
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_FATAL, "Eroare la generare/trimitere", "A apărut o eroare neașteptată."));
        }
    }
    // ==========================================================
    // === METODE NOI PENTRU APLICARE LA PROIECTE ===
    // ==========================================================

    /**
     * Apelează serviciul pentru ca voluntarul curent să aplice la un proiect.
     * @param proiectId ID-ul proiectului la care se aplică.
     */
    public void aplicaLaProiect(Long proiectId) {
        if (currentVoluntar == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Sesiune invalidă. Vă rugăm să vă reautentificați.");
            return;
        }
        try {
            proiectService.aplicaLaProiect(proiectId, this.currentVoluntar.getId());
            addMessage(FacesMessage.SEVERITY_INFO, "Succes!", "Ai aplicat la proiect. Coordonatorul va analiza aplicația ta.");
            // Reîncărcăm datele pentru a actualiza starea butoanelor
            loadDashboardData();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_WARN, "Atenție", e.getMessage());
        }
    }

    /**
     * Verifică dacă voluntarul curent a aplicat deja la un anumit proiect.
     * Folosit în .xhtml pentru a dezactiva butonul "Aplică".
     * @param proiect Proiectul pentru care se face verificarea.
     * @return true dacă a aplicat deja, false altfel.
     */
    public boolean aAplicatLaProiect(Proiect proiect) {
        if (currentVoluntar == null || proiect.getParticipari() == null) {
            return false;
        }
        return proiect.getParticipari().stream()
                .anyMatch(gvp -> gvp.getVoluntar().getId().equals(this.currentVoluntar.getId()));
    }

    // Metodă ajutătoare pentru a afișa mesaje
    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        facesContext.addMessage(null, new FacesMessage(severity, summary, detail));
    }
}
