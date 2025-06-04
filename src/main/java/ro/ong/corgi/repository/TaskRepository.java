package ro.ong.corgi.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;
import ro.ong.corgi.model.Task;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class TaskRepository extends AbstractRepository<Task, Long> {

    public TaskRepository() {
        super(Task.class);
    }

    public List<Task> findByVoluntarId(Long voluntarId) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în TaskRepository");
        }
        TypedQuery<Task> query = this.entityManager.createQuery(
                "SELECT t FROM Task t WHERE t.voluntar.id = :voluntarId", Task.class);
        query.setParameter("voluntarId", voluntarId);
        return query.getResultList();
    }

    public List<Task> findByProiectId(Long proiectId) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în TaskRepository");
        }
        TypedQuery<Task> query = this.entityManager.createQuery(
                "SELECT t FROM Task t WHERE t.proiect.id = :proiectId", Task.class);
        query.setParameter("proiectId", proiectId);
        return query.getResultList();
    }

    public List<Task> findByDeadlineBefore(LocalDate date) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în TaskRepository");
        }
        TypedQuery<Task> query = this.entityManager.createQuery(
                "SELECT t FROM Task t WHERE t.deadline < :date", Task.class);
        query.setParameter("date", date);
        return query.getResultList();
    }
}