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
import ro.ong.corgi.model.Enums.Rol;
import ro.ong.corgi.model.Enums.Status;
import ro.ong.corgi.model.Enums.StatusPrezenta;
import ro.ong.corgi.service.*;
import ro.ong.corgi.model.Enums.TipSedinta;
import ro.ong.corgi.dto.SedintaDTO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Named
@ViewScoped
@Getter
@Setter
public class DashboardSecretarBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject private OrganizatieService organizatieService;
    @Inject private VoluntarService voluntarService;
    @Inject private DepartamentService departamentService;
    @Inject private SedintaService sedintaService;
    @Inject private DocumentGenerationService documentGenerationService;


    // Proprietăți pentru View
    private Departament selectedDepartament;
    private Organizatie organizatie;
    private List<Voluntar> voluntari;
    private List<Voluntar> filteredVoluntari;
    private Voluntar selectedVoluntar;
    private List<Departament> departamente;
    private Departament newDepartament = new Departament();
    private boolean editMode = false;
    private Sedinta sedintaCurenta;
    private Map<Long, StatusPrezenta> prezenteMap = new HashMap<>();
    private List<SedintaDTO> sedinteDTO;
    private boolean editModePrezenta = false;
    private SedintaDTO selectedSedintaDTO;

    @PostConstruct
    public void init() {
        User loggedInUser = (User) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("loggedInUser");
        if (loggedInUser != null && loggedInUser.getRol() == Rol.SECRETAR) {
            this.organizatie = organizatieService.cautaDupaUser(loggedInUser);
            if (this.organizatie != null) {
                // Încărcăm totul o singură dată, corect
                this.voluntari = voluntarService.gasesteVoluntariDinOrganizatie(this.organizatie.getId());
                this.departamente = departamentService.gasesteDepartamentePeOrganizatie(this.organizatie.getId());
                // Apelăm noua metodă care returnează lista de DTO-uri
                this.sedinteDTO = sedintaService.getSedinteInfoPentruOrganizatie(this.organizatie.getId());
            } else {
                handleError("Nu a fost găsită nicio organizație pentru acest cont de secretar.");
            }
        } else {
            redirectToLogin();
        }
    }
    // --- Management Organizație ---
    public void activeazaEditare() { this.editMode = true; }
    public void anuleazaEditare() {
        this.editMode = false;
        this.organizatie = organizatieService.cautaDupaUser(organizatie.getUser());
    }
    public void salveazaDateOrganizatie() {
        try {
            organizatieService.actualizeazaOrganizatie(organizatie);
            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Datele organizației au fost actualizate.");
            this.editMode = false;
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu s-au putut salva datele: " + e.getMessage());
        }
    }

    // --- Management Departamente ---
    public void adaugaDepartament() {
        try {
            User loggedInUser = (User) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("loggedInUser");
            newDepartament.setOrganizatie(this.organizatie);
            departamentService.creeazaDepartament(newDepartament, loggedInUser);
            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Departamentul '" + newDepartament.getNume() + "' a fost creat.");
            this.departamente = departamentService.gasesteDepartamentePeOrganizatie(this.organizatie.getId());
            this.newDepartament = new Departament();
            PrimeFaces.current().ajax().update("departamentListForm", "addDepartamentForm");
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Eroare la creare", e.getMessage());
        }
    }
    // Adaugă aceste două metode noi în DashboardSecretarBean.java

    public void prepareEditDepartament(Departament departament) {
        // Căutăm o instanță proaspătă din baza de date pentru a o edita
        this.selectedDepartament = departamentService.cautaDupaId(departament.getId());
    }

    public void salveazaModificariDepartament() {
        try {
            User loggedInUser = (User) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("loggedInUser");
            departamentService.actualizeazaDepartament(this.selectedDepartament, loggedInUser);
            this.departamente = departamentService.gasesteDepartamentePeOrganizatie(this.organizatie.getId());
            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Departamentul a fost actualizat.");
            PrimeFaces.current().ajax().update("departamentListForm");
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu s-a putut actualiza departamentul: " + e.getMessage());
        }
    }

    // --- Management Voluntari ---
    public void prepareEdit(Voluntar voluntar) {
        this.selectedVoluntar = voluntarService.cautaDupaId(voluntar.getId());
    }
    public void salveazaModificariVoluntar() {
        try {
            voluntarService.actualizeazaVoluntar(this.selectedVoluntar);
            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Datele pentru " + selectedVoluntar.getNumeComplet() + " au fost salvate.");
            this.voluntari = voluntarService.gasesteVoluntariDinOrganizatie(this.organizatie.getId());
            PrimeFaces.current().ajax().update("voluntarForm");
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu s-au putut salva datele: " + e.getMessage());
        }
    }
    public Rol[] getRoluriAtribuibile() { return new Rol[]{Rol.VOLUNTAR, Rol.COORDONATOR}; }
    public Status[] getStatusValues() { return Status.values(); }


    // Adaugă această metodă nouă în DashboardSecretarBean.java

    // Renamed for clarity to avoid confusion with the parameter version
    public void stergereDepartament() {
        if (selectedDepartament == null) {
            return; // Safety check
        }
        try {
            User loggedInUser = (User) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("loggedInUser");
            departamentService.stergeDepartament(selectedDepartament.getId(), loggedInUser);

            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Departamentul '" + selectedDepartament.getNume() + "' a fost șters.");

            // Reîmprospătăm lista de departamente din bean
            this.departamente = departamentService.gasesteDepartamentePeOrganizatie(this.organizatie.getId());

            // Comandăm un update AJAX pe formularul listei
            PrimeFaces.current().ajax().update("departamentListForm");

        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Eroare", e.getMessage());
        }
    }
    // Adaugă această metodă nouă în bean
