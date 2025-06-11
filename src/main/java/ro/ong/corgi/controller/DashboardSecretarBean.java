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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Named
@ViewScoped
@Getter
@Setter
public class DashboardSecretarBean implements Serializable {

    private static final long serialVersionUID = 1L;

    // --- Servicii Injectate ---
    @Inject private VoluntarService voluntarService;
    @Inject private OrganizatieService organizatieService;
    @Inject private UserService userService;
    @Inject private DepartamentService departamentService;
    @Inject private DocumentGenerationService documentGenerationService;
    @Inject private EmailService emailService;
    @Inject private SedintaService sedintaService;
    @Inject private FacesContext facesContext;

    // --- Date Generale ---
    private User loggedInUser;
    private Organizatie currentOrganizatie;
    private List<Departament> departmentList;
    private String searchKeyword;
    private boolean editModeOrganizatie = false;

    // --- Liste Voluntari ---
    private List<Voluntar> voluntariActivi;
    private List<Voluntar> voluntariColaboratori;
    private List<Voluntar> coordonatori;
    private List<Voluntar> voluntariInactivi;
    private List<Voluntar> allVolunteersInOrg;
    private List<Voluntar> voluntariNerepartizati;
    private List<Voluntar> voluntariCoordonatori;

    // --- Management Departamente ---
    private Departament selectedDepartament;
    private List<Voluntar> membriDepartamentCurent;
    private List<Voluntar> voluntariDeAdaugat;

