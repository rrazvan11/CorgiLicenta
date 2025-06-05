package ro.ong.corgi.controller;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import ro.ong.corgi.service.ProiectService;
import ro.ong.corgi.service.TaskService;
import ro.ong.corgi.service.VoluntarService;
import ro.ong.corgi.model.Proiect;
import ro.ong.corgi.model.Task;
import ro.ong.corgi.model.User;
import ro.ong.corgi.model.Voluntar;
import ro.ong.corgi.model.Enums.Rol; //

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
@Getter
@Setter
public class DashboardVoluntarBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private VoluntarService voluntarService;
    @Inject
    private ProiectService proiectService;
    @Inject
    private TaskService taskService;
    @Inject
    private FacesContext facesContext;

    private Voluntar currentVoluntar;
    private List<Proiect> proiecteleVoluntarului = new ArrayList<>();
    private List<Task> taskurileVoluntarului = new ArrayList<>();

    public DashboardVoluntarBean() {
    }

    @PostConstruct
    public void init() {
        User loggedInUser = (User) facesContext.getExternalContext().getSessionMap().get("loggedInUser");
        if (loggedInUser != null && loggedInUser.getRol() == Rol.VOLUNTAR) { //
            this.currentVoluntar = voluntarService.cautaDupaUser(loggedInUser); //
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
            this.proiecteleVoluntarului = proiectService.gasesteProiecteDupaVoluntarId(this.currentVoluntar.getId()); //
            this.taskurileVoluntarului = taskService.findByVoluntar(this.currentVoluntar.getId()); //
            // Reîncarcă voluntarul pentru a avea cele mai recente date (puncte, ore)
            this.currentVoluntar = voluntarService.cautaDupaId(this.currentVoluntar.getId()); //
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
                // Poți adăuga și un redirect către pagina de login aici, dacă dorești
                return;
            }

            // Asigură-te că metoda completeTask din TaskService este implementată corect
            // și gestionează actualizarea punctelor voluntarului și a statusului task-ului.
            taskService.completeTask(taskDeFinalizat.getId(), loggedInUser); //

            loadDashboardData(); // Reîncarcă toate datele pentru a reflecta schimbările

            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes", "Task-ul '" + taskDeFinalizat.getTitlu() + "' a fost marcat ca finalizat!"));

        } catch (RuntimeException e) {
            System.err.println("Eroare la finalizarea task-ului '" + taskDeFinalizat.getTitlu() + "': " + e.getMessage());
            e.printStackTrace(); // Util pentru debugging în consolă server
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare la Finalizare Task", e.getMessage()));
        }
    }

    public String genereazaCertificat() {
        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", "Funcționalitate 'Generează Certificat' în curs de dezvoltare."));
        return null; // Rămâne pe aceeași pagină
    }

    public String genereazaAdeverinta() {
        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", "Funcționalitate 'Generează Adeverință' în curs de dezvoltare."));
        return null;
    }

    public String veziContract() {
        // Aici ar trebui să preiei informații despre contract din DocumentMongoService
        // și să oferi un link de descărcare sau afișare.
        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", "Funcționalitate 'Vezi Contract' în curs de dezvoltare."));
        return null;
    }

    public String goToModificaProfil() {
        // Asigură-te că pagina /xhtml/voluntar/modificaProfilVoluntar.xhtml există
        return "/xhtml/voluntar/modificaProfilVoluntar.xhtml?faces-redirect=true";
    }
}