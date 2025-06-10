package ro.ong.corgi.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.ong.corgi.model.Enums.Rol;
import ro.ong.corgi.model.Enums.TaskStatus;
import ro.ong.corgi.model.Task;
import ro.ong.corgi.model.User;
import ro.ong.corgi.model.Voluntar;
import ro.ong.corgi.model.Proiect;
import ro.ong.corgi.repository.TaskRepository;
// VoluntarService și ProiectService sunt deja injectabile

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class TaskService {

    private final TaskRepository taskRepository;
    private final VoluntarService voluntarService; // Pentru a valida existența voluntarului
    private final ProiectService proiectService;   // Pentru a valida existența proiectului

    @Inject
    public TaskService(TaskRepository taskRepository, VoluntarService voluntarService, ProiectService proiectService) {
        this.taskRepository = taskRepository;
        this.voluntarService = voluntarService;
        this.proiectService = proiectService;
    }
    protected TaskService(){
        this(null,null,null);
    }

    /**
     * Adaugă un task nou.
     * Verifică existența voluntarului și a proiectului asociat.
     * Setează statusul inițial pe WAITING.
     * Deadline-ul trebuie să fie azi sau în viitor.
     */
    @Transactional
    public void adaugaTask(Task task) {
        if (task.getTitlu() == null || task.getTitlu().isBlank()) {
            throw new RuntimeException("Titlul task-ului este obligatoriu.");
        }
        if (task.getDescriere() == null || task.getDescriere().isBlank()) { // Adăugat validare descriere
            throw new RuntimeException("Descrierea task-ului este obligatorie.");
        }
        if (task.getDeadline() == null || task.getDeadline().isBefore(LocalDate.now())) {
            throw new RuntimeException("Deadline invalid. Trebuie să fie azi sau în viitor.");
        }
        if (task.getVoluntar() == null || task.getVoluntar().getId() == null) {
            throw new RuntimeException("Task-ul trebuie asociat unui voluntar valid.");
        }
        if (task.getProiect() == null || task.getProiect().getId() == null) {
            throw new RuntimeException("Task-ul trebuie asociat unui proiect valid.");
        }

        // Validează și preia entitățile gestionate pentru Voluntar și Proiect
        Voluntar v = voluntarService.cautaDupaId(task.getVoluntar().getId()); // cautaDupaId aruncă excepție dacă nu există
        Proiect p = proiectService.cautaDupaId(task.getProiect().getId());   // cautaDupaId aruncă excepție dacă nu există

        task.setVoluntar(v); // Setează entitatea Voluntar gestionată
        task.setProiect(p);  // Setează entitatea Proiect gestionată
        task.setStatus(TaskStatus.WAITING); // Setează statusul inițial

        taskRepository.save(task);
    }

    public Task cautaDupaId(Long id) {
        Task t = taskRepository.findById(id);
        if (t == null) {
            throw new RuntimeException("Task inexistent: " + id);
        }
        return t;
    }

    public List<Task> toateTaskurile() {
        return taskRepository.findAll();
    }

    /**
     * Actualizează un task existent.
     * Unele câmpuri (ex: proiectul, voluntarul inițial) s-ar putea să nu fie modificabile
     * sau modificarea lor să necesite logica specifică.
     */
    @Transactional
    public void actualizeazaTask(Task task) {
        if (task.getId() == null) {
            throw new RuntimeException("ID-ul task-ului este necesar pentru actualizare.");
        }
        Task existent = taskRepository.findById(task.getId());
        if (existent == null) {
            throw new RuntimeException("Task inexistent: " + task.getId());
        }

        // Validări similare cu cele de la adăugare, dacă e cazul
        if (task.getTitlu() == null || task.getTitlu().isBlank()) {
            throw new RuntimeException("Titlul task-ului este obligatoriu.");
        }
        if (task.getDescriere() == null || task.getDescriere().isBlank()) {
            throw new RuntimeException("Descrierea task-ului este obligatorie.");
        }
        if (task.getDeadline() == null || task.getDeadline().isBefore(LocalDate.now())) {
            // Permitem actualizarea deadline-ului doar dacă e în viitor, chiar dacă cel vechi a trecut
            // Dar dacă task-ul e deja DONE, poate nu mai permitem schimbarea deadline-ului?
            // Asta e logica de business de decis.
        }
        if (task.getStatus() == null) { // Statusul ar trebui să fie mereu prezent
            throw new RuntimeException("Statusul task-ului este obligatoriu.");
        }


        // Actualizează câmpurile permise
        existent.setTitlu(task.getTitlu());
        existent.setDescriere(task.getDescriere());
        existent.setDeadline(task.getDeadline());
        existent.setStatus(task.getStatus());

        // Dacă se permite schimbarea voluntarului sau proiectului pentru un task existent:
        if (task.getVoluntar() != null && task.getVoluntar().getId() != null &&
                !existent.getVoluntar().getId().equals(task.getVoluntar().getId())) {
            Voluntar vNou = voluntarService.cautaDupaId(task.getVoluntar().getId());
            existent.setVoluntar(vNou);
        }
        if (task.getProiect() != null && task.getProiect().getId() != null &&
                !existent.getProiect().getId().equals(task.getProiect().getId())) {
            Proiect pNou = proiectService.cautaDupaId(task.getProiect().getId());
            existent.setProiect(pNou);
        }

        taskRepository.update(existent);
    }
    @Transactional
    public void stergeTask(Long id) {
        Task t = taskRepository.findById(id);
        if (t == null) {
            throw new RuntimeException("Task inexistent: " + id);
        }
        taskRepository.delete(t);
    }

    public List<Task> findByVoluntar(Long voluntarId) {
        // Verifică dacă voluntarul există
        voluntarService.cautaDupaId(voluntarId); // Aruncă excepție dacă nu există
        return taskRepository.findByVoluntarId(voluntarId);
    }

    public List<Task> findByProiect(Long proiectId) {
        // Verifică dacă proiectul există
        proiectService.cautaDupaId(proiectId); // Aruncă excepție dacă nu există
        return taskRepository.findByProiectId(proiectId);
    }

    @Transactional
    public void completeTask(Long taskId, User loggedInUser) {
        Task task = cautaDupaId(taskId);
        Voluntar voluntarAsignat = task.getVoluntar();

        // Verifică dacă utilizatorul logat este cel asignat task-ului
        // Sau dacă are un rol care îi permite să modifice (de ex. Coordonator al proiectului)
        // Această verificare de permisiune trebuie rafinată.
        boolean canComplete = false;
        if (loggedInUser.getRol() == Rol.VOLUNTAR && voluntarAsignat.getUser().getId().equals(loggedInUser.getId())) {
            canComplete = true;
        } else if (loggedInUser.getRol() == Rol.COORDONATOR) {
            // Aici ar trebui verificat dacă coordonatorul este responsabil pentru proiectul task-ului
            // Pentru simplitate, momentan permitem oricărui coordonator.
            // Acest aspect necesită o logică de permisiuni mai detaliată ulterior.
            canComplete = true;
        }

        if (!canComplete) {
            throw new RuntimeException("Nu aveți permisiunea să finalizați acest task.");
        }

        if (task.getStatus() == TaskStatus.DONE) {
            throw new RuntimeException("Task-ul este deja finalizat.");
        }

        task.setStatus(TaskStatus.DONE);

        if (task.getPuncteTask() != null && task.getPuncteTask() > 0) {
            voluntarAsignat.setPuncte(voluntarAsignat.getPuncte() + task.getPuncteTask());
            // Presupunem că ai injectat VoluntarRepository sau VoluntarService pentru a salva voluntarul
            // De exemplu, dacă ai VoluntarService:
            voluntarService.actualizeazaVoluntar(voluntarAsignat); // Asigură-te că această metodă salvează în DB
        }
        taskRepository.update(task); // Salvează task-ul actualizat
    }
}