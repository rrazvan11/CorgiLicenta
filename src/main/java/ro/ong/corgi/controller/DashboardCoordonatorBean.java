package ro.ong.corgi.controller;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import ro.ong.corgi.dto.SedintaDTO;
import ro.ong.corgi.model.*;
import ro.ong.corgi.model.Enums.StatusPrezenta;
import ro.ong.corgi.model.Enums.TipSedinta;
import ro.ong.corgi.repository.PrezentaSedintaRepository;
import ro.ong.corgi.service.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Named
@ViewScoped
@Getter
@Setter
public class DashboardCoordonatorBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject private VoluntarService voluntarService;
    @Inject private DepartamentService departamentService;
    @Inject private SedintaService sedintaService;

    private User loggedInUser;
    private Voluntar coordonatorProfile;
    private Departament departamentCoordonat;

    // Proprietăți pentru tab-ul de prezență
    private List<SedintaDTO> sedinteDepartament = new ArrayList<>();
    private List<Voluntar> voluntariDepartament = new ArrayList<>();
    private Sedinta sedintaDepartamentNoua;
    private Map<Long, StatusPrezenta> prezenteDepartamentMap = new HashMap<>();
    private SedintaDTO sedintaSelectata;
    private boolean editModePrezenta = false;

    @PostConstruct
    public void init() {
        this.loggedInUser = (User) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("loggedInUser");

        if (loggedInUser == null) {
            redirectToLogin();
            return;
        }

        this.coordonatorProfile = voluntarService.cautaDupaUser(loggedInUser);
        if (this.coordonatorProfile == null) {
            System.err.println("Eroare: Utilizatorul nu are un profil de voluntar asociat.");
            redirectToLogin();
            return;
        }

        this.departamentCoordonat = departamentService.findByCoordonator(this.coordonatorProfile);
        if (this.departamentCoordonat != null) {
            // Încărcăm datele necesare pentru dashboard
            this.voluntariDepartament = voluntarService.gasesteVoluntariDinDepartament(this.departamentCoordonat.getId());
            this.sedinteDepartament = sedintaService.getSedinteInfoPentruDepartament(this.departamentCoordonat.getId());
        } else {
            System.err.println("Atenție: Voluntarul " + this.coordonatorProfile.getNumeComplet() + " nu este coordonatorul niciunui departament.");
        }
    }

    // --- Metode pentru tab-ul de prezență ---

    public StatusPrezenta[] getStatusPrezentaValues() {
        return StatusPrezenta.values();
    }

    public void pregatesteSedintaNoua() {
        this.sedintaDepartamentNoua = new Sedinta();
        this.sedintaDepartamentNoua.setDescriere("Ședință departament " + this.departamentCoordonat.getNume());
        this.sedintaDepartamentNoua.setDepartament(this.departamentCoordonat);
        this.sedintaDepartamentNoua.setOrganizatie(this.departamentCoordonat.getOrganizatie());
        this.sedintaDepartamentNoua.setTipSedinta(TipSedinta.DEPARTAMENT);

        prezenteDepartamentMap.clear();
        if (this.voluntariDepartament != null) {
            for (Voluntar v : this.voluntariDepartament) {
                prezenteDepartamentMap.put(v.getId(), StatusPrezenta.ABSENT);
            }
        }
    }

    public void salveazaPrezentaDepartament() {
        try {
            sedintaService.creeazaSiInregistreazaPrezenta(sedintaDepartamentNoua, prezenteDepartamentMap);
            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Prezența pentru ședința de departament a fost salvată.");
            // Reîncărcăm lista de ședințe pentru a o afișa pe cea nouă
            this.sedinteDepartament = sedintaService.getSedinteInfoPentruDepartament(this.departamentCoordonat.getId());
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu s-a putut salva prezența: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void executaStergereSedinta() {
        if (sedintaSelectata == null || sedintaSelectata.getSedinta() == null) {
            addMessage(FacesMessage.SEVERITY_WARN, "Atenție", "Nicio ședință selectată pentru ștergere.");
            return;
        }
        try {
            sedintaService.stergeSedintaSiPrezentele(sedintaSelectata.getSedinta().getId());
            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Ședința a fost ștearsă cu succes.");
            // Reîncărcăm datele
            this.sedinteDepartament = sedintaService.getSedinteInfoPentruDepartament(this.departamentCoordonat.getId());
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Eroare la ștergere", e.getMessage());
        }
    }

    // --- Metode Utilitare ---

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
    }

    private void redirectToLogin() {
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(
                    FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/xhtml/login.xhtml"
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}