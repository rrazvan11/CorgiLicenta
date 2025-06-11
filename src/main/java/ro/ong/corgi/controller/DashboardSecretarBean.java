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
import ro.ong.corgi.model.*;
import ro.ong.corgi.model.Enums.*;
import ro.ong.corgi.service.*;

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
    @Inject private FacesContext facesContext;
    @Inject private ProiectService proiectService;
    @Inject private TaskService taskService;

    private User loggedInUser;
    private Organizatie currentOrganizatie;
    private List<Departament> departmentList;
    private String searchKeyword;

    private List<Voluntar> voluntariActivi;
    private List<Voluntar> voluntariColaboratori;
    private List<Voluntar> coordonatori;
    private List<Voluntar> voluntariInactivi;
    private List<Voluntar> allVolunteersInOrg;

    // MODIFICAT: Redenumit din 'voluntariNeasignati' în 'voluntariNerepartizati'
    private List<Voluntar> voluntariNerepartizati;

    private List<Voluntar> voluntariCoordonatori;
    private Voluntar selectedVolunteerForReports;
    private Departament selectedDepartament;
    private List<Voluntar> membriDepartamentCurent;
    private List<Voluntar> voluntariDeAdaugat;
    private boolean editModeOrganizatie = false;

    @PostConstruct
    public void init() {
        loggedInUser = (User) facesContext.getExternalContext().getSessionMap().get("loggedInUser");
        if (loggedInUser == null || loggedInUser.getRol() != Rol.SECRETAR) {
            try {
                facesContext.getExternalContext().redirect(facesContext.getExternalContext().getRequestContextPath() + "/xhtml/login.xhtml");
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
            categorizeazaVoluntarii();
        } else {
            allVolunteersInOrg = Collections.emptyList();
            departmentList = Collections.emptyList();
            categorizeazaVoluntarii();
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenție", "Contul nu este asociat unei organizații."));
        }
        searchKeyword = null;
        editModeOrganizatie = false;
    }

    private void categorizeazaVoluntarii() {
        if (allVolunteersInOrg == null || allVolunteersInOrg.isEmpty()) {
            voluntariActivi = voluntariColaboratori = coordonatori = voluntariInactivi = voluntariNerepartizati = new ArrayList<>();
            return;
        }

        List<Voluntar> activeVolunteers = allVolunteersInOrg.stream().filter(v -> v.getUser().isActiv()).collect(Collectors.toList());
        voluntariInactivi = allVolunteersInOrg.stream().filter(v -> !v.getUser().isActiv()).collect(Collectors.toList());

        List<Voluntar> toCategorize = activeVolunteers;
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            String lowerKeyword = searchKeyword.toLowerCase();
            toCategorize = activeVolunteers.stream()
                    .filter(v -> v.getNume().toLowerCase().contains(lowerKeyword) ||
                            v.getPrenume().toLowerCase().contains(lowerKeyword) ||
                            v.getUser().getEmail().toLowerCase().contains(lowerKeyword))
                    .collect(Collectors.toList());
        }

        coordonatori = toCategorize.stream().filter(v -> v.getUser().getRol() == Rol.COORDONATOR).collect(Collectors.toList());
        voluntariActivi = toCategorize.stream().filter(v -> v.getStatus() == Status.ACTIV && v.getUser().getRol() != Rol.COORDONATOR && v.getDepartament() != null).collect(Collectors.toList());
        voluntariColaboratori = toCategorize.stream().filter(v -> v.getStatus() == Status.COLABORATOR && v.getUser().getRol() != Rol.COORDONATOR && v.getDepartament() != null).collect(Collectors.toList());

        // MODIFICAT: Folosește noua denumire a listei
        voluntariNerepartizati = toCategorize.stream().filter(v -> v.getDepartament() == null && v.getUser().getRol() != Rol.COORDONATOR).collect(Collectors.toList());

        voluntariCoordonatori = allVolunteersInOrg.stream()
                .filter(v -> v.getUser().getRol() == Rol.COORDONATOR && v.getUser().isActiv())
                .collect(Collectors.toList());
    }

    public void cautaVoluntari() {
        categorizeazaVoluntarii();
    }

    public void activeazaModEditareOrganizatie() {
        this.editModeOrganizatie = true;
    }

    public void anuleazaEditareOrganizatie() {
        this.editModeOrganizatie = false;
        this.currentOrganizatie = organizatieService.cautaDupaId(this.currentOrganizatie.getId());
    }

    public void salveazaDetaliiOrganizatie() {
        try {
            organizatieService.actualizeazaOrganizatie(currentOrganizatie);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes", "Datele organizației au fost actualizate."));
            this.editModeOrganizatie = false;
        } catch (Exception e) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu s-au putut salva datele: " + e.getMessage()));
        }
    }

    public void deschideDialogDepartamentNou() {
        this.selectedDepartament = new Departament();
        this.selectedDepartament.setOrganizatie(currentOrganizatie);
    }

    public void salveazaDepartament() {
        try {
            if (selectedDepartament.getId() == null) {
                departamentService.creeazaDepartament(selectedDepartament, loggedInUser);
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes", "Departamentul a fost adăugat."));
            } else {
                departamentService.actualizeazaDepartament(selectedDepartament, loggedInUser);
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes", "Departamentul a fost actualizat."));
            }
            incarcaDateleInitiale();
            PrimeFaces.current().executeScript("PF('manageDepartmentDialog').hide()");
        } catch (Exception e) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare la salvare", e.getMessage()));
        }
    }

    public void stergeDepartament(Departament departament) {
        try {
            departamentService.stergeDepartament(departament.getId(), loggedInUser);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes", "Departamentul a fost șters."));
            incarcaDateleInitiale();
        } catch (Exception e) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare la ștergere", e.getMessage()));
        }
    }

    public void laSchimbareaDepartamentului(Voluntar voluntar) {
        try {
            voluntarService.actualizeazaVoluntar(voluntar);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes", voluntar.getPrenume() + " a fost mutat."));
            incarcaDateleInitiale();
        } catch (Exception e) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu s-a putut schimba departamentul."));
        }
    }

    public void laSchimbareaStatusului(Voluntar voluntar) {
        try {
            voluntarService.actualizeazaVoluntar(voluntar);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes", "Statusul a fost schimbat."));
            incarcaDateleInitiale();
        } catch (Exception e) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu s-a putut schimba statusul."));
        }
    }

    public void laSchimbareaRolului(Voluntar voluntar) {
        try {
            if (voluntar.getUser().getRol() == Rol.COORDONATOR) {
                voluntar.setStatus(Status.ACTIV);
                voluntarService.actualizeazaVoluntar(voluntar);
            }
            userService.changeUserRole(voluntar.getUser().getId(), voluntar.getUser().getRol());
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes", "Rolul a fost schimbat."));
            incarcaDateleInitiale();
        } catch (Exception e) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu s-a putut schimba rolul."));
        }
    }

    public void comutaStareCont(Voluntar voluntar) {
        try {
            User user = voluntar.getUser();
            if (user.isActiv()) {
                userService.dezactiveazaCont(user.getId());
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes", "Contul a fost dezactivat."));
            } else {
                userService.reactiveazaCont(user.getId());
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes", "Contul a fost reactivat."));
            }
            incarcaDateleInitiale();
        } catch (Exception e) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu s-a putut modifica starea contului."));
        }
    }

    public void trimiteCertificatPeEmail() {
        if (selectedVolunteerForReports == null) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenție", "Te rugăm să selectezi un voluntar."));
            return;
        }
        try {
            byte[] pdfData = documentGenerationService.genereazaCertificatPdf(selectedVolunteerForReports, currentOrganizatie);
            String fileName = "Certificat_" + selectedVolunteerForReports.getNume() + "_" + selectedVolunteerForReports.getPrenume() + ".pdf";
            emailService.sendEmailWithAttachment(selectedVolunteerForReports.getUser().getEmail(), "Certificat de Voluntariat", pdfData, fileName);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes!", "Certificatul a fost trimis."));
        } catch (Exception e) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu s-a putut trimite certificatul: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    public void trimiteRaportActivitatePeEmail() {
        if (selectedVolunteerForReports == null) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenție", "Te rugăm să selectezi un voluntar."));
            return;
        }
        try {
            List<Proiect> proiectele = proiectService.gasesteProiecteDupaVoluntarId(selectedVolunteerForReports.getId());
            Map<Proiect, List<Task>> activitatiGrupate = new LinkedHashMap<>();
            for (Proiect p : proiectele) {
                List<Task> taskuriFinalizate = taskService.findByProiect(p.getId())
                        .stream()
                        .filter(t -> t.getVoluntar().getId().equals(selectedVolunteerForReports.getId()) && t.getStatus() == TaskStatus.DONE)
                        .collect(Collectors.toList());
                if (!taskuriFinalizate.isEmpty()) {
                    activitatiGrupate.put(p, taskuriFinalizate);
                }
            }
            if (activitatiGrupate.isEmpty()) {
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", "Nu există activități finalizate pentru raport."));
                return;
            }
            byte[] pdfData = documentGenerationService.genereazaRaportActivitatePdf(selectedVolunteerForReports, activitatiGrupate);
            String fileName = "Raport_Activitate_" + selectedVolunteerForReports.getNume() + "_" + selectedVolunteerForReports.getPrenume() + ".pdf";
            emailService.sendEmailWithAttachment(selectedVolunteerForReports.getUser().getEmail(), "Raport de Activitate", pdfData, fileName);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes!", "Raportul de activitate a fost trimis."));
        } catch (Exception e) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu s-a putut trimite raportul: " + e.getMessage()));
            e.printStackTrace();
        }
    }
    public void adaugaMembriSelectatiLaDepartament() {
        if (voluntariDeAdaugat == null || voluntariDeAdaugat.isEmpty()) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenție", "Niciun voluntar nu a fost selectat."));
            return;
        }

        try {
            for (Voluntar vol : voluntariDeAdaugat) {
                vol.setDepartament(selectedDepartament);
                voluntarService.actualizeazaVoluntar(vol);
            }

            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes",
                    voluntariDeAdaugat.size() + " membri au fost adăugați."));

            vizualizeazaMembriDepartament(this.selectedDepartament);
            incarcaDateleInitiale();
            PrimeFaces.current().executeScript("PF('addMemberDialog').hide()");

        } catch (Exception e) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Membrii nu au putut fi adăugați."));
        }
    }
    public void scoateVoluntarDinDepartament(Voluntar voluntar) {
        try {
            voluntar.setDepartament(null);
            voluntarService.actualizeazaVoluntar(voluntar);

            vizualizeazaMembriDepartament(this.selectedDepartament);
            incarcaDateleInitiale();

            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes",
                    voluntar.getNumeComplet() + " a fost scos din departament."));
        } catch (Exception e) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare",
                    "Nu s-a putut scoate voluntarul din departament."));
        }
    }
    public void vizualizeazaMembriDepartament(Departament dept) {
        this.selectedDepartament = dept;
        if (dept != null && dept.getId() != null) {
            this.membriDepartamentCurent = voluntarService.gasesteVoluntariDinDepartament(dept.getId());
        } else {
            this.membriDepartamentCurent = new ArrayList<>();
        }
        this.voluntariDeAdaugat = new ArrayList<>();
    }
}