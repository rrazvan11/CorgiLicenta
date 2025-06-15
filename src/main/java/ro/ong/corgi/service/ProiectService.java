package ro.ong.corgi.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.ong.corgi.model.*;
import ro.ong.corgi.model.Enums.StatusAplicari; // Importăm denumirea corectă
import ro.ong.corgi.model.Enums.StatusProiect; // Importăm denumirea corectă
import ro.ong.corgi.repository.GrupareVoluntariProiecteRepository;
import ro.ong.corgi.repository.ProiectRepository;
import ro.ong.corgi.repository.OrganizatieRepository;
import ro.ong.corgi.repository.VoluntarRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProiectService {

    @Inject private ProiectRepository proiectRepository;
    @Inject private OrganizatieRepository organizatieRepository;
    @Inject private GrupareVoluntariProiecteRepository grupareVoluntariProiecteRepository;
    @Inject private VoluntarRepository voluntarRepository;


    protected ProiectService() {}

    @Transactional
    public void adaugaProiect(Proiect p) {
        if (p.getNumeProiect() == null || p.getNumeProiect().isBlank()) {
            throw new RuntimeException("Numele proiectului este obligatoriu.");
        }
        if (p.getOrganizatie() == null || p.getOrganizatie().getId() == null) {
            throw new RuntimeException("Proiectul trebuie asociat unei organizații valide.");
        }
        if (p.getDataInceput() == null || p.getDataSfarsit() == null || p.getDataInceput().isAfter(p.getDataSfarsit())) {
            throw new RuntimeException("Perioada de desfășurare a proiectului este invalidă.");
        }

        Organizatie orgExistent = organizatieRepository.findById(p.getOrganizatie().getId());
        if (orgExistent == null) {
            throw new RuntimeException("Organizația specificată pentru proiect nu există.");
        }
        p.setOrganizatie(orgExistent);

        List<Proiect> existente = proiectRepository.findByNameAndOrg(p.getNumeProiect(), p.getOrganizatie().getId());
        if (!existente.isEmpty()) {
            throw new RuntimeException("Proiectul „" + p.getNumeProiect() + "” există deja pentru această organizație.");
        }

        p.setStatus(StatusProiect.INSCRIERI_DESCHISE);
        proiectRepository.save(p);
    }

    @Transactional
    public void actualizeazaProiect(Proiect p) {
        Proiect existent = proiectRepository.findById(p.getId());
        if (existent == null) throw new RuntimeException("Proiect inexistent: " + p.getId());

        existent.setNumeProiect(p.getNumeProiect());
        existent.setDescriere(p.getDescriere());
        existent.setNecesarVoluntari(p.getNecesarVoluntari());
        existent.setDataInceput(p.getDataInceput());
        existent.setDataSfarsit(p.getDataSfarsit());
        existent.setStatus(p.getStatus());

        proiectRepository.update(existent);
    }

    @Transactional
    public void stergeProiect(Long id) {
        Proiect p = proiectRepository.findById(id);
        if (p == null) throw new RuntimeException("Proiect inexistent: " + id);
        if (p.getTaskuri() != null && !p.getTaskuri().isEmpty()) {
            throw new RuntimeException("Proiectul are taskuri asociate. Ștergeți sau reasignați taskurile mai întâi.");
        }
        proiectRepository.delete(p);
    }

    @Transactional
    public void aplicaLaProiect(Long proiectId, Long voluntarId) {
        Proiect proiect = proiectRepository.findById(proiectId);
        Voluntar voluntar = voluntarRepository.findById(voluntarId);
        if (proiect == null || voluntar == null) throw new RuntimeException("Proiect sau voluntar invalid.");
        if (proiect.getStatus() != StatusProiect.INSCRIERI_DESCHISE) {
            throw new RuntimeException("Înscrierile pentru acest proiect nu sunt deschise.");
        }
        boolean aMaiAplicat = proiect.getParticipari().stream().anyMatch(gvp -> gvp.getVoluntar().getId().equals(voluntarId));
        if (aMaiAplicat) {
            throw new RuntimeException("Ați aplicat deja la acest proiect.");
        }

        GrupareVoluntariProiecte aplicatie = GrupareVoluntariProiecte.builder()
                .proiect(proiect)
                .voluntar(voluntar)
                .statusAplicatie(StatusAplicari.APLICAT)
                .dataAplicatie(LocalDateTime.now())
                .build();

        grupareVoluntariProiecteRepository.save(aplicatie);
    }

    @Transactional
    public void gestioneazaAplicatie(Long grupareId, StatusAplicari statusNou) { // Am schimbat tipul aici
        GrupareVoluntariProiecte aplicatie = grupareVoluntariProiecteRepository.findById(grupareId);
        if (aplicatie == null) throw new RuntimeException("Aplicație inexistentă.");

        aplicatie.setStatusAplicatie(statusNou);
        grupareVoluntariProiecteRepository.update(aplicatie);
    }

    public List<GrupareVoluntariProiecte> getAplicatiiPentruProiect(Long proiectId) {
        return grupareVoluntariProiecteRepository.findByProiectIdWithVoluntar(proiectId);
    }

    public List<Voluntar> getVoluntariAcceptatiInProiect(Long proiectId) {
        Proiect proiect = proiectRepository.findById(proiectId);
        if (proiect == null || proiect.getParticipari() == null) return List.of();

        return proiect.getParticipari().stream()
                .filter(gvp -> gvp.getStatusAplicatie() == StatusAplicari.ACCEPTAT)
                .map(GrupareVoluntariProiecte::getVoluntar)
                .collect(Collectors.toList());
    }

    public Proiect cautaDupaId(Long id) {
        Proiect p = proiectRepository.findById(id);
        if (p == null) throw new RuntimeException("Proiect inexistent: " + id);
        return p;
    }

    public List<Proiect> toateProiectele() {
        return proiectRepository.findAll();
    }

    @Transactional // Asigură-te că metoda este tranzacțională pentru a menține sesiunea deschisă
    public List<Proiect> gasesteDupaOrganizatie(Long orgId) {
        if (organizatieRepository.findById(orgId) == null) {
            throw new RuntimeException("Organizație cu ID " + orgId + " inexistentă.");
        }
        // Metoda acum doar pasează apelul către repository.
        // Repository-ul face toată treaba de încărcare a datelor.
        return proiectRepository.findByOrganizatieId(orgId);
    }

    public List<Proiect> gasesteProiecteDupaVoluntarId(Long voluntarId) {
        // Această metodă va necesita o ajustare în ProiectRepository
        return proiectRepository.findByVoluntarId(voluntarId);
    }

    // Adaugă această metodă nouă în clasă
    public List<Proiect> gasesteProiecteDupaStatus(StatusProiect status) {
        return proiectRepository.findByStatus(status);
    }
}