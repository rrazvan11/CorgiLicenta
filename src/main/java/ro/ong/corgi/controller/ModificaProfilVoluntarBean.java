package ro.ong.corgi.controller;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped; // Important pentru a păstra starea formularului
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import ro.ong.corgi.model.Enums.AnStudiu;
import ro.ong.corgi.model.Enums.Facultate;
import ro.ong.corgi.model.Enums.Rol;
import ro.ong.corgi.model.User;
import ro.ong.corgi.model.Voluntar;
import ro.ong.corgi.service.VoluntarService;

import java.io.IOException;
import java.io.Serializable;

@Named // Permite accesul din XHTML ca #{modificaProfilVoluntarBean}
@ViewScoped // Potrivit pentru a menține datele formularului pe parcursul interacțiunilor AJAX și postback-uri
@Getter
@Setter
public class ModificaProfilVoluntarBean implements Serializable {

    private static final long serialVersionUID = 1L;
    @Getter @Setter
    private boolean editMode = false;
    @Inject
    private VoluntarService voluntarService; // Serviciul pentru operații legate de voluntar

    @Inject
    private FacesContext facesContext; // Contextul JSF pentru mesaje, redirect etc.

    private Voluntar currentVoluntar; // Obiectul voluntar ale cărui date vor fi modificate

// Înlocuiește metoda existentă din ModificaProfilVoluntarBean.java cu aceasta

    @PostConstruct
    public void init() {
        User loggedInUser = (User) facesContext.getExternalContext().getSessionMap().get("loggedInUser");
        if (loggedInUser != null && loggedInUser.getRol() == Rol.VOLUNTAR) {
            // Preluăm o copie proaspătă a voluntarului din baza de date
            Voluntar foundVoluntar = voluntarService.cautaDupaUser(loggedInUser);
            if (foundVoluntar != null) {
                this.currentVoluntar = foundVoluntar;

                // --- LINIA ADĂUGATĂ ---
                // Setăm modul de editare pe 'true' imediat după încărcarea datelor.
                this.editMode = true;

            } else {
                // Cazul în care userul este VOLUNTAR dar nu se găsește profilul de voluntar
                handleError("Profilul de voluntar asociat contului dumneavoastră nu a fost găsit.");
            }
        } else {
            // Cazul în care utilizatorul nu este logat sau nu are rolul de VOLUNTAR
            handleError("Utilizator nelogat sau rol invalid pentru accesarea acestei pagini.");
            try {
                // Redirect către pagina de login dacă nu este autorizat
                facesContext.getExternalContext().redirect(
                        facesContext.getExternalContext().getRequestContextPath() + "/xhtml/login.xhtml?faces-redirect=true"
                );
            } catch (IOException e) {
                System.err.println("Eroare la redirect către login din ModificaProfilVoluntarBean: " + e.getMessage());
            }
        }
    }

    private void handleError(String message) {
        System.err.println("ModificaProfilVoluntarBean: " + message);
        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare Profil", message));
        // Setăm currentVoluntar la null pentru a preveni afișarea formularului dacă datele nu pot fi încărcate
        this.currentVoluntar = null;
    }
    // In ModificaProfilVoluntarBean.java

    public void anuleazaEditare() {
        this.editMode = false;
        // Aici poți reîncărca datele din DB dacă vrei să anulezi modificările ne-salvate
        // init();
    }
// Adaugă sau înlocuiește metoda existentă cu aceasta în clasa ModificaProfilVoluntarBean.java

    public String salveazaModificari() {
        if (currentVoluntar == null) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Datele voluntarului nu sunt disponibile pentru salvare."));
            return null; // Rămâne pe aceeași pagină
        }
        try {
            // Obiectul this.currentVoluntar conține deja noile date introduse în formular
            voluntarService.actualizeazaVoluntar(this.currentVoluntar);

            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes!", "Profilul dumneavoastră a fost actualizat."));

            // Se dezactivează modul de editare pentru a reveni la afișarea ca text
            this.editMode = false;

            // Rămânem pe pagină pentru a vedea mesajul de succes.
            return null;

        } catch (RuntimeException e) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare la salvare", e.getMessage()));
            System.err.println("Eroare la actualizare profil pentru voluntar ID " + (this.currentVoluntar.getId() != null ? this.currentVoluntar.getId() : "N/A") + ": " + e.getMessage());
            e.printStackTrace(); // Util pentru debugging

            // Rămânem pe pagină pentru a afișa eroarea
            return null;
        }
    }

    // Metode pentru a popula dropdown-urile din XHTML
    public Facultate[] getFacultati() {
        return Facultate.values(); // Returnează toate valorile din enum-ul Facultate
    }

    public AnStudiu[] getAniStudiu() {
        return AnStudiu.values(); // Returnează toate valorile din enum-ul AnStudiu
    }
}