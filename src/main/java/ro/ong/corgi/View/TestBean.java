package ro.ong.corgi.View; // Sau ro.ong.corgi.bean

import jakarta.enterprise.context.RequestScoped; // Sau ViewScoped, SessionScoped
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named; // Pentru a-l face accesibil în JSF ca #{testBean}
import lombok.Getter;
import lombok.Setter;

@Named // Face bean-ul accesibil în JSF cu numele "testBean" (implicit, numele clasei cu literă mică)
@RequestScoped // Durata de viață a bean-ului (per request HTTP)
@Getter // Lombok pentru getteri
@Setter // Lombok pentru setteri
public class TestBean {

    private String nume;

    public TestBean() {
        System.out.println("TestBean a fost creat!");
    }

    public void salveazaNume() {
        if (nume != null && !nume.trim().isEmpty()) {
            System.out.println("Numele introdus este: " + nume);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes!", "Numele '" + nume + "' a fost procesat."));
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenție!", "Te rog introdu un nume."));
        }
    }
}