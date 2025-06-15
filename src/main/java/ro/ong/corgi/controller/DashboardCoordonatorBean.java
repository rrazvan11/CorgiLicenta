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
import ro.ong.corgi.model.Enums.StatusPrezenta;
import ro.ong.corgi.model.Enums.TipSedinta;
import ro.ong.corgi.repository.PrezentaSedintaRepository;
import ro.ong.corgi.service.DepartamentService;
import ro.ong.corgi.service.SedintaService;
import ro.ong.corgi.service.VoluntarService;

import java.io.IOException;
import java.io.Serializable;
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
    @Inject private PrezentaSedintaRepository prezentaSedintaRepository;

    private User loggedInUser;
    private Voluntar coordonatorProfile;
    private Departament departamentCoordonat;

    private List<SedintaDTO> sedinteDepartament = new ArrayList<>();
    private List<Voluntar> voluntariDepartament = new ArrayList<>();
    private Sedinta sedintaCurenta;
    private Map<Long, StatusPrezenta> prezenteDepartamentMap = new HashMap<>();
    private boolean editModePrezenta = false;
    private SedintaDTO sedintaSelectata;

    @PostConstruct
    public void init() {
        this.loggedInUser = (User) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("loggedInUser");
        if (loggedInUser == null) { redirectToLogin(); return; }

        this.coordonatorProfile = voluntarService.cautaDupaUser(loggedInUser);
        if (this.coordonatorProfile == null) { redirectToLogin(); return; }

        this.departamentCoordonat = departamentService.findByCoordonator(this.coordonatorProfile);
        if (this.departamentCoordonat != null) {
            this.voluntariDepartament = voluntarService.gasesteVoluntariDinDepartament(this.departamentCoordonat.getId());
            this.sedinteDepartament = sedintaService.getSedinteInfoPentruDepartament(this.departamentCoordonat.getId());
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
}