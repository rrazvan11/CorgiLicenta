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
    public List<Voluntar> findByOrganizatieId(Long organizatieId) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în VoluntarRepository");
        }
        TypedQuery<Voluntar> query = this.entityManager.createQuery(
                "SELECT v FROM Voluntar v WHERE v.organizatie.id = :organizatieId", Voluntar.class);
        query.setParameter("organizatieId", organizatieId);
        return query.getResultList();
    }
    public long countByOrganizatieId(Long organizatieId) {
        TypedQuery<Long> query = this.entityManager.createQuery(
                "SELECT COUNT(v) FROM Voluntar v WHERE v.organizatie.id = :organizatieId", Long.class);
        query.setParameter("organizatieId", organizatieId);
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