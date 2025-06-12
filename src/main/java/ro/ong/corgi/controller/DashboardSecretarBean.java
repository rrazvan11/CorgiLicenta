package ro.ong.corgi.controller;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import ro.ong.corgi.model.*;
import ro.ong.corgi.model.Enums.*;
import ro.ong.corgi.service.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Named
@ViewScoped
@Getter
@Setter
public class DashboardSecretarBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject private VoluntarService voluntarService;
    @Inject private OrganizatieService organizatieService;
    @Inject private UserService userService;
    @Inject private DepartamentService departamentService;
    @Inject private DocumentGenerationService documentGenerationService;
    @Inject private EmailService emailService;
    @Inject private ProiectService proiectService;
    @Inject private TaskService taskService;
    @Inject private SedintaService sedintaService; // Metode pentru sedinta
    @Inject private FacesContext facesContext;

    private User loggedInUser;
    private Organizatie currentOrganizatie;
    private List<Departament> departmentList;
    private String searchKeyword;

    private List<Voluntar> voluntariActivi;
    private List<Voluntar> voluntariColaboratori;
    private List<Voluntar> coordonatori;
    private List<Voluntar> voluntariInactivi;
    private List<Voluntar> allVolunteersInOrg;
    private List<Voluntar> voluntariNerepartizati;
    private List<Voluntar> voluntariCoordonatori;
    private Voluntar selectedVolunteerForReports;
    private Departament selectedDepartament;
    private List<Voluntar> membriDepartamentCurent;
    private List<Voluntar> voluntariDeAdaugat;
    private boolean editModeOrganizatie = false;

    // Secțiune Ședințe
    private Sedinta newSedinta;
    private Map<Long, StatusPrezenta> prezenteMap;
    private List<Sedinta> istoricSedinte;

    // Secțiune Export
    private StreamedContent fisierExportat;

    @PostConstruct
    public void init() {
        loggedInUser = (User) facesContext.getExternalContext().getSessionMap().get("loggedInUser");
        if (loggedInUser == null || loggedInUser.getRol() != Rol.SECRETAR) {
            try {
                facesContext.getExternalContext().redirect(
                        facesContext.getExternalContext().getRequestContextPath() + "/xhtml/login.xhtml");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        incarcaDateleInitiale();
    }

    public void incarcaDateleInitiale() {
        currentOrganizatie = organizatieService.cautaDupaUser(loggedInUser);
        if (currentOrganizatie != null) {
            allVolunteersInOrg = voluntarService.gasesteVoluntariDinOrganizatie(currentOrganizatie.getId());
            departmentList = departamentService.gasesteDepartamentePeOrganizatie(currentOrganizatie.getId());
            istoricSedinte = sedintaService.getToateSedintele();
            categorizeazaVoluntarii();
        } else {
            allVolunteersInOrg = Collections.emptyList();
            departmentList = Collections.emptyList();
            istoricSedinte = Collections.emptyList();
            categorizeazaVoluntarii();
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenție", "Contul nu este asociat unei organizații."));
        }
        searchKeyword = null;
        editModeOrganizatie = false;
    }

    private void categorizeazaVoluntarii() {
        // ... clasificarea voluntarilor ...
    }

    public void cautaVoluntari() {
        categorizeazaVoluntarii();
    }

    // Metode Organizatie, Departament, Voluntar ...

    // Metode Ședințe (NOU)
    public void deschideDialogPrezentaNoua() {
        newSedinta = new Sedinta();
        prezenteMap = new LinkedHashMap<>();
        if (allVolunteersInOrg != null) {
            for (Voluntar v : allVolunteersInOrg) {
                if (v.getUser().isActiv()) {
                    prezenteMap.put(v.getId(), StatusPrezenta.PREZENT);
                }
            }
        }
    }

    public void salveazaPrezenta() {
        try {
            sedintaService.creeazaSiInregistreazaPrezenta(newSedinta, prezenteMap);
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes", "Prezența a fost înregistrată."));
            incarcaDateleInitiale();
            PrimeFaces.current().executeScript("PF('attendanceDialog').hide()");
        } catch (Exception e) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu s-a putut salva prezența: " + e.getMessage()));
        }
    }

    public StatusPrezenta[] getStatusPrezentaValues() {
        return StatusPrezenta.values();
    }

    public List<Voluntar> getVoluntariActiviPentruPontaj() {
        if (allVolunteersInOrg == null) return new ArrayList<>();
        return allVolunteersInOrg.stream()
                .filter(v -> v.getUser().isActiv())
                .collect(Collectors.toList());
    }

    // Metode Export (NOU)
    public void exportaListaMembriDepartament() {
        if (selectedDepartament == null || membriDepartamentCurent == null || membriDepartamentCurent.isEmpty()) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenție",
                            "Selectează un departament cu membri pentru a exporta."));
            return;
        }
        try {
            byte[] pdfData = documentGenerationService.genereazaRaportDepartamentPdf(
                    selectedDepartament, membriDepartamentCurent);
            String numeFisier = "Raport_Departament_" +
                    selectedDepartament.getNume().replaceAll("\\s+", "_") + ".pdf";
            fisierExportat = DefaultStreamedContent.builder()
                    .name(numeFisier)
                    .contentType("application/pdf")
                    .stream(() -> new ByteArrayInputStream(pdfData))
                    .build();
        } catch (Exception e) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Eroare la export", e.getMessage()));
            e.printStackTrace();
        }
    }
}
