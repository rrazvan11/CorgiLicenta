package ro.ong.corgi.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpSession; // Vom avea nevoie pentru logout
import lombok.Getter;
import lombok.Setter;
import ro.ong.corgi.model.User;
import ro.ong.corgi.service.AuthService;

import java.io.Serializable; // Bună practică pentru beans, mai ales cele cu scope mai lung

@Named // Face bean-ul accesibil în paginile JSF ca "#{loginBean}"
@RequestScoped // Ciclul de viață al bean-ului: o nouă instanță pentru fiecare request HTTP
@Getter // Lombok pentru a genera automat getteri
@Setter // Lombok pentru a genera automat setteri
public class LoginBean implements Serializable {

    private static final long serialVersionUID = 1L; // Bună practică pentru Serializable

    private String email;
    private String parola;

    @Inject
    private AuthService authService; // Injectăm serviciul de autentificare

    public LoginBean() {
        // Constructorul implicit este suficient, CDI se ocupă de instanțiere
        System.out.println("LoginBean a fost creat (RequestScoped)");
    }

    public String doLogin() {
        try {
            System.out.println("Încercare login pentru email: " + email); // Log pentru debug
            User userAutentificat = authService.login(email, parola);

            if (userAutentificat != null) {
                // Login reușit!
                FacesContext facesContext = FacesContext.getCurrentInstance();
                ExternalContext externalContext = facesContext.getExternalContext();
                externalContext.getSessionMap().put("loggedInUser", userAutentificat); // Stocăm user-ul în sesiune

                System.out.println("Utilizator autentificat: " + userAutentificat.getUsername() + ", Rol: " + userAutentificat.getRol());

                // Adaugă un mesaj de succes global (opțional)
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Autentificare reușită!", "Bun venit, " + userAutentificat.getUsername() + "!"));


                // TODO: Navigare către pagina corespunzătoare rolului sau un dashboard general
                // Pentru moment, vom naviga către pagina de index pentru a testa.
                // Asigură-te că paginile țintă există în folderul /xhtml/
                // Vom crea aceste pagini de dashboard mai târziu.

                // Exemplu de navigație bazată pe rol (de completat ulterior)
                switch (userAutentificat.getRol()) {
                    case VOLUNTAR:
                        // return "/xhtml/voluntar/dashboardVoluntar.xhtml?faces-redirect=true";
                        System.out.println("Navigare către dashboard voluntar...");
                        return "/xhtml/index.xhtml?faces-redirect=true"; // TEMPORAR
                    case COORDONATOR:
                        // return "/xhtml/coordonator/dashboardCoordonator.xhtml?faces-redirect=true";
                        System.out.println("Navigare către dashboard coordonator...");
                        return "/xhtml/index.xhtml?faces-redirect=true"; // TEMPORAR
                    case SECRETAR:
                        // return "/xhtml/secretar/dashboardSecretar.xhtml?faces-redirect=true";
                        System.out.println("Navigare către dashboard secretar...");
                        return "/xhtml/index.xhtml?faces-redirect=true"; // TEMPORAR
                    default:
                        System.out.println("Rol necunoscut, navigare către index...");
                        return "/xhtml/index.xhtml?faces-redirect=true"; // Pagina default după login
                }

            } else {
                // Teoretic, authService.login aruncă excepție la eșec, deci acest `else` nu ar trebui atins.
                FacesContext.getCurrentInstance().addMessage(null, // Mesaj global
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare Autentificare", "Credențiale invalide."));
                System.out.println("Login eșuat (caz neașteptat, user null fără excepție).");
                return null; // Rămâne pe pagina de login
            }
        } catch (RuntimeException e) {
            // AuthService.login aruncă RuntimeException pentru credențiale greșite sau alte probleme
            FacesContext.getCurrentInstance().addMessage(null, // Mesaj global
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare Autentificare", e.getMessage()));
            System.err.println("Eroare la login: " + e.getMessage());
            return null; // Rămâne pe pagina de login pentru a afișa eroarea
        }
    }

    // Metodă pentru logout (o vom lega la un buton/link mai târziu)
    public String doLogout() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        HttpSession session = (HttpSession) externalContext.getSession(false); // Ia sesiunea existentă, nu crea una noua

        System.out.println("Se încearcă delogarea...");
        if (session != null) {
            externalContext.getSessionMap().remove("loggedInUser"); // Elimină user-ul din sesiune
            session.invalidate(); // Invalidează sesiunea HTTP
            System.out.println("Sesiune invalidată.");
        }
        // Redirecționează către pagina de login
        return "/xhtml/login.xhtml?faces-redirect=true";
    }
}