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

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class VoluntarService {

    private final VoluntarRepository voluntarRepository;
    private final AuthService authService;
    private final OrganizatieRepository organizatieRepository;
    private final DepartamentRepository departamentRepository;

    @Inject
    public VoluntarService(VoluntarRepository voluntarRepository,
                           AuthService authService,
                           OrganizatieRepository organizatieRepository,
                           DepartamentRepository departamentRepository) {
        this.voluntarRepository = voluntarRepository;
        this.authService = authService;
        this.organizatieRepository = organizatieRepository;
        this.departamentRepository = departamentRepository;
    }

    protected VoluntarService(){
        this(null,null,null,null );
    }

    @Transactional
    public void adaugaVoluntar(Voluntar voluntar, String username, String emailUser, String parolaUser, Long cifOrganizatie) {
        if (voluntar.getNume() == null || voluntar.getNume().isBlank() ||
                voluntar.getPrenume() == null || voluntar.getPrenume().isBlank()) {
            throw new RuntimeException("Numele și prenumele voluntarului sunt obligatorii.");
        }

        Organizatie organizatieAfiliere = organizatieRepository.findByCif(cifOrganizatie);
        if (organizatieAfiliere == null) {
            throw new RuntimeException("Organizație cu CIF-ul " + cifOrganizatie + " nu a fost găsită.");
        }

        User userPentruVoluntar = authService.register(username, emailUser, parolaUser, Rol.VOLUNTAR);

        voluntar.setUser(userPentruVoluntar);
        voluntar.setDataInrolare(LocalDate.now());
        voluntar.setPuncte(0);
        voluntar.setStatus(Status.COLABORATOR);
        voluntar.setDepartament(null);

        // MODIFICARE CHEIE: Se setează legătura directă cu organizația
        voluntar.setOrganizatie(organizatieAfiliere);

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

    public Voluntar cautaDupaEmail(String email) {
        User user = authService.cautaDupaEmail(email);
        if (user != null && user.getId() != null) {
            return voluntarRepository.findSingleByField("user.id", user.getId());
        }
        return null;
    }

    public Voluntar cautaDupaUser(User user) {
        if (user == null || user.getId() == null) {
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

        existent.setNume(voluntar.getNume());
        existent.setPrenume(voluntar.getPrenume());
        existent.setTelefon(voluntar.getTelefon());
        existent.setFacultate(voluntar.getFacultate());
        existent.setSpecializare(voluntar.getSpecializare());
        existent.setAnStudiu(voluntar.getAnStudiu());
        existent.setPuncte(voluntar.getPuncte());
        existent.setStatus(voluntar.getStatus());
        // Nu modificăm organizația aici, se presupune că un voluntar nu își schimbă organizația.

        if (voluntar.getDepartament() != null && voluntar.getDepartament().getId() != null) {
            if (existent.getDepartament() == null || !existent.getDepartament().getId().equals(voluntar.getDepartament().getId())) {
                Departament dNou = departamentRepository.findById(voluntar.getDepartament().getId());
                if (dNou == null) {
                    throw new RuntimeException("Departamentul specificat (ID: " + voluntar.getDepartament().getId() + ") nu există.");
                }
                existent.setDepartament(dNou);
            }
        } else {
            existent.setDepartament(null);
        }

        voluntarRepository.update(existent);
    }

    @Transactional
    public void stergeVoluntar(Long id) {
        Voluntar voluntar = cautaDupaId(id);
        if (voluntar.getTaskuri() != null && !voluntar.getTaskuri().isEmpty()) {
            throw new RuntimeException("Voluntarul (ID: " + id + ") are task-uri asignate și nu poate fi șters.");
        }
        User userAsociat = voluntar.getUser();
        voluntarRepository.delete(voluntar);
        System.out.println("Voluntarul cu ID " + id + " a fost șters.");
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