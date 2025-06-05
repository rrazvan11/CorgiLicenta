package ro.ong.corgi.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;
import ro.ong.corgi.model.Proiect;
import java.util.List;

@ApplicationScoped
public class ProiectRepository extends AbstractRepository<Proiect, Long> {

    public ProiectRepository() {
        super(Proiect.class);
    }

    // ... alte metode existente ...

    public List<Proiect> findByNameAndOrg(String numeProiect, Long organizatieId) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în ProiectRepository");
        }
        TypedQuery<Proiect> q = this.entityManager.createQuery(
                "SELECT p FROM Proiect p WHERE p.numeProiect = :nume AND p.organizatie.id = :orgId",
                Proiect.class
        );
        q.setParameter("nume", numeProiect);
        q.setParameter("orgId", organizatieId);
        return q.getResultList();
    }

    public Proiect findByNume(String numeProiect) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în ProiectRepository");
        }
        TypedQuery<Proiect> query = this.entityManager.createQuery(
                "SELECT p FROM Proiect p WHERE p.numeProiect = :nume", Proiect.class);
        query.setParameter("nume", numeProiect);
        return query.getResultStream().findFirst().orElse(null);
    }

    public List<Proiect> findByOrganizatieId(Long organizatieId) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în ProiectRepository");
        }
        TypedQuery<Proiect> query = this.entityManager.createQuery(
                "SELECT p FROM Proiect p WHERE p.organizatie.id = :orgId", Proiect.class);
        query.setParameter("orgId", organizatieId);
        return query.getResultList();
    }

    // --- ACEASTA ESTE METODA NECESARĂ ---
    public List<Proiect> findByVoluntarId(Long voluntarId) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în ProiectRepository");
        }
        // Interogarea corectă prin entitatea de legătură GrupareVoluntariProiecte
        TypedQuery<Proiect> query = this.entityManager.createQuery(
                "SELECT gvp.proiect FROM GrupareVoluntariProiecte gvp WHERE gvp.voluntar.id = :voluntarId", Proiect.class);
        query.setParameter("voluntarId", voluntarId);
        return query.getResultList();
    }
    // --- SFÂRȘIT METODĂ NOUĂ ---
}