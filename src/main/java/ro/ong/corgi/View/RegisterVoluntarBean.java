package ro.ong.corgi.View; // Sau ro.ong.corgi.view dacă ai folosit 'v' mic

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import ro.ong.corgi.model.Voluntar;
import ro.ong.corgi.controller.VoluntarService;

import java.io.Serializable;

@Named
@RequestScoped
@Getter
@Setter
public class RegisterVoluntarBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;
    private String emailUser;
    private String parola;
    private String confirmParola;
    private Voluntar voluntar = new Voluntar();

    // MODIFICARE: cifOrganizatie este acum String
    private String cifOrganizatie;

    @Inject
    private VoluntarService voluntarService;

    // Constructor protected fără argumente pentru proxy-urile CDI
    protected RegisterVoluntarBean() {
        System.out.println("RegisterVoluntarBean a fost creat (RequestScoped).");
    }

    public String doRegister() {
        FacesContext context = FacesContext.getCurrentInstance();
        boolean valid = true;

        // 1. Verificări pentru câmpuri obligatorii și potrivirea parolelor
        if (username == null || username.trim().isEmpty()) {
            context.addMessage("registerVoluntarForm:username", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Numele de utilizator este obligatoriu."));
            valid = false;
        }
        if (emailUser == null || emailUser.trim().isEmpty()) {
            context.addMessage("registerVoluntarForm:emailUser", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Adresa de email este obligatorie."));
            valid = false;
        } else if (!emailUser.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) { // Validare format email
            context.addMessage("registerVoluntarForm:emailUser", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Format email invalid."));
            valid = false;
        }

        if (parola == null || parola.trim().isEmpty()) {
            context.addMessage("registerVoluntarForm:parola",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Parola este obligatorie."));
            valid = false;
        }
        if (confirmParola == null || confirmParola.trim().isEmpty()) {
            context.addMessage("registerVoluntarForm:confirmParola",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Confirmarea parolei este obligatorie."));
            valid = false;
        }

        // Verifică potrivirea parolelor doar dacă ambele sunt completate
        if (parola != null && !parola.trim().isEmpty() && confirmParola != null && !confirmParola.trim().isEmpty()) {
            if (!parola.equals(confirmParola)) {
                context.addMessage("registerVoluntarForm:confirmParola",
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Parolele nu se potrivesc."));
                valid = false;
            }
        }

        if (voluntar.getNume() == null || voluntar.getNume().trim().isEmpty()) {
            context.addMessage("registerVoluntarForm:numeVoluntar", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Numele este obligatoriu."));
            valid = false;
        }
        if (voluntar.getPrenume() == null || voluntar.getPrenume().trim().isEmpty()) {
            context.addMessage("registerVoluntarForm:prenumeVoluntar", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Prenumele este obligatoriu."));
            valid = false;
        }

        // 2. Conversie și validare CIF manual
        Long cifNumeric = null;
        if (cifOrganizatie == null || cifOrganizatie.trim().isEmpty()) {
            context.addMessage("registerVoluntarForm:cifOrganizatie",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "CIF-ul organizației este obligatoriu."));
            valid = false;
        } else {
            try {
                cifNumeric = Long.parseLong(cifOrganizatie.trim());
            } catch (NumberFormatException nfe) {
                context.addMessage("registerVoluntarForm:cifOrganizatie",
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare Format", "CIF-ul trebuie să fie un număr valid."));
                valid = false;
            }
        }

        // Validare număr de telefon (dacă e introdus, trebuie să aibă 10 cifre)
        if (voluntar.getTelefon() != null && !voluntar.getTelefon().trim().isEmpty()) {
            if (!voluntar.getTelefon().trim().matches("^\\d{10}$")) {
                context.addMessage("registerVoluntarForm:telefon",
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare Format", "Telefonul trebuie să conțină exact 10 cifre."));
                valid = false;
            }
        }

        if (!valid) {
            return null; // Rămâne pe aceeași pagină dacă sunt erori de validare
        }

        // 3. Setăm email-ul în obiectul voluntar
        voluntar.setEmail(this.emailUser);

        try {
            // 4. Apelăm serviciul
            voluntarService.adaugaVoluntar(voluntar, username, parola, cifNumeric);

            FacesMessage successMessage = new FacesMessage(FacesMessage.SEVERITY_INFO, "Înregistrare Reușită!",
                    "Contul tău a fost creat. Te poți autentifica acum.");
            context.addMessage(null, successMessage);
            context.getExternalContext().getFlash().setKeepMessages(true);
            System.out.println("Înregistrare reușită pentru: " + username);
            return "/xhtml/login.xhtml?faces-redirect=true";

        } catch (RuntimeException e) {
            FacesMessage errorMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare la Înregistrare", e.getMessage());
            context.addMessage(null, errorMessage);
            System.err.println("Eroare la înregistrare voluntar: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}