// Înlocuiește metoda existentă cu aceasta:
    public List<Voluntar> getCoordonatoriDisponibili() {
        if (this.voluntari == null || this.departamente == null) {
            return new ArrayList<>();
        }

        // Pasul 1: Creăm o listă cu ID-urile tuturor coordonatorilor care sunt deja asignați
        // unui departament.
        List<Long> coordonatoriOcupatiIds = this.departamente.stream()
                .filter(d -> d.getCoordonator() != null) // Luăm doar departamentele care au un coordonator
                .map(d -> d.getCoordonator().getId())      // Extragem ID-ul voluntarului-coordonator
                .collect(Collectors.toList());

        // Pasul 2: Filtrăm lista principală de voluntari
        return this.voluntari.stream()
                .filter(v -> {
                    // Condiția 1: Trebuie să aibă rolul de COORDONATOR
                    if (v.getUser().getRol() != Rol.COORDONATOR) {
                        return false;
                    }

                    // Condiția 2: Verificăm dacă este disponibil
                    boolean esteOcupat = coordonatoriOcupatiIds.contains(v.getId());

                    // Cazul special pentru EDITARE: dacă edităm un departament care DEJA
                    // îl are pe acest voluntar drept coordonator, trebuie să-l afișăm în listă.
                    if (selectedDepartament != null && selectedDepartament.getCoordonator() != null &&
                            selectedDepartament.getCoordonator().getId().equals(v.getId())) {
                        return true; // Îl lăsăm în listă, chiar dacă tehnic este "ocupat"
                    }

                    // În toate celelalte cazuri, îl returnăm doar dacă NU este ocupat.
                    return !esteOcupat;
                })
                .collect(Collectors.toList());
    }
    // --- Prezențe ---
    public void initPrezentaNoua() {
        this.editModePrezenta = false;
        sedintaCurenta = new Sedinta();
        sedintaCurenta.setDescriere("Adunare Generală");
        sedintaCurenta.setDataSedinta(LocalDateTime.now());
        sedintaCurenta.setTipSedinta(TipSedinta.ADUNARE_GENERALĂ);
        sedintaCurenta.setOrganizatie(this.organizatie);
        sedintaCurenta.setDepartament(null);
        prezenteMap.clear();
        if (voluntari != null) voluntari.forEach(v -> prezenteMap.put(v.getId(), StatusPrezenta.ABSENT));
    }
    public void salveazaPrezenta() {
        // ADAUGĂ ACEASTĂ LINIE PENTRU DEBUG
        System.out.println("--- DEBUG: Se salvează prezența. Conținutul hărții este: " + prezenteMap);

        try {
            sedintaService.creeazaSiInregistreazaPrezenta(sedintaCurenta, prezenteMap);
            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Prezența a fost salvată.");
            this.voluntari = voluntarService.gasesteVoluntariDinOrganizatie(this.organizatie.getId());
            this.sedinteDTO = sedintaService.getSedinteInfoPentruOrganizatie(this.organizatie.getId());
            PrimeFaces.current().ajax().update(":sedinteForm:sedinteTable", ":voluntarForm:voluntariTable", ":messages");
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_FATAL, "Eroare la salvare", "Nu s-a putut salva prezența: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // Adaugă această metodă în bean
    public StreamedContent genereazaRaportDepartamente() {
        // Apelăm noua metodă din repository care aduce toate datele necesare
        List<Departament> data = departamentService.gasesteToateDepartamenteleCuVoluntari();

        // Generăm PDF-ul
        byte[] pdfBytes = documentGenerationService.genereazaRaportDepartamente(data);

        // Îl oferim pentru descărcare
        return DefaultStreamedContent.builder()
                .name("Raport_Departamente.pdf")
                .contentType("application/pdf")
                .stream(() -> new ByteArrayInputStream(pdfBytes))
                .build();
    }
    public StatusPrezenta[] getStatusPrezentaValues() { return StatusPrezenta.values(); }

    // În DashboardSecretarBean.java
    public String executaStergereVoluntar() {
        if (selectedVoluntar == null) {
            return null;
        }
        try {
            voluntarService.stergeVoluntar(selectedVoluntar.getId());
            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Voluntarul '" + selectedVoluntar.getNumeComplet() + "' a fost șters.");

            // Păstrăm mesajul de succes după refresh
            FacesContext.getCurrentInstance().getExternalContext().getFlash().setKeepMessages(true);

            // Forțăm un redirect către aceeași pagină pentru a o reîncărca complet
            return "/xhtml/dashboardSecretar.xhtml?faces-redirect=true";

        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Eroare la ștergere", e.getMessage());
            return null; // Rămânem pe pagină să vedem eroarea
        }
    }
    // --- Rapoarte ---
    public StreamedContent genereazaRaportVoluntariActivi() {
        List<Voluntar> data = voluntari.stream().filter(v -> v.getStatus() == Status.ACTIV).collect(Collectors.toList());
        byte[] pdfBytes = documentGenerationService.genereazaRaportListaVoluntari("Listă voluntari activi", data);
        return DefaultStreamedContent.builder().name("Raport_Activi.pdf").contentType("application/pdf").stream(() -> new ByteArrayInputStream(pdfBytes)).build();
    }
    public StreamedContent genereazaRaportVoluntariColaboratori() {
        List<Voluntar> data = voluntari.stream().filter(v -> v.getStatus() == Status.COLABORATOR).collect(Collectors.toList());
        byte[] pdfBytes = documentGenerationService.genereazaRaportListaVoluntari("Listă voluntari colaboratori", data);
        return DefaultStreamedContent.builder().name("Raport_Colaboratori.pdf").contentType("application/pdf").stream(() -> new ByteArrayInputStream(pdfBytes)).build();
    }

    public StreamedContent genereazaRaportVoluntariFull() {
        List<Voluntar> data = voluntari.stream().collect(Collectors.toList());
        byte[] pdfBytes = documentGenerationService.genereazaRaportListaVoluntari("Listă voluntari integrală", data);
        return DefaultStreamedContent.builder().name("Raport_listă_integrală.pdf").contentType("application/pdf").stream(() -> new ByteArrayInputStream(pdfBytes)).build();
    }
    public StreamedContent genereazaRaportVoluntariInactivi() {
        List<Voluntar> data = voluntari.stream().filter(v -> v.getStatus() != Status.ACTIV).collect(Collectors.toList());
        byte[] pdfBytes = documentGenerationService.genereazaRaportListaVoluntari("Listă voluntari inactivi", data);
        return DefaultStreamedContent.builder().name("Raport_Inactivi.pdf").contentType("application/pdf").stream(() -> new ByteArrayInputStream(pdfBytes)).build();
    }

    // --- Utilitare ---
    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
    }
    private void handleError(String message) {
        addMessage(FacesMessage.SEVERITY_WARN, "Atenție", message);
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

    public void initEditarePrezenta(SedintaDTO sedintaDTO) {
        this.editModePrezenta = true;
        this.sedintaCurenta = sedintaDTO.getSedinta();

        // CORECT: Apelăm metoda din serviciu, care este deja injectat
        List<PrezentaSedinta> prezenteExistente = sedintaService.getPrezentePentruSedinta(sedintaCurenta.getId());

        prezenteMap.clear();
        // Populăm mapa cu valorile deja salvate
        for (PrezentaSedinta p : prezenteExistente) {
            prezenteMap.put(p.getVoluntar().getId(), p.getStatusPrezenta());
        }

        // ADAUGĂ ACEASTĂ SECȚIUNE pentru a include și voluntarii noi, care nu au fost la ședință
        // Este util dacă ai adăugat un voluntar nou după ce s-a ținut ședința
        if (this.voluntari != null) {
            for (Voluntar v : this.voluntari) {
                prezenteMap.putIfAbsent(v.getId(), StatusPrezenta.ABSENT);
            }
        }
    }

    // Adaugă această metodă nouă
    public void saveOrUpdatePrezenta() {
        if (this.editModePrezenta) {
            salveazaModificariPrezenta();
        } else {
            salveazaPrezenta();
        }
    }

    public void salveazaModificariPrezenta() {
        try {
            sedintaService.actualizeazaPrezenta(sedintaCurenta.getId(), prezenteMap);
            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Prezența a fost actualizată.");
            // Reîmprospătăm datele din pagină
            this.sedinteDTO = sedintaService.getSedinteInfoPentruOrganizatie(this.organizatie.getId());
            PrimeFaces.current().ajax().update(":sedinteForm:sedinteTable", ":voluntarForm:voluntariTable", ":messages");
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_FATAL, "Eroare la actualizare", e.getMessage());
            e.printStackTrace();
        }
    }

    public void executaStergereSedinta() {
        if (selectedSedintaDTO == null || selectedSedintaDTO.getSedinta() == null) {
            addMessage(FacesMessage.SEVERITY_WARN, "Atenție", "Nicio ședință selectată pentru ștergere.");
            return;
        }
        try {
            sedintaService.stergeSedintaSiPrezentele(selectedSedintaDTO.getSedinta().getId());
            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Ședința a fost ștearsă.");

            // Reîmprospătăm toate datele care ar fi putut fi afectate
            init(); // Cel mai simplu mod de a reîncărca totul corect

        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Eroare la ștergere", e.getMessage());
            e.printStackTrace();
        }
    }
}