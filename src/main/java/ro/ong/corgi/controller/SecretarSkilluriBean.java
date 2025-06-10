package ro.ong.corgi.controller;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import ro.ong.corgi.model.Skill;
import ro.ong.corgi.service.SkillService;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
@Getter @Setter
public class SecretarSkilluriBean implements Serializable {

    @Inject
    private SkillService skillService;

    private List<Skill> skills;
    private Skill newSkill;

    @PostConstruct
    public void init() {
        skills = skillService.findAll();
        newSkill = new Skill();
    }

    public void adaugaSkill() {
        try {
            skillService.save(newSkill);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes", "Competența a fost adăugată."));
            init(); // Re-încarcă lista și resetează formularul
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu s-a putut salva competența. Verificați dacă există deja."));
        }
    }

    public void stergeSkill(Skill skillToDelete) {
        try {
            skillService.delete(skillToDelete);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes", "Competența a fost ștearsă."));
            skills.remove(skillToDelete); // Actualizează lista fără a re-interoga baza de date
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Eroare", "Nu s-a putut șterge competența."));
        }
    }
}