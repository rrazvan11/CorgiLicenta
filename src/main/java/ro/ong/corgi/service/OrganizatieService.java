package ro.ong.corgi.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ro.ong.corgi.model.Organizatie;
import ro.ong.corgi.model.User;
import ro.ong.corgi.repository.OrganizatieRepository;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class OrganizatieService {

    private final OrganizatieRepository organizatieRepository;

    @Inject
    public OrganizatieService(OrganizatieRepository organizatieRepository) {
        this.organizatieRepository = organizatieRepository;
    }
    protected OrganizatieService(){
        this(null);
    }

    @Transactional
    public void adaugaOrganizatie(Organizatie o) {
        if (o == null) {
            throw new IllegalArgumentException("Obiectul organizație nu poate fi null.");
        }
        if (o.getUser() == null || o.getUser().getId() == null) {
            throw new RuntimeException("Organizația trebuie să aibă un cont de utilizator valid (cu ID) asociat.");
        }
        if (o.getCif() == null) {
            throw new RuntimeException("CIF-ul este obligatoriu.");
        }
        // CORECT: Folosim findById pentru a verifica existența după noua cheie primară (CIF).
        if (organizatieRepository.findById(o.getCif()) != null) {
            throw new RuntimeException("Organizație cu CIF-ul „" + o.getCif() + "” există deja.");
        }
        if (o.getNume() == null || o.getNume().isBlank()) {
            throw new RuntimeException("Numele organizației este obligatoriu.");
        }
        Organizatie orgCuAcelasiNume = organizatieRepository.findSingleByField("nume", o.getNume());
        if (orgCuAcelasiNume != null) {
            throw new RuntimeException("Organizație cu numele „" + o.getNume() + "” există deja.");
        }

        organizatieRepository.save(o);
        System.out.println("OrganizatieService: Organizația '" + o.getNume() + "' a fost salvată.");
    }

    @Transactional
    public void actualizeazaOrganizatie(Organizatie o) {
        if (o == null || o.getCif() == null) {
            throw new IllegalArgumentException("Organizația sau CIF-ul ei nu pot fi null pentru actualizare.");
        }
        Organizatie existent = organizatieRepository.findById(o.getCif());
        if (existent == null) {
            throw new RuntimeException("Organizație inexistentă: " + o.getCif());
        }

        if (!existent.getNume().equals(o.getNume())) {
            Organizatie orgCuAcelasiNume = organizatieRepository.findSingleByField("nume", o.getNume());
            if (orgCuAcelasiNume != null && !orgCuAcelasiNume.getCif().equals(existent.getCif())) {
                throw new RuntimeException("O altă organizație cu numele „" + o.getNume() + "” există deja.");
            }
        }

        existent.setNume(o.getNume());
        existent.setAdresa(o.getAdresa());
        existent.setMail(o.getMail());

        organizatieRepository.update(existent);
    }

    public Organizatie cautaDupaUser(User user) {
        if (user == null) {
            return null;
        }
        return organizatieRepository.findSingleByField("user.id", user.getId());
    }
}