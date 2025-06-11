package ro.ong.corgi.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.ong.corgi.model.Departament;
import ro.ong.corgi.model.Enums.Rol;
import ro.ong.corgi.model.User; // User este un parametru, nu o dependență injectată a serviciului
import ro.ong.corgi.model.Voluntar;
import ro.ong.corgi.repository.DepartamentRepository;
import ro.ong.corgi.repository.VoluntarRepository;
// import ro.ong.corgi.repository.OrganizatieRepository; // Dacă ai nevoie de el

import java.util.List;

@ApplicationScoped
public class DepartamentService {

    private final DepartamentRepository departamentRepository;
    private final VoluntarRepository voluntarRepository;

    @Inject
    public DepartamentService(DepartamentRepository departamentRepository, VoluntarRepository voluntarRepository) {
        this.departamentRepository = departamentRepository;
        this.voluntarRepository = voluntarRepository;
    }

    protected DepartamentService() { this(null, null); }

    @Transactional
    public Departament creeazaDepartament(Departament d, User actor) {
        if (actor.getRol() != Rol.SECRETAR) {
            throw new RuntimeException("Nu ai permisiunea de a crea departamente");
        }
        if (d.getOrganizatie() == null || d.getOrganizatie().getId() == null) {
            throw new RuntimeException("Departamentul trebuie asociat unei organizații valide.");
        }
        if (departamentRepository.findByNumeAndOrganizatieId(d.getNume(), d.getOrganizatie().getId()) != null) {
            throw new RuntimeException("Un departament cu numele „" + d.getNume() + "” există deja în această organizație.");
        }
        departamentRepository.save(d);
        return d;
    }

    @Transactional
    public Departament actualizeazaDepartament(Departament d, User actor) {
        if (actor.getRol() != Rol.SECRETAR) {
            throw new RuntimeException("Nu ai permisiunea de a modifica departamente");
        }
        Departament existent = departamentRepository.findById(d.getId());
        if (existent == null) {
            throw new RuntimeException("Departament inexistent: " + d.getId());
        }
        if (!existent.getNume().equals(d.getNume()) &&
                departamentRepository.findByNumeAndOrganizatieId(d.getNume(), existent.getOrganizatie().getId()) != null) {
            throw new RuntimeException("Un alt departament cu numele „" + d.getNume() + "” există deja în această organizație.");
        }

        existent.setNume(d.getNume());
        existent.setDescriere(d.getDescriere());

        if (d.getCoordonator() != null && d.getCoordonator().getId() != null) {
            Voluntar coordonatorManaged = voluntarRepository.findById(d.getCoordonator().getId());
            existent.setCoordonator(coordonatorManaged);
        } else {
            existent.setCoordonator(null);
        }

        departamentRepository.update(existent);
        return existent;
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
}