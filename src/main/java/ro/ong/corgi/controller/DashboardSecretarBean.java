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
import ro.ong.corgi.model.Enums.*;
import ro.ong.corgi.service.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
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
    @Inject private FacesContext facesContext;
    @Inject private DocumentGenerationService documentGenerationService;
    @Inject private EmailService emailService;

    private User loggedInUser;
    private Organizatie currentOrganizatie;
    private List<Voluntar> allVolunteers;
    private List<Voluntar> filteredVolunteers;
    private Voluntar selectedVoluntar;
    private Voluntar selectedVolunteerForReports;
    private String searchKeyword;
    private List<Departament> departmentList;

    @PostConstruct
    public void init() {
        loggedInUser = (User) facesContext.getExternalContext().getSessionMap().get("loggedInUser");

        if (loggedInUser == null || loggedInUser.getRol() != Rol.SECRETAR) {
            try {
                facesContext.getExternalContext().redirect(facesContext.getExternalContext().getRequestContextPath() + "/xhtml/login.xhtml");
            } catch (IOException e) { /* Ignored */ }
            return;
        }
        loadInitialData();
    }

    public void loadInitialData() {
        currentOrganizatie = organizatieService.cautaDupaUser(loggedInUser);

        if (currentOrganizatie != null) {
            allVolunteers = voluntarService.gasesteVoluntariDinOrganizatie(currentOrganizatie.getId());
            departmentList = departamentService.gasesteDepartamentePeOrganizatie(currentOrganizatie.getId());
        } else {
            allVolunteers = Collections.emptyList();
            departmentList = Collections.emptyList();
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenție", "Contul de secretar nu este asociat niciunei organizații."));
        }

        filteredVolunteers = allVolunteers;
        searchKeyword = null;
    }

    public void onDepartmentChange(Voluntar voluntar) {
        voluntarService.actualizeazaVoluntar(voluntar);
        facesContext.addMessage(null, new FacesMessage("Succes", voluntar.getPrenume() + " a fost mutat în departamentul " + voluntar.getDepartament().getNume()));
        loadInitialData();
    }

    public void searchVolunteers() {
        if (searchKeyword == null || searchKeyword.trim().isEmpty()) {
            filteredVolunteers = allVolunteers;
            return;
        }
        String lowerKeyword = searchKeyword.toLowerCase();
        filteredVolunteers = allVolunteers.stream()
                .filter(v -> v.getNume().toLowerCase().contains(lowerKeyword) || v.getPrenume().toLowerCase().contains(lowerKeyword) || v.getUser().getEmail().toLowerCase().contains(lowerKeyword) || v.getUser().getUsername().toLowerCase().contains(lowerKeyword))
                .collect(Collectors.toList());
    }

    public List<Voluntar> getVoluntariActivi() { if (filteredVolunteers == null) return Collections.emptyList(); return filteredVolunteers.stream().filter(v -> v.getUser().isActiv() && v.getStatus() == Status.ACTIV && v.getUser().getRol() != Rol.COORDONATOR).collect(Collectors.toList()); }
    public List<Voluntar> getVoluntariColaboratori() { if (filteredVolunteers == null) return Collections.emptyList(); return filteredVolunteers.stream().filter(v -> v.getUser().isActiv() && v.getStatus() == Status.COLABORATOR).collect(Collectors.toList()); }
    public List<Voluntar> getCoordonatori() { if (filteredVolunteers == null) return Collections.emptyList(); return filteredVolunteers.stream().filter(v -> v.getUser().isActiv() && v.getUser().getRol() == Rol.COORDONATOR).collect(Collectors.toList()); }

    public Status[] getStatusValues() { return new Status[]{Status.ACTIV, Status.COLABORATOR}; }
    public Facultate[] getFacultateValues() { return Facultate.values(); }
    public AnStudiu[] getAnStudiuValues() { return AnStudiu.values(); }

    public void onStatusChange(Voluntar voluntar) { voluntarService.actualizeazaVoluntar(voluntar); loadInitialData(); }
    public void onRoleChange(Voluntar voluntar) { if (voluntar.getUser().getRol() == Rol.COORDONATOR && voluntar.getStatus() != Status.ACTIV) { voluntar.setStatus(Status.ACTIV); voluntarService.actualizeazaVoluntar(voluntar); } userService.changeUserRole(voluntar.getUser().getId(), voluntar.getUser().getRol()); loadInitialData(); }

    public void deschideEditareDialog(Voluntar voluntar) { this.selectedVoluntar = voluntarService.cautaDupaId(voluntar.getId()); }
    public void salveazaModificariVoluntar() { voluntarService.actualizeazaVoluntar(selectedVoluntar); facesContext.addMessage(null, new FacesMessage("Succes", "Datele voluntarului au fost salvate.")); loadInitialData(); }
    public void toggleAccountStatus() { User user = selectedVoluntar.getUser(); if (user.isActiv()) { userService.dezactiveazaCont(user.getId()); } else { userService.reactiveazaCont(user.getId()); } facesContext.addMessage(null, new FacesMessage("Succes", "Starea contului a fost modificată.")); loadInitialData(); }

    public void sendCertificateByEmail() {
        if (selectedVolunteerForReports == null) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenție", "Te rugăm să selectezi un voluntar din tabel."));
            return;
        }
        try {
            byte[] pdfData = documentGenerationService.genereazaCertificatPdf(selectedVolunteerForReports, currentOrganizatie);
            String fileName = "Certificat_" + selectedVolunteerForReports.getNume() + "_" + selectedVolunteerForReports.getPrenume() + ".pdf";
            emailService.sendEmailWithAttachment(selectedVolunteerForReports.getUser().getEmail(), "Certificat de Voluntariat", pdfData, fileName);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes!", "Certificatul a fost trimis pe adresa de email a voluntarului."));
        } catch (Exception e) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu s-a putut trimite certificatul: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    public void sendActivityReportByEmail() {
        if (selectedVolunteerForReports == null) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenție", "Te rugăm să selectezi un voluntar din tabel."));
            return;
        }
        try {
            byte[] pdfData = documentGenerationService.genereazaCertificatPdf(selectedVolunteerForReports, currentOrganizatie);
            String fileName = "Raport_Activitate_" + selectedVolunteerForReports.getNume() + "_" + selectedVolunteerForReports.getPrenume() + ".pdf";
            emailService.sendEmailWithAttachment(selectedVolunteerForReports.getUser().getEmail(), "Raport de Activitate", pdfData, fileName);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes!", "Raportul de activitate a fost trimis cu succes."));
        } catch (Exception e) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu s-a putut trimite raportul: " + e.getMessage()));
            e.printStackTrace();
        }
    }
}
