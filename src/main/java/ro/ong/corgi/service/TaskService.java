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

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class TaskService {

    private final TaskRepository taskRepository;
    private final VoluntarService voluntarService;
    private final ProiectService proiectService;

    @Inject
    public TaskService(TaskRepository taskRepository, VoluntarService voluntarService, ProiectService proiectService) {
        this.taskRepository = taskRepository;
        this.voluntarService = voluntarService;
        this.proiectService = proiectService;
    }

    protected TaskService(){
        this(null,null,null);
    }

    @Transactional
    public void adaugaTask(Task task) {
        if (task.getTitlu() == null || task.getTitlu().isBlank()) {
            throw new RuntimeException("Titlul task-ului este obligatoriu.");
        }
        if (task.getDescriere() == null || task.getDescriere().isBlank()) {
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

        Voluntar v = voluntarService.cautaDupaId(task.getVoluntar().getId());
        Proiect p = proiectService.cautaDupaId(task.getProiect().getId());

        task.setVoluntar(v);
        task.setProiect(p);
        task.setStatus(TaskStatus.WAITING);

        taskRepository.save(task);
    }

    public Task cautaDupaId(Long id) {
        Task t = taskRepository.findById(id);
        if (t == null) {
            throw new RuntimeException("Task inexistent: " + id);
        }
        return t;
    }

    // METODA toateTaskurile A FOST ȘTEARSĂ

    @Transactional
    public void actualizeazaTask(Task task) {
        if (task.getId() == null) {
            throw new RuntimeException("ID-ul task-ului este necesar pentru actualizare.");
        }
        Task existent = taskRepository.findById(task.getId());
        if (existent == null) {
            throw new RuntimeException("Task inexistent: " + task.getId());
        }

        if (task.getTitlu() == null || task.getTitlu().isBlank()) {
            throw new RuntimeException("Titlul task-ului este obligatoriu.");
        }
        if (task.getDescriere() == null || task.getDescriere().isBlank()) {
            throw new RuntimeException("Descrierea task-ului este obligatorie.");
        }
        if (task.getDeadline() == null || task.getDeadline().isBefore(LocalDate.now())) {
        }
        if (task.getStatus() == null) {
            throw new RuntimeException("Statusul task-ului este obligatoriu.");
        }

        existent.setTitlu(task.getTitlu());
        existent.setDescriere(task.getDescriere());
        existent.setDeadline(task.getDeadline());
        existent.setStatus(task.getStatus());

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
        voluntarService.cautaDupaId(voluntarId);
        return taskRepository.findByVoluntarId(voluntarId);
    }

    public List<Task> findByProiect(Long proiectId) {
        proiectService.cautaDupaId(proiectId);
        return taskRepository.findByProiectId(proiectId);
    }

    @Transactional
    public void completeTask(Long taskId, User loggedInUser) {
        Task task = cautaDupaId(taskId);
        Voluntar voluntarAsignat = task.getVoluntar();

        boolean canComplete = false;
        if (loggedInUser.getRol() == Rol.VOLUNTAR && voluntarAsignat.getUser().getId().equals(loggedInUser.getId())) {
            canComplete = true;
        } else if (loggedInUser.getRol() == Rol.COORDONATOR) {
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
            double puncteActuale = voluntarAsignat.getPuncte() != null ? voluntarAsignat.getPuncte() : 0.0;
            voluntarAsignat.setPuncte(puncteActuale + task.getPuncteTask());
        }
    }
}