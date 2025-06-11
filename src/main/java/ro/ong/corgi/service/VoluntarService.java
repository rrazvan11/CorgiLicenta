package ro.ong.corgi.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.ong.corgi.model.Voluntar;
import ro.ong.corgi.model.User;
import ro.ong.corgi.model.Organizatie;
import ro.ong.corgi.model.Departament;
import ro.ong.corgi.model.Enums.Rol;
import ro.ong.corgi.model.Enums.Status;
import ro.ong.corgi.repository.VoluntarRepository;
import ro.ong.corgi.repository.OrganizatieRepository;
import ro.ong.corgi.repository.DepartamentRepository;
// Importă și UserRepository dacă vrei să cauți useri direct prin el, deși e mai bine prin AuthService
// import ro.ong.corgi.repository.UserRepository;


import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class VoluntarService {

    private final VoluntarRepository voluntarRepository;
    private final AuthService authService;
    private final OrganizatieRepository organizatieRepository;
    private final DepartamentRepository departamentRepository;
    // private final UserRepository userRepository; // Injectează dacă e nevoie pentru cautaDupaEmail

    @Inject
    public VoluntarService(VoluntarRepository voluntarRepository,
                           AuthService authService,
                           OrganizatieRepository organizatieRepository,
                           DepartamentRepository departamentRepository /*, UserRepository userRepository */) {
        this.voluntarRepository = voluntarRepository;
        this.authService = authService;
        this.organizatieRepository = organizatieRepository;
        this.departamentRepository = departamentRepository;
        // this.userRepository = userRepository;
    }

    protected VoluntarService(){
        this(null,null,null,null );
    }

    /**
     * Adaugă un voluntar nou și contul de utilizator asociat.
     * Email-ul pentru contul de utilizator este primit ca parametru separat.
     */
    @Transactional
    public void adaugaVoluntar(Voluntar voluntar, String username, String emailUser, String parolaUser, Long cifOrganizatie) {
        // Validări pentru datele voluntarului (nume, prenume)
        if (voluntar.getNume() == null || voluntar.getNume().isBlank() ||
                voluntar.getPrenume() == null || voluntar.getPrenume().isBlank()) {
            throw new RuntimeException("Numele și prenumele voluntarului sunt obligatorii.");
        }

        Organizatie organizatieAfiliere = organizatieRepository.findByCif(cifOrganizatie);
        if (organizatieAfiliere == null) {
            throw new RuntimeException("Organizație cu CIF-ul " + cifOrganizatie + " nu a fost găsită.");
        }

        User userPentruVoluntar;
        try {
            userPentruVoluntar = authService.register(username, emailUser, parolaUser, Rol.VOLUNTAR);
        } catch (RuntimeException e) {
            throw new RuntimeException("Nu s-a putut crea contul de utilizator pentru voluntar: " + e.getMessage(), e);
        }

        // --- CORECȚIE APLICATĂ AICI ---
        // Logica pentru a găsi sau crea departamentul "Nedesmnat"
        final String defaultDeptName = "Nedesmnat";
        Departament departamentDefault = departamentRepository.findByNumeAndOrganizatieId(defaultDeptName, organizatieAfiliere.getId());

        if (departamentDefault == null) {
            // Dacă nu există, îl creăm
            departamentDefault = Departament.builder()
                    .nume(defaultDeptName)
                    .descriere("Departament general pentru voluntarii noi.")
                    .organizatie(organizatieAfiliere)
                    .build();
            departamentRepository.save(departamentDefault);
        }
        // --- SFÂRȘIT CORECȚIE ---

        voluntar.setUser(userPentruVoluntar);
        voluntar.setDataInrolare(LocalDate.now());
        voluntar.setStatus(Status.ACTIV);
        voluntar.setPuncte(0);
        voluntar.setDepartament(departamentDefault); // Asignăm noului voluntar departamentul default

        if (voluntarRepository.findSingleByField("user.id", userPentruVoluntar.getId()) != null){
            throw new RuntimeException("Eroare internă critică: ID-ul de utilizator nou creat este deja asociat unui alt voluntar.");
        }

        voluntarRepository.save(voluntar);
    }

    public Voluntar cautaDupaId(Long id) {
        Voluntar v = voluntarRepository.findById(id);
        if (v == null) {
            throw new RuntimeException("Voluntar inexistent cu ID: " + id);
        }
        return v;
    }

    /**
     * Caută un voluntar pe baza emailului contului de utilizator asociat.
     */
    public Voluntar cautaDupaEmail(String email) {
        // Căutăm întâi User-ul după email
        User user = authService.cautaDupaEmail(email); // Presupunem că AuthService are metoda cautaDupaEmail
        // care la rândul ei apelează UserRepository.findByEmail
        if (user != null && user.getId() != null) {
            // Apoi căutăm Voluntarul asociat acelui User
            return voluntarRepository.findSingleByField("user.id", user.getId());
        }
        return null; // Nu s-a găsit un user cu acest email sau userul nu are profil de voluntar
    }

    public Voluntar cautaDupaUser(User user) {
        if (user == null || user.getId() == null) {
            // Adaug un mesaj mai specific dacă user-ul e null
            // System.err.println("Încercare de a căuta voluntar cu user null sau user ID null.");
            return null;
        }
        return voluntarRepository.findSingleByField("user.id", user.getId());
    }
    @Transactional
    public void actualizeazaVoluntar(Voluntar voluntar) {
        if (voluntar.getId() == null) {
            throw new RuntimeException("ID-ul voluntarului este necesar pentru actualizare.");
        }
        Voluntar existent = voluntarRepository.findById(voluntar.getId());
        if (existent == null) {
            throw new RuntimeException("Voluntar inexistent pentru actualizare cu ID: " + voluntar.getId());
        }

        // Actualizăm câmpurile. Email-ul nu se mai gestionează aici, ci la nivel de User.
        existent.setNume(voluntar.getNume());
        existent.setPrenume(voluntar.getPrenume());
        // existent.setEmail(voluntar.getEmail()); // ELIMINAT
        existent.setTelefon(voluntar.getTelefon());
        existent.setFacultate(voluntar.getFacultate());
        existent.setSpecializare(voluntar.getSpecializare());
        existent.setAnStudiu(voluntar.getAnStudiu());
        // existent.setOreDeVoluntariat(voluntar.getOreDeVoluntariat()); // Acest câmp nu există în Voluntar.java
        existent.setPuncte(voluntar.getPuncte());
        existent.setStatus(voluntar.getStatus());

        // Gestionarea departamentului
        if (voluntar.getDepartament() != null && voluntar.getDepartament().getId() != null) {
            // Verifică dacă departamentul s-a schimbat sau era null
            if (existent.getDepartament() == null || !existent.getDepartament().getId().equals(voluntar.getDepartament().getId())) {
                Departament dNou = departamentRepository.findById(voluntar.getDepartament().getId());
                if (dNou == null) {
                    throw new RuntimeException("Departamentul specificat pentru actualizare (ID: " + voluntar.getDepartament().getId() + ") nu există.");
                }
                existent.setDepartament(dNou);
            }
        } else {
            // Dacă voluntar.getDepartament() e null, setăm și la cel existent tot null
            existent.setDepartament(null);
        }

        voluntarRepository.update(existent);
    }
     @Transactional
    public void stergeVoluntar(Long id) {
        Voluntar voluntar = cautaDupaId(id); // Aruncă excepție dacă nu există
        if (voluntar.getTaskuri() != null && !voluntar.getTaskuri().isEmpty()) {
            throw new RuntimeException("Voluntarul (ID: " + id + ") are task-uri asignate și nu poate fi șters. Reasignați task-urile mai întâi.");
        }

        User userAsociat = voluntar.getUser();

        voluntarRepository.delete(voluntar); // Șterge voluntarul
        System.out.println("Voluntarul cu ID " + id + " a fost șters. User-ul asociat (ID: " + (userAsociat != null ? userAsociat.getId() : "null") + ") nu a fost modificat automat.");

    }
    @Transactional
    public void schimbaStatusVoluntar(Long id, Status statusNou) {
        Voluntar voluntar = cautaDupaId(id);
        voluntar.setStatus(statusNou);
        voluntarRepository.update(voluntar);
    }

    public List<Voluntar> totiVoluntarii() {
        return voluntarRepository.findAll();
    }

    public List<Voluntar> gasesteVoluntariDinOrganizatie(Long organizatieId) {
        Organizatie org = organizatieRepository.findById(organizatieId);
        if (org == null) {
            throw new RuntimeException("Organizație cu ID " + organizatieId + " inexistentă.");
        }
        return voluntarRepository.findByOrganizatieId(organizatieId);
    }

    public List<Voluntar> gasesteVoluntariDinDepartament(Long departamentId) {
        Departament dep = departamentRepository.findById(departamentId);
        if (dep == null) {
            throw new RuntimeException("Departament cu ID " + departamentId + " inexistent.");
        }
        return voluntarRepository.findByField("departament.id", departamentId);
    }
}