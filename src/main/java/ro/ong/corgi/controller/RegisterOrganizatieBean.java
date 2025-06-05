package ro.ong.corgi.controller; // Sau ro.ong.corgi.view

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import ro.ong.corgi.model.Organizatie;
import ro.ong.corgi.model.User;
import ro.ong.corgi.model.Enums.Rol;
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
    private String cifOrganizatie; // Primit ca String din formular

    @Inject
    private OrganizatieService organizatieService;

    @Inject
    private AuthService authService;

    protected RegisterOrganizatieBean() {
        System.out.println("RegisterOrganizatieBean a fost creat.");
    }

    public String doRegisterOrganizatie() {
        FacesContext context = FacesContext.getCurrentInstance();
        boolean isValid = true;

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
            // required="true" din XHTML ar trebui să prindă asta, dar pentru siguranță
            context.addMessage("registerOrgForm:cifOrganizatie", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "CIF-ul este obligatoriu."));
            isValid = false;
        }


        if (!isValid) {
            return null;
        }

        try {
            User adminUser = authService.register(usernameAdmin, emailAdmin, parolaAdmin, Rol.COORDONATOR);
            organizatie.setUser(adminUser);
            organizatieService.adaugaOrganizatie(organizatie);

            FacesMessage successMessage = new FacesMessage(FacesMessage.SEVERITY_INFO, "Înregistrare Reușită!",
                    "Organizația '" + organizatie.getNume() + "' și contul de admin '" + usernameAdmin + "' au fost create.");
            context.addMessage(null, successMessage);
            context.getExternalContext().getFlash().setKeepMessages(true);

            System.out.println("Organizație înregistrată: " + organizatie.getNume());
            return "/xhtml/login.xhtml?faces-redirect=true";

        } catch (RuntimeException e) {
            FacesMessage errorMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare la Înregistrare", e.getMessage());
            context.addMessage(null, errorMessage);
            System.err.println("Eroare la înregistrare organizație: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}