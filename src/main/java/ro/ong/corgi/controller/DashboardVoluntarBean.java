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

    // Datele pentru pagină
    private Voluntar currentVoluntar;
    private List<Proiect> proiecteleVoluntarului = new ArrayList<>();
    private List<Task> taskurileVoluntarului = new ArrayList<>();

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

    private void loadDashboardData() {
        if (this.currentVoluntar != null && this.currentVoluntar.getId() != null) {
            // Re-interogăm datele pentru a fi siguri că avem cele mai noi informații
            this.proiecteleVoluntarului = proiectService.gasesteProiecteDupaVoluntarId(this.currentVoluntar.getId());
            this.taskurileVoluntarului = taskService.findByVoluntar(this.currentVoluntar.getId());
            this.currentVoluntar = voluntarService.cautaDupaId(this.currentVoluntar.getId());
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
}
