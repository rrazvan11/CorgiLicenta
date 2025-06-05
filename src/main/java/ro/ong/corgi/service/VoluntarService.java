package ro.ong.corgi.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ro.ong.corgi.model.Voluntar;
import ro.ong.corgi.model.User;
import ro.ong.corgi.model.Organizatie;
import ro.ong.corgi.model.Departament;
import ro.ong.corgi.model.Enums.Rol;
import ro.ong.corgi.model.Enums.Status;
import ro.ong.corgi.repository.VoluntarRepository;
import ro.ong.corgi.repository.OrganizatieRepository;
import ro.ong.corgi.repository.DepartamentRepository;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class VoluntarService {

    private final VoluntarRepository voluntarRepository;
    private final AuthService authService;
    private final OrganizatieRepository organizatieRepository;
    private final DepartamentRepository departamentRepository;
    // private final OrganizatieService organizatieService; // Sau injectează OrganizatieService dacă preferi

    @Inject
    public VoluntarService(VoluntarRepository voluntarRepository,
                           AuthService authService,
                           OrganizatieRepository organizatieRepository,
                           DepartamentRepository departamentRepository) {
        // OrganizatieService organizatieService) { // Adaugă dacă vrei să verifici org prin service
        this.voluntarRepository = voluntarRepository;
        this.authService = authService;
        this.organizatieRepository = organizatieRepository;
        this.departamentRepository = departamentRepository;
        // this.organizatieService = organizatieService;
    }
    protected VoluntarService(){
        this(null,null,null,null);
    }
    public void adaugaVoluntar(Voluntar voluntar, String username, String parolaUser, Long cifOrganizatie) {
        if (voluntar.getNume() == null || voluntar.getNume().isBlank() ||
                voluntar.getPrenume() == null || voluntar.getPrenume().isBlank()) {
            throw new RuntimeException("Numele și prenumele voluntarului sunt obligatorii.");
        }
        if (voluntar.getEmail() == null || voluntar.getEmail().isBlank()) {
            throw new RuntimeException("Email-ul voluntarului este obligatoriu.");
        }

        Organizatie organizatieAfiliere = organizatieRepository.findByCif(cifOrganizatie);
        if (organizatieAfiliere == null) {
            throw new RuntimeException("Organizație cu CIF-ul " + cifOrganizatie + " nu a fost găsită.");
        }

        User userPentruVoluntar;
        try {
            userPentruVoluntar = authService.register(username, voluntar.getEmail(), parolaUser, Rol.VOLUNTAR);
        } catch (RuntimeException e) {
            throw new RuntimeException("Nu s-a putut crea contul de utilizator pentru voluntar: " + e.getMessage(), e);
        }

        voluntar.setUser(userPentruVoluntar);
        voluntar.setDataInrolare(LocalDate.now());
        voluntar.setStatus(Status.ACTIV);
        voluntar.setOreDeVoluntariat(0);
        voluntar.setPuncte(0);
        voluntar.setDepartament(null);

        Voluntar existentCuAcelEmail = voluntarRepository.findFirstByEmail(voluntar.getEmail());
        if(existentCuAcelEmail != null && (existentCuAcelEmail.getUser() == null || !existentCuAcelEmail.getUser().getId().equals(userPentruVoluntar.getId()))){
            // Aruncă eroare doar dacă emailul e folosit de un *alt* voluntar/user.
            // Dacă e același user (deși fluxul de register ar trebui să prevină asta pentru email existent), poate e ok.
            // Cel mai sigur e ca authService.register să garanteze unicitatea emailului pentru User.
            throw new RuntimeException("Un voluntar (sau utilizator) cu emailul " + voluntar.getEmail() + " există deja.");
        }
        if (voluntarRepository.findSingleByField("user.id", userPentruVoluntar.getId()) != null){
            throw new RuntimeException("Eroare internă: User ID duplicat pentru voluntar nou.");
        }

        voluntarRepository.save(voluntar);
    }

    public Voluntar cautaDupaId(Long id) {
        Voluntar v = voluntarRepository.findById(id);
        if (v == null) {
            throw new RuntimeException("Voluntar inexistent: " + id);
        }
        return v;
    }

    public Voluntar cautaDupaEmail(String email) {
        return voluntarRepository.findFirstByEmail(email);
    }

    public Voluntar cautaDupaUser(User user) {
        if (user == null || user.getId() == null) return null;
        return voluntarRepository.findSingleByField("user.id", user.getId());
    }

    public void actualizeazaVoluntar(Voluntar voluntar) {
        if (voluntar.getId() == null) {
            throw new RuntimeException("ID-ul voluntarului este necesar pentru actualizare.");
        }
        Voluntar existent = voluntarRepository.findById(voluntar.getId());
        if (existent == null) {
            throw new RuntimeException("Voluntar inexistent: " + voluntar.getId());
        }

        existent.setNume(voluntar.getNume());
        existent.setPrenume(voluntar.getPrenume());
        existent.setEmail(voluntar.getEmail());
        existent.setTelefon(voluntar.getTelefon());
        existent.setFacultate(voluntar.getFacultate());
        existent.setSpecializare(voluntar.getSpecializare());
        existent.setAnStudiu(voluntar.getAnStudiu());
        existent.setOreDeVoluntariat(voluntar.getOreDeVoluntariat());
        existent.setPuncte(voluntar.getPuncte());
        existent.setStatus(voluntar.getStatus());

        if (voluntar.getDepartament() != null && voluntar.getDepartament().getId() != null) {
            if (existent.getDepartament() == null || !existent.getDepartament().getId().equals(voluntar.getDepartament().getId())) {
                Departament dNou = departamentRepository.findById(voluntar.getDepartament().getId());
                if (dNou == null) {
                    throw new RuntimeException("Departamentul specificat pentru actualizare nu există.");
                }
                existent.setDepartament(dNou);
            }
        } else {
            existent.setDepartament(null);
        }

        voluntarRepository.update(existent);
    }

    public void stergeVoluntar(Long id) {
        Voluntar voluntar = cautaDupaId(id);
        if (voluntar.getTaskuri() != null && !voluntar.getTaskuri().isEmpty()) {
            throw new RuntimeException("Voluntarul are task-uri asignate și nu poate fi șters. Reasignați task-urile mai întâi.");
        }
        voluntarRepository.delete(voluntar);
        // Consideră și dezactivarea User-ului asociat:
        // if (voluntar.getUser() != null) {
        // authService.dezactiveazaCont(voluntar.getUser().getId());
        // }
    }

    public void schimbaStatusVoluntar(Long id, Status statusNou) {
        Voluntar voluntar = cautaDupaId(id);
        voluntar.setStatus(statusNou);
        voluntarRepository.update(voluntar);
    }

    public List<Voluntar> totiVoluntarii() {
        return voluntarRepository.findAll();
    }

    // --- METODĂ ACTUALIZATĂ ---
    public List<Voluntar> gasesteVoluntariDinOrganizatie(Long organizatieId) {
        // Opțional, dar recomandat: verifică dacă organizația există
        Organizatie org = organizatieRepository.findById(organizatieId); // Folosește organizatieRepository injectat
        if (org == null) {
            throw new RuntimeException("Organizație cu ID " + organizatieId + " inexistentă.");
        }
        return voluntarRepository.findByOrganizatieId(organizatieId);
    }
    // --- SFÂRȘIT METODĂ ACTUALIZATĂ ---

    public List<Voluntar> gasesteVoluntariDinDepartament(Long departamentId) {
        // Opțional: verifică dacă departamentul există
        if (departamentRepository.findById(departamentId) == null) {
            throw new RuntimeException("Departament cu ID " + departamentId + " inexistent.");
        }
        return voluntarRepository.findByField("departament.id", departamentId);
    }
}