package ro.ong.corgi.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.ong.corgi.model.Departament;
import ro.ong.corgi.model.Enums.Rol;
import ro.ong.corgi.model.User; // User este un parametru, nu o dependență injectată a serviciului
import ro.ong.corgi.model.Voluntar;
import ro.ong.corgi.repository.DepartamentRepository;
// import ro.ong.corgi.repository.OrganizatieRepository; // Dacă ai nevoie de el

import java.util.List;

@ApplicationScoped
public class DepartamentService {

    private final DepartamentRepository departamentRepository;
    // private final OrganizatieRepository organizatieRepository; // Exemplu, dacă ai nevoie

    @Inject
    public DepartamentService(DepartamentRepository departamentRepository) {
        this.departamentRepository = departamentRepository;
    }
    protected DepartamentService(){
        this(null);
    }
    @Transactional
    public void creeazaDepartament(Departament d, User actor) {
        if (actor.getRol() != Rol.SECRETAR) { // Sau un rol de admin organizație
            throw new RuntimeException("Nu ai permisiunea de a crea departamente");
        }
        if (d.getOrganizatie() == null || d.getOrganizatie().getId() == null) {
            throw new RuntimeException("Departamentul trebuie asociat unei organizații valide.");
        }
        // Verifică unicitatea numelui în cadrul organizației
        if (departamentRepository.findByNumeAndOrganizatieId(d.getNume(), d.getOrganizatie().getId()) != null) {
            throw new RuntimeException("Un departament cu numele „" + d.getNume() + "” există deja în această organizație.");
        }
        departamentRepository.save(d);
    }
    @Transactional
    public void actualizeazaDepartament(Departament d, User actor) {
        if (actor.getRol() != Rol.SECRETAR) {
            throw new RuntimeException("Nu ai permisiunea de a modifica departamente");
        }
        Departament existent = departamentRepository.findById(d.getId());
        if (existent == null) {
            throw new RuntimeException("Departament inexistent: " + d.getId());
        }
        // Verifică dacă numele nou nu intră în conflict cu alt departament din aceeași organizație
        if (!existent.getNume().equals(d.getNume()) &&
                departamentRepository.findByNumeAndOrganizatieId(d.getNume(), existent.getOrganizatie().getId()) != null) {
            throw new RuntimeException("Un alt departament cu numele „" + d.getNume() + "” există deja în această organizație.");
        }

        existent.setNume(d.getNume());
        existent.setDescriere(d.getDescriere());
        existent.setCoordonator(d.getCoordonator());
        // existent.setOrganizatie(d.getOrganizatie()); // De obicei organizația unui departament nu se schimbă
        departamentRepository.update(existent);
    }
    @Transactional
    public void stergeDepartament(Long id, User actor) {
        if (actor.getRol() != Rol.SECRETAR) {
            throw new RuntimeException("Nu ai permisiunea de a șterge departamente");
        }
        Departament d = departamentRepository.findById(id);
        if (d == null) {
            throw new RuntimeException("Departament inexistent: " + id);
        }
        if (d.getVoluntari() != null && !d.getVoluntari().isEmpty()) {
            throw new RuntimeException("Departamentul conține voluntari și nu poate fi șters. Reasignați sau ștergeți mai întâi voluntarii.");
        }
        departamentRepository.delete(d);
    }

    public Departament cautaDupaId(Long id){
        Departament d = departamentRepository.findById(id);
        if (d == null) {
            throw new RuntimeException("Departament inexistent: " + id);
        }
        return d;
    }
    public List<Departament> toateDepartamentele() {
        return departamentRepository.findAll();
    }

    public List<Departament> gasesteDepartamentePeOrganizatie(Long organizatieId) {
        return departamentRepository.findByField("organizatie.id", organizatieId);
    }
    // Adaugă această metodă în clasa DepartamentService.java

    public List<Departament> gasesteToateDepartamenteleCuVoluntari() {
        // Apelăm metoda corespunzătoare din repository
        return departamentRepository.gasesteDepartamenteVoluntari();
    }
    public Departament findByCoordonator(Voluntar coordonator) {
        if (coordonator == null || coordonator.getId() == null) {
            return null;
        }
        // Folosim metoda deja existentă din AbstractRepository pentru a căuta
        // un departament care are ID-ul coordonatorului egal cu cel al voluntarului dat.
        return departamentRepository.findSingleByField("coordonator.id", coordonator.getId());
    }
}