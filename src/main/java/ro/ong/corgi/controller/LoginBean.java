package ro.ong.corgi.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.Setter;
import ro.ong.corgi.model.User;
import ro.ong.corgi.model.Enums.Rol; // Asigură-te că ai importat Rol
import ro.ong.corgi.service.AuthService;

import java.io.Serializable;

@Named
@RequestScoped
@Getter
@Setter
public class LoginBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String email;
    private String parola;

    @Inject
    private AuthService authService;

    public LoginBean() {
        System.out.println("LoginBean a fost creat (RequestScoped)");
    }

    public String doLogin() {
        try {
            System.out.println("Încercare login pentru email: " + email);
            User userAutentificat = authService.login(email, parola);

            if (userAutentificat != null) {
                FacesContext facesContext = FacesContext.getCurrentInstance();
                ExternalContext externalContext = facesContext.getExternalContext();
                externalContext.getSessionMap().put("loggedInUser", userAutentificat);

                System.out.println("Utilizator autentificat: " + userAutentificat.getUsername() + ", Rol: " + userAutentificat.getRol());
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Autentificare reușită!", "Bun venit, " + userAutentificat.getUsername() + "!"));

                // Navigare bazată pe rol
                switch (userAutentificat.getRol()) {
                    case VOLUNTAR:
                        System.out.println("Navigare către dashboard voluntar...");
                        return "/xhtml/dashboardVoluntar.xhtml?faces-redirect=true";
                    case COORDONATOR:
                        System.out.println("Navigare către dashboard coordonator...");
                        // TODO: Actualizează cu calea corectă când dashboard-ul coordonatorului e gata
                        return "/xhtml/index.xhtml?faces-redirect=true"; // TEMPORAR
                    case SECRETAR:
                        System.out.println("Navigare către dashboard secretar...");
                        return "/xhtml/index.xhtml?faces-redirect=true";
                    default:
                        System.out.println("Rol necunoscut (" + userAutentificat.getRol() + "), navigare către index...");
                        return "/xhtml/index.xhtml?faces-redirect=true";
                }

            } else {
                // Acest bloc nu ar trebui, teoretic, să fie atins dacă authService.login aruncă excepție la eșec.
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare Autentificare", "Credențiale invalide (user null)."));
                System.out.println("Login eșuat (caz neașteptat, user null returnat fără excepție de AuthService).");
                return null;
            }
        } catch (RuntimeException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare Autentificare", e.getMessage()));
            System.err.println("Eroare la login pentru email '" + email + "': " + e.getMessage());
            // e.printStackTrace(); // Util pentru debug în consola serverului, dacă e nevoie
            return null;
        }
    }

    public String doLogout() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        HttpSession session = (HttpSession) externalContext.getSession(false);

        User loggedInUser = (User) externalContext.getSessionMap().get("loggedInUser");
        String usernamePentruLog = (loggedInUser != null) ? loggedInUser.getUsername() : "necunoscut";

        System.out.println("Se încearcă delogarea pentru utilizatorul: " + usernamePentruLog);
        if (session != null) {
            externalContext.getSessionMap().remove("loggedInUser");
            session.invalidate();
            System.out.println("Sesiune invalidată pentru: " + usernamePentruLog);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Delogare reușită", "Ai fost delogat cu succes."));
        } else {
            System.out.println("Nu a fost găsită o sesiune activă pentru invalidare.");
        }
        return "/xhtml/login.xhtml?faces-redirect=true";
    }
}