package ro.ong.corgi.controller;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import ro.ong.corgi.dto.SedintaDTO;
import ro.ong.corgi.model.*;
import ro.ong.corgi.model.Enums.StatusAplicari;
import ro.ong.corgi.model.Enums.StatusPrezenta;
import ro.ong.corgi.model.Enums.StatusProiect;
import ro.ong.corgi.model.Enums.TipSedinta;
import ro.ong.corgi.repository.PrezentaSedintaRepository;
import ro.ong.corgi.service.*;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Named
@ViewScoped
@Getter
@Setter
public class DashboardCoordonatorBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject private VoluntarService voluntarService;
    @Inject private DepartamentService departamentService;
    @Inject private SedintaService sedintaService;
    @Inject private PrezentaSedintaRepository prezentaSedintaRepository;
    @Inject private ProiectService proiectService;
    @Inject private TaskService taskService;




    private List<Proiect> proiecteInscrieri = new ArrayList<>();
    private List<Proiect> proiecteInDerulare = new ArrayList<>();
    private Task taskNou = new Task();
    private Task taskSelectat;
    private User loggedInUser;
    private Voluntar coordonatorProfile;
    private Departament departamentCoordonat;

    private List<SedintaDTO> sedinteDepartament = new ArrayList<>();
    private List<Voluntar> voluntariDepartament = new ArrayList<>();
    private Sedinta sedintaCurenta;
    private Map<Long, StatusPrezenta> prezenteDepartamentMap = new HashMap<>();
    private boolean editModePrezenta = false;
    private SedintaDTO sedintaSelectata;
    private Organizatie organizatieCurenta; // Avem nevoie de organizație pentru a crea proiecte
    private List<Proiect> proiecteCoordonator = new ArrayList<>();
    private Proiect proiectNou = new Proiect();
    private Proiect proiectSelectat;
    private List<GrupareVoluntariProiecte> aplicatiiPentruProiect = new ArrayList<>();
    private List<Voluntar> voluntariAcceptatiInProiect = new ArrayList<>();
    @PostConstruct
    public void init() {
        this.loggedInUser = (User) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("loggedInUser");
        if (loggedInUser == null) { redirectToLogin(); return; }

        this.coordonatorProfile = voluntarService.cautaDupaUser(loggedInUser);
        if (this.coordonatorProfile == null) { redirectToLogin(); return; }

        this.departamentCoordonat = departamentService.findByCoordonator(this.coordonatorProfile);

        // Extragem organizația din departamentul coordonat
        if (this.departamentCoordonat != null && this.departamentCoordonat.getOrganizatie() != null) {
            this.organizatieCurenta = this.departamentCoordonat.getOrganizatie();
            // Încărcăm toate datele necesare pentru dashboard
            loadDashboardData();
        }
    }
    private void loadDashboardData() {
        // Logica existentă pentru ședințe
        if (this.departamentCoordonat != null) {
            this.voluntariDepartament = voluntarService.gasesteVoluntariDinDepartament(this.departamentCoordonat.getId());
            this.sedinteDepartament = sedintaService.getSedinteInfoPentruDepartament(this.departamentCoordonat.getId());
        }

        if (this.organizatieCurenta != null) {
            List<Proiect> toateProiectele = proiectService.gasesteDupaOrganizatie(this.organizatieCurenta.getId());

            // ==========================================================
            // === ADAUGĂ ACEST BLOC PENTRU DEBUG ===
            // ==========================================================
            System.out.println("--- DEBUG START ---");
            System.out.println("Metoda gasesteDupaOrganizatie a returnat: " + toateProiectele.size() + " proiecte.");
            for (Proiect p : toateProiectele) {
                System.out.println("--> Proiect găsit: '" + p.getNumeProiect() + "' cu Status: " + p.getStatus());
            }
            System.out.println("--- DEBUG END ---");
            // ==========================================================

            // Logica existentă de filtrare
            this.proiecteInscrieri = toateProiectele.stream()
                    .filter(p -> p.getStatus() == StatusProiect.INSCRIERI_DESCHISE || p.getStatus() == StatusProiect.INSCRIERI_INCHISE)
                    .collect(Collectors.toList());

            this.proiecteInDerulare = toateProiectele.stream()
                    .filter(p -> p.getStatus() == StatusProiect.IN_CURS)
                    .collect(Collectors.toList());
        }
    }

    public void pregatesteSedintaNoua() {
        this.editModePrezenta = false;
        this.sedintaCurenta = new Sedinta();
        this.sedintaCurenta.setDescriere("Ședință departament " + this.departamentCoordonat.getNume());

        prezenteDepartamentMap.clear();
        if (this.voluntariDepartament != null) {
            this.voluntariDepartament.forEach(v -> prezenteDepartamentMap.put(v.getId(), StatusPrezenta.ABSENT));
        }
    }

    public void pregatesteEditarePrezenta() {
        if (sedintaSelectata == null) return;
        this.editModePrezenta = true;
        this.sedintaCurenta = sedintaSelectata.getSedinta();

        List<PrezentaSedinta> prezenteExistente = prezentaSedintaRepository.findBySedintaId(sedintaCurenta.getId());

        prezenteDepartamentMap.clear();
        prezenteExistente.forEach(p -> prezenteDepartamentMap.put(p.getVoluntar().getId(), p.getStatusPrezenta()));

        if (this.voluntariDepartament != null) {
            this.voluntariDepartament.forEach(v -> prezenteDepartamentMap.putIfAbsent(v.getId(), StatusPrezenta.ABSENT));
        }
    }

    public void saveOrUpdatePrezenta() {
        try {
            if (editModePrezenta) {
                sedintaService.actualizeazaPrezenta(sedintaCurenta.getId(), prezenteDepartamentMap);
                addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Prezența a fost actualizată.");
            } else {
                sedintaCurenta.setDepartament(this.departamentCoordonat);
                sedintaCurenta.setOrganizatie(this.departamentCoordonat.getOrganizatie());
                sedintaCurenta.setTipSedinta(TipSedinta.DEPARTAMENT);
                sedintaService.creeazaSiInregistreazaPrezenta(sedintaCurenta, prezenteDepartamentMap);
                addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Ședința nouă a fost salvată.");
            }
            init();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Operațiunea a eșuat: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void executaStergereSedinta() {
        if (sedintaSelectata == null) return;
        try {
            sedintaService.stergeSedintaSiPrezentele(sedintaSelectata.getSedinta().getId());
            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Ședința a fost ștearsă.");
            init();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Eroare la ștergere", e.getMessage());
        }
    }

    public StatusPrezenta[] getStatusPrezentaValues() { return StatusPrezenta.values(); }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
    }

    private void redirectToLogin() {
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(
                    FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/xhtml/login.xhtml"
            );
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ==========================================================
    // === METODE NOI PENTRU MANAGEMENTUL PROIECTELOR ===
    // ==========================================================

    /**
     * Inițializează un obiect nou pentru dialogul de creare proiect.
     */
    public void pregatesteProiectNou() {
        this.proiectNou = new Proiect();
        // Setăm data de început la ziua curentă, pentru conveniență
        this.proiectNou.setDataInceput(LocalDate.now());
    }

    /**
     * Apelează serviciul pentru a salva un proiect nou în baza de date.
     */
    /**
     * Apelează serviciul pentru a salva un proiect nou în baza de date.
     */
    public void creeazaProiect() {
        try {
            // Setăm automat coordonatorul proiectului ca fiind utilizatorul logat
            proiectNou.setCoordonatorProiect(this.coordonatorProfile);

            // Asociem proiectul cu organizația coordonatorului
            proiectNou.setOrganizatie(this.organizatieCurenta);

            proiectService.adaugaProiect(proiectNou);
            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Proiectul a fost creat.");
            // Reîncărcăm lista de proiecte și resetăm formularul
            loadDashboardData();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Eroare la creare", e.getMessage());
        }
    }

    /**
     * Apelează serviciul pentru a actualiza datele unui proiect existent.
     */
    public void actualizeazaProiect() {
        if (proiectSelectat == null) return;
        try {
            proiectService.actualizeazaProiect(proiectSelectat);
            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Proiectul a fost actualizat.");
            loadDashboardData(); // Reîmprospătare listă
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Eroare la actualizare", e.getMessage());
        }
    }


    /**
     * Returnează valorile din Enum pentru a le folosi în dropdown-uri.
     */
    public StatusProiect[] getProiectStatusValues() {
        return StatusProiect.values();
    }
    /**
     * Metodă nouă, apelată de dropdown-ul de status.
     * Primește direct obiectul 'grupare' actualizat de JSF cu noua valoare.
     */
    public void gestioneazaAplicatieDropdown(GrupareVoluntariProiecte grupare) {
        if (grupare == null) {
            return;
        }
        try {
            // Apelăm serviciul cu noul status direct din obiect
            proiectService.gestioneazaAplicatie(grupare.getId(), grupare.getStatusAplicatie());

            // Reîncărcăm datele
            loadDashboardData();

            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Statusul pentru " + grupare.getVoluntar().getNumeComplet() + " a fost actualizat.");

        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu s-a putut modifica statusul aplicației: " + e.getMessage());
        }
    }

    /**
     * Returnează valorile din Enum pentru a le folosi în dropdown.
     */
    public StatusAplicari[] getStatusAplicariValues() {
        return StatusAplicari.values();
    }
    // ==========================================================
    // === METODE NOI PENTRU MANAGEMENTUL TASK-URILOR ===
    // ==========================================================

    public void pregatesteEditareProiectSiTaskuri(Proiect proiect) {
        // Când edităm un proiect, încărcăm și lista de voluntari acceptați și taskurile
        this.proiectSelectat = proiect;
        this.voluntariAcceptatiInProiect = proiectService.getVoluntariAcceptatiInProiect(proiect.getId());
        // Forțăm încărcarea listei de task-uri, dacă nu a fost deja încărcată
        this.proiectSelectat.setTaskuri(taskService.findByProiect(proiect.getId()));
    }

    public void pregatesteTaskNou() {
        this.taskNou = new Task();
        this.taskNou.setProiect(this.proiectSelectat);
        // Putem preseta un deadline, de ex. o săptămână de acum
        this.taskNou.setDeadline(LocalDate.now().plusWeeks(1));
    }

    public void salveazaTask() {
        try {
            taskService.adaugaTask(taskNou);
            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Task-ul a fost creat și asignat.");
            // Reîmprospătăm datele
            pregatesteEditareProiectSiTaskuri(this.proiectSelectat);
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu s-a putut salva task-ul: " + e.getMessage());
        }
    }

    public void stergeTask(Task task) {
        try {
            taskService.stergeTask(task.getId());
            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Task-ul a fost șters.");
            // Reîmprospătăm datele
            pregatesteEditareProiectSiTaskuri(this.proiectSelectat);
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu s-a putut șterge task-ul: " + e.getMessage());
        }
    }

    // Metoda pentru a obține valorile din enum-ul de status task
    public ro.ong.corgi.model.Enums.TaskStatus[] getTaskStatusValues() {
        return ro.ong.corgi.model.Enums.TaskStatus.values();
    }
}