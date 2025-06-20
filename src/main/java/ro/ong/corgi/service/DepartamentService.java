package ro.ong.corgi.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.ong.corgi.model.Departament;
import ro.ong.corgi.model.Enums.Rol;
import ro.ong.corgi.model.User;
import ro.ong.corgi.model.Voluntar;
import ro.ong.corgi.repository.DepartamentRepository;

import java.util.List;

@ApplicationScoped
public class DepartamentService {

    private final DepartamentRepository departamentRepository;

    @Inject
    public DepartamentService(DepartamentRepository departamentRepository) {
        this.departamentRepository = departamentRepository;
    }

    protected DepartamentService(){
        this(null);
    }

    @Transactional
    public void creeazaDepartament(Departament d, User actor) {
        if (actor.getRol() != Rol.SECRETAR) {
            throw new RuntimeException("Nu ai permisiunea de a crea departamente");
        }
        if (d.getOrganizatie() == null || d.getOrganizatie().getCif() == null) {
            throw new RuntimeException("Departamentul trebuie asociat unei organizații valide.");
        }
        if (departamentRepository.findByNumeAndOrganizatieId(d.getNume(), d.getOrganizatie().getCif()) != null) {
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
        if (!existent.getNume().equals(d.getNume()) &&
                departamentRepository.findByNumeAndOrganizatieId(d.getNume(), existent.getOrganizatie().getCif()) != null) {
            throw new RuntimeException("Un alt departament cu numele „" + d.getNume() + "” există deja în această organizație.");
        }

        existent.setNume(d.getNume());
        existent.setDescriere(d.getDescriere());
        existent.setCoordonator(d.getCoordonator());
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

    public List<Departament> gasesteDepartamentePeOrganizatie(Long organizatieCif) {
        return departamentRepository.findByField("organizatie.id", organizatieCif);
    }

    public List<Departament> gasesteToateDepartamenteleCuVoluntari() {
        return departamentRepository.gasesteDepartamenteVoluntari();
    }

    public Departament findByCoordonator(Voluntar coordonator) {
        if (coordonator == null || coordonator.getId() == null) {
            return null;
        }
        return departamentRepository.findSingleByField("coordonator.id", coordonator.getId());
    }
}