    // --- NOU: Secțiune Prezențe ---
    private Sedinta sedintaCurenta;
    private List<PrezentaWrapper> listaPrezente;
    private List<Sedinta> istoricSedinte;
    private boolean attendanceMode = false;


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
            istoricSedinte = sedintaService.getSedinteByOrganizatie(currentOrganizatie.getId());
            categorizeazaVoluntarii();
        } else {
            // MODIFICAT: Am separat asignările pentru a evita erorile de tip.
            allVolunteersInOrg = Collections.emptyList();
            departmentList = Collections.emptyList();
            istoricSedinte = Collections.emptyList();
            categorizeazaVoluntarii();
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenție", "Contul nu este asociat unei organizații."));
        }
        searchKeyword = null;
        editModeOrganizatie = false;
        attendanceMode = false;
    }

    private void categorizeazaVoluntarii() {
        if (allVolunteersInOrg == null || allVolunteersInOrg.isEmpty()) {
            voluntariActivi = voluntariColaboratori = coordonatori = voluntariInactivi = voluntariNerepartizati = voluntariCoordonatori = new ArrayList<>();
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
        voluntariNerepartizati = toCategorize.stream().filter(v -> v.getDepartament() == null && v.getUser().getRol() != Rol.COORDONATOR).collect(Collectors.toList());
        voluntariCoordonatori = allVolunteersInOrg.stream()
                .filter(v -> v.getUser().getRol() == Rol.COORDONATOR && v.getUser().isActiv())
                .collect(Collectors.toList());
    }

    public void cautaVoluntari() {
        categorizeazaVoluntarii();
    }

    //<editor-fold desc="Metode Management Organizatie & Departamente (existente)">
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
                Departament savedDept = departamentService.creeazaDepartament(selectedDepartament, loggedInUser);
                this.departmentList.add(savedDept);
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes", "Departamentul '" + savedDept.getNume() + "' a fost adăugat."));
            } else {
                Departament updatedDept = departamentService.actualizeazaDepartament(selectedDepartament, loggedInUser);
                this.departmentList.removeIf(d -> d.getId().equals(updatedDept.getId()));
                this.departmentList.add(updatedDept);
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes", "Departamentul '" + updatedDept.getNume() + "' a fost actualizat."));
            }
            this.selectedDepartament = new Departament();
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

    public void vizualizeazaMembriDepartament(Departament dept) {
        this.selectedDepartament = dept;
        if (dept != null && dept.getId() != null) {
            this.membriDepartamentCurent = voluntarService.gasesteVoluntariDinDepartament(dept.getId());
        } else {
            this.membriDepartamentCurent = new ArrayList<>();
        }
        this.voluntariDeAdaugat = new ArrayList<>();
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
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes", voluntariDeAdaugat.size() + " membri au fost adăugați."));
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
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes", voluntar.getNumeComplet() + " a fost scos din departament."));
        } catch (Exception e) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu s-a putut scoate voluntarul din departament."));
        }
    }
    //</editor-fold>

    //<editor-fold desc="Metode Management Status & Roluri Voluntari (existente)">
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
    //</editor-fold>

    // --- NOU: Funcționalități Rapoarte ---
    public void genereazaSiTrimiteRaportDepartament(Departament departament) {
        if (departament == null) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenție", "Trebuie să selectați un departament."));
            return;
        }
        try {
            List<Voluntar> voluntariDinDepartament = voluntarService.gasesteVoluntariDinDepartament(departament.getId());
            if (voluntariDinDepartament.isEmpty()) {
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", "Departamentul selectat nu are membri."));
                return;
            }
            byte[] pdfData = documentGenerationService.genereazaRaportDepartamentPdf(departament, voluntariDinDepartament, currentOrganizatie);
            String fileName = "Raport_Departament_" + departament.getNume().replaceAll("\\s+", "_") + ".pdf";

            emailService.sendEmailWithAttachment(loggedInUser.getEmail(), "Raport Voluntari Departament: " + departament.getNume(), pdfData, fileName);

            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes!", "Raportul pentru departamentul '" + departament.getNume() + "' a fost trimis pe emailul dvs."));
        } catch (Exception e) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu s-a putut genera sau trimite raportul: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    // --- NOU: Funcționalități Prezențe ---
    public void initiazaFoaiePrezenta() {
        this.sedintaCurenta = new Sedinta();
        this.sedintaCurenta.setDataSedinta(LocalDateTime.now());
        this.sedintaCurenta.setOrganizatie(this.currentOrganizatie);
        this.sedintaCurenta.setDescriere("Ședință Generală");

        this.listaPrezente = allVolunteersInOrg.stream()
                .filter(v -> v.getUser().isActiv()) // Doar voluntarii activi
                .map(v -> new PrezentaWrapper(v, StatusPrezenta.ABSENT)) // Default to ABSENT
                .collect(Collectors.toList());

        this.attendanceMode = true;
    }

    public void anuleazaFoaiePrezenta() {
        this.attendanceMode = false;
        this.listaPrezente = null;
        this.sedintaCurenta = null;
    }

    public void salveazaFoaiePrezenta() {
        if (sedintaCurenta == null || listaPrezente == null || listaPrezente.isEmpty()) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu există date de prezență pentru a fi salvate."));
            return;
        }

        try {
            List<PrezentaSedinta> entitatiPrezenta = new ArrayList<>();
            for (PrezentaWrapper wrapper : listaPrezente) {
                entitatiPrezenta.add(PrezentaSedinta.builder()
                        .voluntar(wrapper.getVoluntar())
                        .statusPrezenta(wrapper.getStatus())
                        .dataInregistrare(LocalDateTime.now())
                        .build());
            }

            sedintaService.creeazaSedintaCuPrezente(sedintaCurenta, entitatiPrezenta);
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes", "Prezența a fost înregistrată și punctele au fost acordate."));

            incarcaDateleInitiale(); // Reîncarcă tot, inclusiv istoricul ședințelor și punctajele voluntarilor
        } catch (Exception e) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare la Salvare", "Nu s-a putut salva prezența: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    public StatusPrezenta[] getStatusPrezentaValues() {
        return StatusPrezenta.values();
    }

    @Getter @Setter
    public static class PrezentaWrapper implements Serializable {
        private Voluntar voluntar;
        private StatusPrezenta status;

        public PrezentaWrapper(Voluntar voluntar, StatusPrezenta status) {
            this.voluntar = voluntar;
            this.status = status;
        }
    }
}