package ro.ong.corgi.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;
import ro.ong.corgi.model.Enums.StatusProiect;
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
    public List<Proiect> findByStatus(StatusProiect status) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în ProiectRepository");
        }
        TypedQuery<Proiect> query = this.entityManager.createQuery(
                "SELECT p FROM Proiect p WHERE p.status = :status ORDER BY p.dataInceput DESC", Proiect.class);
        query.setParameter("status", status);
        return query.getResultList();
    }
    public List<Proiect> findByOrganizatieId(Long organizatieId) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în ProiectRepository");
        }

        // Interogarea finală. Aduce proiectele, participările și coordonatorul de proiect.
        String jpql = "SELECT DISTINCT p FROM Proiect p " +
                "LEFT JOIN FETCH p.participari " +
                "LEFT JOIN FETCH p.coordonatorProiect " +
                "WHERE p.organizatie.id = :orgId";

        TypedQuery<Proiect> query = this.entityManager.createQuery(jpql, Proiect.class);
        query.setParameter("orgId", organizatieId);
        return query.getResultList();
    }

    public List<Proiect> findByVoluntarId(Long voluntarId) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în ProiectRepository");
        }
        // Corecție: Folosim un parametru numit ':status' pentru valoarea Enum
        TypedQuery<Proiect> query = this.entityManager.createQuery(
                "SELECT gvp.proiect FROM GrupareVoluntariProiecte gvp WHERE gvp.voluntar.id = :voluntarId AND gvp.statusAplicatie = :status", Proiect.class);

        query.setParameter("voluntarId", voluntarId);
        // Setăm valoarea pentru parametrul ':status' folosind clasa Enum
        query.setParameter("status", ro.ong.corgi.model.Enums.StatusAplicari.ACCEPTAT);

        return query.getResultList();
    }

}