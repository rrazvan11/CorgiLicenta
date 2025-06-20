package ro.ong.corgi.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.ong.corgi.model.*;
import ro.ong.corgi.model.Enums.StatusAplicari;
import ro.ong.corgi.model.Enums.StatusProiect;
import ro.ong.corgi.repository.GrupareVoluntariProiecteRepository;
import ro.ong.corgi.repository.ProiectRepository;
import ro.ong.corgi.repository.OrganizatieRepository;
import ro.ong.corgi.repository.VoluntarRepository;

import java.time.LocalDateTime;
import java.util.List;

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
        if (p.getOrganizatie() == null || p.getOrganizatie().getCif() == null) {
            throw new RuntimeException("Proiectul trebuie asociat unei organizații valide.");
        }
        if (p.getDataInceput() == null || p.getDataSfarsit() == null || p.getDataInceput().isAfter(p.getDataSfarsit())) {
            throw new RuntimeException("Perioada de desfășurare a proiectului este invalidă.");
        }

        Organizatie orgExistent = organizatieRepository.findById(p.getOrganizatie().getCif());
        if (orgExistent == null) {
            throw new RuntimeException("Organizația specificată pentru proiect nu există.");
        }
        p.setOrganizatie(orgExistent);

        List<Proiect> existente = proiectRepository.findByNameAndOrg(p.getNumeProiect(), p.getOrganizatie().getCif());
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

    // METODA stergeProiect A FOST ȘTEARSĂ

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
    public void gestioneazaAplicatie(Long grupareId, StatusAplicari statusNou) {
        GrupareVoluntariProiecte aplicatie = grupareVoluntariProiecteRepository.findById(grupareId);
        if (aplicatie == null) throw new RuntimeException("Aplicație inexistentă.");

        aplicatie.setStatusAplicatie(statusNou);
        grupareVoluntariProiecteRepository.update(aplicatie);
    }

    // METODA getAplicatiiPentruProiect A FOST ȘTEARSĂ

    public List<Voluntar> getVoluntariAcceptatiInProiect(Long proiectId) {
        return voluntarRepository.findVoluntariAcceptatiInProiect(proiectId);
    }

    public Proiect cautaDupaId(Long id) {
        Proiect p = proiectRepository.findById(id);
        if (p == null) throw new RuntimeException("Proiect inexistent: " + id);
        return p;
    }

    // METODA toateProiectele A FOST ȘTEARSĂ

    @Transactional
    public List<Proiect> gasesteDupaOrganizatie(Long orgCif) { // Am redenumit parametrul pentru claritate
        if (organizatieRepository.findById(orgCif) == null) {
            throw new RuntimeException("Organizație cu CIF " + orgCif + " inexistentă.");
        }
        return proiectRepository.findByOrganizatieId(orgCif);
    }

    public List<Proiect> gasesteProiecteDupaVoluntarId(Long voluntarId) {
        return proiectRepository.findByVoluntarId(voluntarId);
    }

    public List<Proiect> gasesteProiecteDupaStatus(StatusProiect status) {
        return proiectRepository.findByStatus(status);
    }
}