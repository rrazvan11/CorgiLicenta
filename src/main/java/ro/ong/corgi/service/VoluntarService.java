package ro.ong.corgi.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.ong.corgi.model.Departament;
import ro.ong.corgi.model.Enums.Rol;
import ro.ong.corgi.model.Enums.Status;
import ro.ong.corgi.model.Organizatie;
import ro.ong.corgi.model.User;
import ro.ong.corgi.model.Voluntar;
import ro.ong.corgi.repository.DepartamentRepository;
import ro.ong.corgi.repository.OrganizatieRepository;
import ro.ong.corgi.repository.UserRepository;
import ro.ong.corgi.repository.VoluntarRepository;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class VoluntarService {

    // --- Am curățat injecțiile. Folosim @Inject direct pe câmpuri. ---
    @Inject
    private VoluntarRepository voluntarRepository;
    @Inject
    private AuthService authService;
    @Inject
    private OrganizatieRepository organizatieRepository;
    @Inject
    private DepartamentRepository departamentRepository;
    @Inject
    private UserRepository userRepository;

    // Constructorul gol este suficient acum.
    public VoluntarService() {
    }

    @Transactional
    public void adaugaVoluntar(Voluntar voluntar, String username, String emailUser, String parolaUser, Long cifOrganizatie) {
        if (voluntar.getNume() == null || voluntar.getNume().isBlank() ||
                voluntar.getPrenume() == null || voluntar.getPrenume().isBlank()) {
            throw new RuntimeException("Numele și prenumele voluntarului sunt obligatorii.");
        }

        Organizatie organizatieAfiliere = organizatieRepository.findById(cifOrganizatie);
        if (organizatieAfiliere == null) {
            throw new RuntimeException("Organizație cu CIF-ul " + cifOrganizatie + " nu a fost găsită.");
        }

        User userPentruVoluntar = authService.register(username, emailUser, parolaUser, Rol.VOLUNTAR);

        voluntar.setUser(userPentruVoluntar);
        voluntar.setDataInrolare(LocalDate.now());
        voluntar.setPuncte(0.0);
        voluntar.setStatus(Status.ACTIV); // Am schimbat în ACTIV pentru consistență
        voluntar.setOrganizatie(organizatieAfiliere);

        // ====================================================================
        // === LOGICA NOUĂ: Asignăm departamentul implicit "Nerepartizat" ===
        // ====================================================================
        // Căutăm departamentul "Nerepartizat" specific acestei organizații
        Departament deptNerepartizat = departamentRepository.findByNumeAndOrganizatieId("Nerepartizat", organizatieAfiliere.getCif());
        if (deptNerepartizat == null) {
            // Măsură de siguranță, în caz că ceva nu a mers bine la crearea organizației
            throw new IllegalStateException("Departamentul implicit 'Nerepartizat' nu a fost găsit. Asigură-te că este creat odată cu organizația.");
        }
        // Asignăm departamentul găsit voluntarului nou.
        voluntar.setDepartament(deptNerepartizat);
        // ====================================================================

        if (voluntarRepository.findSingleByField("user.id", userPentruVoluntar.getId()) != null) {
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

    public Voluntar cautaDupaUser(User user) {
        if (user == null || user.getId() == null) {
            return null;
        }
        return voluntarRepository.findSingleByField("user.id", user.getId());
    }

// Înlocuiește metoda existentă din VoluntarService.java cu aceasta

    @Transactional
    public void actualizeazaVoluntar(Voluntar voluntarModificat) {
        if (voluntarModificat == null || voluntarModificat.getId() == null) {
            throw new RuntimeException("ID-ul voluntarului este necesar pentru actualizare.");
        }

        // Preluăm voluntarul "viu" din baza de date, pe care vom aplica modificările
        Voluntar voluntarExistent = voluntarRepository.findById(voluntarModificat.getId());
        if (voluntarExistent == null) {
            throw new RuntimeException("Voluntar inexistent pentru actualizare cu ID: " + voluntarModificat.getId());
        }

        // Copiem datele personale ale voluntarului din formular pe obiectul din baza de date
        voluntarExistent.setTelefon(voluntarModificat.getTelefon());
        voluntarExistent.setFacultate(voluntarModificat.getFacultate());
        voluntarExistent.setSpecializare(voluntarModificat.getSpecializare());
        voluntarExistent.setAnStudiu(voluntarModificat.getAnStudiu());

        // Copiem statusul (dacă este editabil din altă parte, ex. de către secretar)
        voluntarExistent.setStatus(voluntarModificat.getStatus());

        // Copiem departamentul (dacă este editabil din altă parte)
        if (voluntarModificat.getDepartament() != null) {
            Departament deptNou = departamentRepository.findById(voluntarModificat.getDepartament().getId());
            voluntarExistent.setDepartament(deptNou);
        } else {
            voluntarExistent.setDepartament(null);
        }

        // Preluăm utilizatorul asociat pentru a-i modifica datele (email, rol)
        User userAsociat = voluntarExistent.getUser();
        User userModificat = voluntarModificat.getUser();

        if (userAsociat != null && userModificat != null) {
            // Verificăm dacă emailul a fost schimbat
            if (!userAsociat.getEmail().equals(userModificat.getEmail())) {
                // Verificăm dacă noul email nu este deja folosit de alt cont
                User userCuEmailNou = userRepository.findByEmail(userModificat.getEmail());
                if (userCuEmailNou != null && !userCuEmailNou.getId().equals(userAsociat.getId())) {
                    throw new RuntimeException("Adresa de email '" + userModificat.getEmail() + "' este deja folosită de alt cont.");
                }
                // Dacă totul e în regulă, actualizăm emailul
                userAsociat.setEmail(userModificat.getEmail());
            }

            // Actualizăm și rolul, dacă a fost modificat (util pentru panoul de secretar)
            if (!userAsociat.getRol().equals(userModificat.getRol())) {
                userAsociat.setRol(userModificat.getRol());
            }
        }

        // Fiind într-o metodă @Transactional, containerul va salva automat în baza de date
        // toate modificările făcute pe obiectele "voluntarExistent" și "userAsociat" la finalul metodei.
    }

    // În VoluntarService.java
    @Transactional
    public void stergeVoluntar(Long id) {
        Voluntar voluntar = cautaDupaId(id);

        // VERIFICARE NOUĂ: Verificăm dacă voluntarul este coordonator
        List<Departament> departamenteCoordonate = departamentRepository.findByField("coordonator.id", id);
        if (departamenteCoordonate != null && !departamenteCoordonate.isEmpty()) {
            throw new RuntimeException("Acest voluntar este coordonator pentru departamentul '" +
                    departamenteCoordonate.get(0).getNume() + "' și nu poate fi șters. Schimbați mai întâi coordonatorul.");
        }

        // Verificarea existentă pentru task-uri
        if (voluntar.getTaskuri() != null && !voluntar.getTaskuri().isEmpty()) {
            throw new RuntimeException("Voluntarul (ID: " + id + ") are task-uri delegate și nu poate fi șters.");
        }

        // Ștergerea efectivă (utilizatorul asociat se șterge prin cascade)
        voluntarRepository.delete(voluntar);
        System.out.println("Voluntarul a fost șters.");
    }


    public List<Voluntar> gasesteVoluntariDinOrganizatie(Long organizatieId) {
        return voluntarRepository.findByOrganizatieId(organizatieId);
    }

    public List<Voluntar> gasesteVoluntariDinDepartament(Long departamentId) {
        return voluntarRepository.findByField("departament.id", departamentId);
    }
}