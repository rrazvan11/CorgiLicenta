package ro.ong.corgi.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import ro.ong.corgi.model.Voluntar;
import ro.ong.corgi.model.Enums.AnStudiu;
import ro.ong.corgi.model.Enums.Facultate;
import ro.ong.corgi.service.VoluntarService;

import java.io.Serializable;

@Named
@RequestScoped
@Getter
@Setter
public class RegisterVoluntarBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;
    private String emailUser; // Acesta va fi emailul pentru entitatea User
    private String parola;
    private String confirmParola;
    private Voluntar voluntar = new Voluntar();

    private String cifOrganizatie;

    @Inject
    private VoluntarService voluntarService;

    public RegisterVoluntarBean() {
        System.out.println("RegisterVoluntarBean a fost creat (RequestScoped).");
    }

    public AnStudiu[] getAnStudiuValues() {
        return AnStudiu.values();
    }

    public Facultate[] getFacultateValues() {
        return Facultate.values();
    }

    public String doRegister() {
        FacesContext context = FacesContext.getCurrentInstance();
        boolean valid = true;

        // Verificări (am scos validarea pentru voluntar.email, deoarece nu mai există)
        if (username == null || username.trim().isEmpty()) {
            context.addMessage("registerVoluntarForm:username", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Numele de utilizator este obligatoriu."));
            valid = false;
        }
        if (emailUser == null || emailUser.trim().isEmpty()) {
            context.addMessage("registerVoluntarForm:emailUser", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Adresa de email este obligatorie."));
            valid = false;
        } else if (!emailUser.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            context.addMessage("registerVoluntarForm:emailUser", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Format email invalid."));
            valid = false;
        }

        // ... restul verificărilor pentru parolă, nume, prenume, CIF, telefon rămân la fel ...
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

        if (voluntar.getTelefon() != null && !voluntar.getTelefon().trim().isEmpty()) {
            if (!voluntar.getTelefon().trim().matches("^\\d{10}$")) {
                context.addMessage("registerVoluntarForm:telefon",
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare Format", "Telefonul trebuie să conțină exact 10 cifre."));
                valid = false;
            }
        }


        if (!valid) {
            return null;
        }

        // NU mai setăm voluntar.setEmail() aici

        try {
            // Pasăm this.emailUser ca parametru separat către serviciu
            voluntarService.adaugaVoluntar(voluntar, username, this.emailUser, parola, cifNumeric);

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