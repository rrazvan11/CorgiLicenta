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
    public void adaugaOrganizatie(Organizatie o) { // <-- SEMNĂTURA CORECTATĂ: primește doar Organizatie
        if (o == null) {
            throw new IllegalArgumentException("Obiectul organizație nu poate fi null.");
        }
        if (o.getUser() == null || o.getUser().getId() == null) {
            // User-ul ar trebui creat și asociat în RegisterOrganizatieBean înainte de a ajunge aici.
            // ID-ul user-ului este necesar pentru constrângerea de cheie externă.
            throw new RuntimeException("Organizația trebuie să aibă un cont de utilizator valid (cu ID) asociat.");
        }
        if (o.getCif() == null) {
            throw new RuntimeException("CIF-ul este obligatoriu.");
        }
        if (organizatieRepository.findByCif(o.getCif()) != null) {
            throw new RuntimeException("Organizație cu CIF-ul „" + o.getCif() + "” există deja.");
        }
        if (o.getNume() == null || o.getNume().isBlank()) {
            throw new RuntimeException("Numele organizației este obligatoriu.");
        }
        // Verifică unicitatea numelui organizației
        Organizatie orgCuAcelasiNume = organizatieRepository.findSingleByField("nume", o.getNume());
        if (orgCuAcelasiNume != null) {
            throw new RuntimeException("Organizație cu numele „" + o.getNume() + "” există deja.");
        }

        organizatieRepository.save(o);
        System.out.println("OrganizatieService: Organizația '" + o.getNume() + "' a fost salvată.");
    }

    public Organizatie cautaDupaId(Long id) {
        Organizatie o = organizatieRepository.findById(id);
        if (o == null) {
            throw new RuntimeException("Organizație inexistentă: " + id);
        }
        return o;
    }

    public Organizatie cautaDupaCif(Long cif) {
        Organizatie o = organizatieRepository.findByCif(cif);
        if (o == null) {
            throw new RuntimeException("Organizație cu CIF " + cif + " inexistentă.");
        }
        return o;
    }

    public List<Organizatie> toateOrganizatiile() {
        return organizatieRepository.findAll();
    }

    public void actualizeazaOrganizatie(Organizatie o) {
        if (o == null || o.getId() == null) {
            throw new IllegalArgumentException("Organizația sau ID-ul ei nu pot fi null pentru actualizare.");
        }
        Organizatie existent = organizatieRepository.findById(o.getId());
        if (existent == null) {
            throw new RuntimeException("Organizație inexistentă: " + o.getId());
        }

        // Verifică unicitatea numelui dacă s-a schimbat
        if (!existent.getNume().equals(o.getNume())) {
            Organizatie orgCuAcelasiNume = organizatieRepository.findSingleByField("nume", o.getNume());
            if (orgCuAcelasiNume != null && !orgCuAcelasiNume.getId().equals(existent.getId())) {
                throw new RuntimeException("O altă organizație cu numele „" + o.getNume() + "” există deja.");
            }
        }
        // Verifică unicitatea CIF-ului dacă s-a schimbat
        if (!existent.getCif().equals(o.getCif())) {
            Organizatie orgCuAcelasiCif = organizatieRepository.findByCif(o.getCif());
            if (orgCuAcelasiCif != null && !orgCuAcelasiCif.getId().equals(existent.getId())) {
                throw new RuntimeException("O altă organizație cu CIF-ul „" + o.getCif() + "” există deja.");
            }
        }

        existent.setNume(o.getNume());
        existent.setAdresa(o.getAdresa());
        existent.setCif(o.getCif());
        existent.setMail(o.getMail());

        organizatieRepository.update(existent);
    }

    public void stergeOrganizatie(Long id) {
        Organizatie o = organizatieRepository.findById(id);
        if (o == null) {
            throw new RuntimeException("Organizație inexistentă: " + id);
        }
        if (o.getProiecte() != null && !o.getProiecte().isEmpty()){
            throw new RuntimeException("Organizația are proiecte asociate și nu poate fi ștearsă.");
        }

        // Consideră ce faci cu User-ul asociat (dezactivare/ștergere)
        // if (o.getUser() != null && authService != null) { // Ai nevoie de authService injectat dacă vrei să faci asta
        // authService.dezactiveazaCont(o.getUser().getId());
        // }
        organizatieRepository.delete(o);
    }
    public Organizatie cautaDupaUser(User user) {
        if (user == null) {
            return null;
        }
        // Folosim metoda findSingleByField moștenită din AbstractRepository
        return organizatieRepository.findSingleByField("user.id", user.getId());
    }
}