package ro.ong.corgi.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ro.ong.corgi.model.Proiect;
import ro.ong.corgi.model.Organizatie;
import ro.ong.corgi.repository.ProiectRepository;
import ro.ong.corgi.repository.OrganizatieRepository; // Necesar pentru a verifica organizația

import java.util.List;

@ApplicationScoped
public class ProiectService {

    private final ProiectRepository proiectRepository;
    private final OrganizatieRepository organizatieRepository; // Pentru a valida existența organizației

    @Inject
    public ProiectService(ProiectRepository proiectRepository, OrganizatieRepository organizatieRepository) {
        this.proiectRepository = proiectRepository;
        this.organizatieRepository = organizatieRepository;
    }
    protected ProiectService(){
        this(null,null);
    }

    public void adaugaProiect(Proiect p) {
        if (p.getNumeProiect() == null || p.getNumeProiect().isBlank()) {
            throw new RuntimeException("Numele proiectului este obligatoriu.");
        }
        if (p.getOrganizatie() == null || p.getOrganizatie().getId() == null) {
            throw new RuntimeException("Proiectul trebuie asociat unei organizații valide.");
        }

        // Verifică dacă organizația există
        Organizatie orgExistent = organizatieRepository.findById(p.getOrganizatie().getId());
        if (orgExistent == null) {
            throw new RuntimeException("Organizația specificată pentru proiect nu există.");
        }
        p.setOrganizatie(orgExistent); // Asigură-te că folosești entitatea gestionată

        List<Proiect> existente = proiectRepository.findByNameAndOrg(p.getNumeProiect(), p.getOrganizatie().getId());
        if (!existente.isEmpty()) {
            throw new RuntimeException("Proiectul „" + p.getNumeProiect() + "” există deja pentru această organizație.");
        }
        proiectRepository.save(p);
    }

    public Proiect cautaDupaId(Long id) {
        Proiect p = proiectRepository.findById(id);
        if (p == null) {
            throw new RuntimeException("Proiect inexistent: " + id);
        }
        return p;
    }

    public List<Proiect> toateProiectele() {
        return proiectRepository.findAll();
    }

    public void actualizeazaProiect(Proiect p) {
        if (p.getId() == null) {
            throw new RuntimeException("ID-ul proiectului este necesar pentru actualizare.");
        }
        Proiect existent = proiectRepository.findById(p.getId());
        if (existent == null) {
            throw new RuntimeException("Proiect inexistent: " + p.getId());
        }

        // Verifică dacă organizația s-a schimbat și dacă cea nouă e validă
        if (p.getOrganizatie() == null || p.getOrganizatie().getId() == null) {
            throw new RuntimeException("Proiectul trebuie asociat unei organizații valide la actualizare.");
        }
        if (!existent.getOrganizatie().getId().equals(p.getOrganizatie().getId())) {
            Organizatie orgNoua = organizatieRepository.findById(p.getOrganizatie().getId());
            if (orgNoua == null) {
                throw new RuntimeException("Noua organizație specificată pentru proiect nu există.");
            }
            existent.setOrganizatie(orgNoua);
        }

        // Verifică unicitatea numelui dacă s-a schimbat numele sau organizația
        if (!existent.getNumeProiect().equals(p.getNumeProiect()) ||
                !existent.getOrganizatie().getId().equals(p.getOrganizatie().getId())) {
            List<Proiect> conflicte = proiectRepository.findByNameAndOrg(p.getNumeProiect(), existent.getOrganizatie().getId());
            // Exclude proiectul curent din verificare dacă ID-ul e același
            conflicte.removeIf(conflict -> conflict.getId().equals(existent.getId()));
            if (!conflicte.isEmpty()) {
                throw new RuntimeException("Un alt proiect cu numele „" + p.getNumeProiect() + "” există deja pentru această organizație.");
            }
        }

        existent.setNumeProiect(p.getNumeProiect());
        existent.setDescriere(p.getDescriere());
        existent.setNecesarVoluntari(p.getNecesarVoluntari());
        // Taskurile și participările sunt gestionate prin relațiile lor

        proiectRepository.update(existent);
    }

    public void stergeProiect(Long id) {
        Proiect p = proiectRepository.findById(id);
        if (p == null) {
            throw new RuntimeException("Proiect inexistent: " + id);
        }
        // Aici poți adăuga logica de a șterge/dezasocia taskurile și participările
        // sau lași cascadele din JPA să se ocupe dacă sunt setate corespunzător.
        // `GrupareVoluntariProiecte` are `CascadeType.ALL` și `orphanRemoval=true`, deci se vor șterge.
        // `Task.proiect` nu are cascade, deci taskurile vor rămâne cu `proiect_id` null sau vor da eroare
        // dacă `proiect_id` în taskuri e `NOT NULL` și nu există `ON DELETE SET NULL/CASCADE` la nivel de DB.
        // E mai sigur să gestionezi taskurile explicit.
        if (p.getTaskuri() != null && !p.getTaskuri().isEmpty()){
            throw new RuntimeException("Proiectul are taskuri asociate. Ștergeți sau reasignați taskurile mai întâi.");
        }

        proiectRepository.delete(p);
    }

    public List<Proiect> gasesteDupaOrganizatie(Long orgId) {
        // Verifică dacă organizația există
        if (organizatieRepository.findById(orgId) == null) {
            throw new RuntimeException("Organizație cu ID " + orgId + " inexistentă.");
        }
        return proiectRepository.findByOrganizatieId(orgId);
    }
}