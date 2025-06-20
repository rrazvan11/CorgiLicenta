package ro.ong.corgi.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;
import ro.ong.corgi.model.Voluntar;
import java.util.List;

@ApplicationScoped
public class VoluntarRepository extends AbstractRepository<Voluntar, Long> {

    public VoluntarRepository() {
        super(Voluntar.class);
    }

    // MODIFICAT: Numele parametrului și utilizarea lui au fost actualizate pentru claritate.
    public List<Voluntar> findByOrganizatieId(Long organizatieCif) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în VoluntarRepository");
        }
        TypedQuery<Voluntar> query = this.entityManager.createQuery(
                "SELECT v FROM Voluntar v WHERE v.organizatie.id = :organizatieCif", Voluntar.class);
        query.setParameter("organizatieCif", organizatieCif);
        return query.getResultList();
    }

    // MODIFICAT: Numele parametrului și utilizarea lui au fost actualizate pentru claritate.
    public long countByOrganizatieId(Long organizatieCif) {
        TypedQuery<Long> query = this.entityManager.createQuery(
                "SELECT COUNT(v) FROM Voluntar v WHERE v.organizatie.id = :organizatieCif", Long.class);
        query.setParameter("organizatieCif", organizatieCif);
        return query.getSingleResult();
    }

    public List<Voluntar> findVoluntariAcceptatiInProiect(Long proiectId) {
        String jpql = "SELECT gvp.voluntar FROM GrupareVoluntariProiecte gvp " +
                "WHERE gvp.proiect.id = :proiectId " +
                "AND gvp.statusAplicatie = :status";
        TypedQuery<Voluntar> query = this.entityManager.createQuery(jpql, Voluntar.class);
        query.setParameter("proiectId", proiectId);
        query.setParameter("status", ro.ong.corgi.model.Enums.StatusAplicari.ACCEPTAT);
        return query.getResultList();
    }
}