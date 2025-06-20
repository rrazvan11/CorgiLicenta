package ro.ong.corgi.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import ro.ong.corgi.model.Departament;
import java.util.List;

@ApplicationScoped
public class DepartamentRepository extends AbstractRepository<Departament, Long> {

    public DepartamentRepository() {
        super(Departament.class);
    }

    public List<Departament> gasesteDepartamenteVoluntari() {
        TypedQuery<Departament> query = this.entityManager.createQuery(
                "SELECT DISTINCT d FROM Departament d LEFT JOIN FETCH d.voluntari", Departament.class);
        return query.getResultList();
    }

    // Metodă ajustată pentru a căuta după nume și CIF organizație
    public Departament findByNumeAndOrganizatieId(String nume, Long organizatieCif) { // MODIFICAT: Numele parametrului
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în DepartamentRepository");
        }
        try {
            // MODIFICAT: Numele parametrului în interogare
            TypedQuery<Departament> query = this.entityManager.createQuery(
                    "SELECT d FROM Departament d WHERE d.nume = :nume AND d.organizatie.id = :organizatieCif", Departament.class);
            query.setParameter("nume", nume);
            query.setParameter("organizatieCif", organizatieCif); // MODIFICAT: Numele parametrului
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }


    public List<Departament> findAllOrderedByNume() {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în DepartamentRepository");
        }
        TypedQuery<Departament> query = this.entityManager.createQuery(
                "SELECT d FROM Departament d ORDER BY d.nume", Departament.class);
        return query.getResultList();
    }
}