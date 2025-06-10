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
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
@Getter
@Setter
public class DashboardVoluntarBean implements Serializable {

    @Serial // Adnotare mai modernă pentru serialVersionUID
    private static final long serialVersionUID = 1L;

    @Inject
    private VoluntarService voluntarService;
    @Inject
    private ProiectService proiectService;
    @Inject
    private TaskService taskService;
    @Inject
    private FacesContext facesContext;
    @Inject
    private DocumentGenerationService documentGenerationService;
    @Inject
    private EmailService emailService;
    private Voluntar currentVoluntar;
    private List<Proiect> proiecteleVoluntarului = new ArrayList<>();
    private List<Task> taskurileVoluntarului = new ArrayList<>();

    public DashboardVoluntarBean() {
    }

    @PostConstruct
    public void init() {
        User loggedInUser = (User) facesContext.getExternalContext().getSessionMap().get("loggedInUser");
        if (loggedInUser != null && loggedInUser.getRol() == Rol.VOLUNTAR) {
            this.currentVoluntar = voluntarService.cautaDupaUser(loggedInUser);
            if (this.currentVoluntar != null) {
                loadDashboardData();
            } else {
                String msg = "Profilul de voluntar nu a fost găsit pentru utilizatorul: " + loggedInUser.getUsername();
                System.err.println(msg);
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenție", msg));
            }
        } else {
            System.err.println("Utilizator nelogat sau fără rol de voluntar. Redirecționare către login.");
            try {
                facesContext.getExternalContext().redirect(facesContext.getExternalContext().getRequestContextPath() + "/xhtml/login.xhtml");
            } catch (IOException e) {
                System.err.println("Eroare la redirect către login: " + e.getMessage());
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare Sistem", "Nu s-a putut face redirectarea: " + e.getMessage()));
            }
        }
    }

    private void loadDashboardData() {
        if (this.currentVoluntar != null && this.currentVoluntar.getId() != null) {
            this.proiecteleVoluntarului = proiectService.gasesteProiecteDupaVoluntarId(this.currentVoluntar.getId());
            this.taskurileVoluntarului = taskService.findByVoluntar(this.currentVoluntar.getId());
            this.currentVoluntar = voluntarService.cautaDupaId(this.currentVoluntar.getId());
        }
    }

    public void completeazaTask(Task taskDeFinalizat) {
        if (taskDeFinalizat == null || taskDeFinalizat.getId() == null) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Eroare", "Task invalid."));
            return;
        }
        try {
            User loggedInUser = (User) facesContext.getExternalContext().getSessionMap().get("loggedInUser");
            if (loggedInUser == null) {
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare Sesiune", "Sesiunea a expirat. Vă rugăm să vă autentificați din nou."));
                return;
            }
            taskService.completeTask(taskDeFinalizat.getId(), loggedInUser);
            loadDashboardData(); // Reîncarcă datele
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes", "Task-ul '" + taskDeFinalizat.getTitlu() + "' a fost marcat ca finalizat!"));
        } catch (RuntimeException e) {
            System.err.println("Eroare la finalizarea task-ului '" + taskDeFinalizat.getTitlu() + "': " + e.getMessage());
            e.printStackTrace();
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare la Finalizare Task", e.getMessage()));
        }
    }
    public TaskStatus[] getTaskStatusValues() {
        return TaskStatus.values();
    }

    /**
     * Metodă apelată prin AJAX când voluntarul schimbă statusul unui task din dropdown.
     * @param task Obiectul Task de pe rândul respectiv, cu noul status deja setat de JSF.
     */
    public void onTaskStatusChange(Task task) {
        if (task == null) {
            return;
        }
        if (task.getStatus() == TaskStatus.DONE) {
            // Dacă da, apelăm logica specială care acordă și puncte
            User loggedInUser = (User) facesContext.getExternalContext().getSessionMap().get("loggedInUser");
            taskService.completeTask(task.getId(), loggedInUser);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Felicitări!", "Task-ul '" + task.getTitlu() + "' a fost finalizat și punctele au fost adăugate."));
        } else {
            // Dacă statusul este altul (STARTED, PENDING etc.), doar actualizăm task-ul
            taskService.actualizeazaTask(task);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes", "Statusul pentru task-ul '" + task.getTitlu() + "' a fost actualizat."));
        }

        loadDashboardData();
    }

    public String genereazaCertificat() {
        if (currentVoluntar == null) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Datele voluntarului nu sunt disponibile."));
            return null;
        }
        try {
            // Pentru test, pasăm null pentru organizație; serviciul va folosi datele default.
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
        return null;
    }
    public String goToModificaProfil() {
        return "/xhtml/modificaProfilVoluntar.xhtml?faces-redirect=true";
    }
}