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

        // Validarea pentru emailUser se face în RegisterVoluntarBean și/sau în AuthService.register
        // Nu mai validăm voluntar.getEmail() aici

        Organizatie organizatieAfiliere = organizatieRepository.findByCif(cifOrganizatie);
        if (organizatieAfiliere == null) {
            throw new RuntimeException("Organizație cu CIF-ul " + cifOrganizatie + " nu a fost găsită.");
        }

        User userPentruVoluntar;
        try {
            // Creăm contul de utilizator folosind emailUser primit ca parametru
            userPentruVoluntar = authService.register(username, emailUser, parolaUser, Rol.VOLUNTAR);
        } catch (RuntimeException e) {
            // Specificăm mai clar sursa erorii
            throw new RuntimeException("Nu s-a putut crea contul de utilizator pentru voluntar (username: " + username + ", email: " + emailUser + "): " + e.getMessage(), e);
        }

        voluntar.setUser(userPentruVoluntar);
        voluntar.setDataInrolare(LocalDate.now());
        voluntar.setStatus(Status.ACTIV);
        voluntar.setPuncte(0); // Acest câmp există în Voluntar.java
        voluntar.setDepartament(null); // Inițial, voluntarul nu este asignat unui departament

        // Verificarea unicității emailului este acum responsabilitatea AuthService.register.
        // Verificarea 'existentCuAcelEmail' bazată pe voluntar.getEmail() a fost eliminată.

        // Această verificare este pentru a preveni asignarea unui User ID la mai mulți Voluntari.
        // Având în vedere că userPentruVoluntar este nou creat și unic (garantat de authService.register),
        // această verificare ar trebui să fie mereu falsă în acest flux specific.
        // Este mai relevantă dacă ai un flux unde un User *existent* poate fi legat de un *nou* profil Voluntar.
        if (voluntarRepository.findSingleByField("user.id", userPentruVoluntar.getId()) != null){
            // Dacă se ajunge aici, înseamnă că authService.register nu a garantat unicitatea userului
            // sau există o problemă de logică/concurență.
            throw new RuntimeException("Eroare internă critică: ID-ul de utilizator nou creat (" + userPentruVoluntar.getId() + ") este deja asociat unui alt voluntar.");
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

        // Relația @OneToMany cu GrupareVoluntariProiecte din Voluntar.java are cascade = CascadeType.ALL și orphanRemoval = true,
        // deci acele înregistrări se vor șterge automat.

        // User-ul asociat: entitatea User are @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
        // pentru câmpul 'voluntar'.
        // Asta înseamnă că dacă ștergi User-ul, se șterge și Voluntarul.
        // Invers, dacă ștergi Voluntarul, User-ul NU este șters automat prin această configurație.
        // Va trebui să gestionezi manual User-ul.
        User userAsociat = voluntar.getUser();

        voluntarRepository.delete(voluntar); // Șterge voluntarul

        // Ce facem cu User-ul? Opțiuni:
        // 1. Ștergere: userRepository.delete(userAsociat); (ATENȚIE: Poate e folosit și altundeva? De ex. Organizatie)
        // 2. Dezactivare: authService.dezactiveazaCont(userAsociat.getId());
        // 3. Lăsat așa: Va rămâne un User fără profil de voluntar.
        // Momentan, nu facem nimic explicit cu User-ul, ceea ce înseamnă că va rămâne în sistem.
        // Aceasta este o decizie de business. Pentru o curățare completă, ar trebui șters sau dezactivat.
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