package ro.ong.corgi.controller; // Sau ro.ong.corgi.view

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import ro.ong.corgi.model.Departament;
import ro.ong.corgi.model.Organizatie;
import ro.ong.corgi.model.User;
import ro.ong.corgi.model.Enums.Rol;
import ro.ong.corgi.service.DepartamentService;
import ro.ong.corgi.service.OrganizatieService;
import ro.ong.corgi.service.AuthService;

import java.io.Serializable;

@Named
@RequestScoped
@Getter
@Setter
public class RegisterOrganizatieBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private Organizatie organizatie = new Organizatie();
    private String usernameAdmin;
    private String emailAdmin;
    private String parolaAdmin;
    private String confirmParolaAdmin;
    private String cifOrganizatie;

    @Inject
    private OrganizatieService organizatieService;

    @Inject
    private AuthService authService;

    @Inject
    private DepartamentService departamentService;

    protected RegisterOrganizatieBean() {
        System.out.println("RegisterOrganizatieBean a fost creat.");
    }

    public String doRegisterOrganizatie() {
        FacesContext context = FacesContext.getCurrentInstance();
        boolean isValid = true;

        // Aici rămân validările tale pentru parolă, CIF etc.
        if (parolaAdmin == null || !parolaAdmin.equals(confirmParolaAdmin)) {
            context.addMessage("registerOrgForm:confirmParolaAdmin",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Parolele pentru admin nu se potrivesc."));
            isValid = false;
        }

        Long cifNumeric = null;
        if (cifOrganizatie != null && !cifOrganizatie.trim().isEmpty()) {
            try {
                cifNumeric = Long.parseLong(cifOrganizatie.trim());
                organizatie.setCif(cifNumeric);
            } catch (NumberFormatException nfe) {
                context.addMessage("registerOrgForm:cifOrganizatie",
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Format Invalid", "CIF-ul trebuie să fie un număr valid."));
                isValid = false;
            }
        } else {
            context.addMessage("registerOrgForm:cifOrganizatie", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "CIF-ul este obligatoriu."));
            isValid = false;
        }

        if (!isValid) {
            return null;
        }

        try {
            // Pasul 1: Creează User-ul și Organizația (cod pe care îl aveai deja)
            User adminUser = authService.register(usernameAdmin, emailAdmin, parolaAdmin, Rol.SECRETAR);
            organizatie.setUser(adminUser);
            organizatieService.adaugaOrganizatie(organizatie);

            // ==========================================================
            // === PASUL 2: LOGICA NOUĂ - Crearea departamentului implicit ===
            // ==========================================================
            Departament deptImplicit = new Departament();
            deptImplicit.setNume("Nerepartizat");
            deptImplicit.setDescriere("Departament implicit pentru voluntarii noi.");
            deptImplicit.setOrganizatie(organizatie); // Îl legăm de organizația proaspăt creată

            // Folosim serviciul pentru a-l salva în baza de date
            departamentService.creeazaDepartament(deptImplicit, adminUser);
            // ==========================================================

            FacesMessage successMessage = new FacesMessage(FacesMessage.SEVERITY_INFO, "Înregistrare Reușită!",
                    "Organizația '" + organizatie.getNume() + "' și departamentul 'Nerepartizat' au fost create.");
            context.addMessage(null, successMessage);
            context.getExternalContext().getFlash().setKeepMessages(true);

            return "/xhtml/login.xhtml?faces-redirect=true";

        } catch (RuntimeException e) {
            FacesMessage errorMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare la Înregistrare", e.getMessage());
            context.addMessage(null, errorMessage);
            e.printStackTrace();
            return null;
        }
    }
}