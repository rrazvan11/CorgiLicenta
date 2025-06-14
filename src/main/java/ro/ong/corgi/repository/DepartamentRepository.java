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

    public Departament findByNume(String nume) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în DepartamentRepository");
        }
        try {
            TypedQuery<Departament> query = this.entityManager.createQuery(
                    // Presupunând că vrei un departament specific dintr-o organizație anume,
                    // ar trebui să adaugi și un filtru pe organizatie_id.
                    // Momentan, caută un departament cu acest nume oriunde.
                    // Pentru unicitate per organizație, interogarea ar trebui să fie mai specifică,
                    // posibil necesitând un ID de organizație ca parametru.
                    // "SELECT d FROM Departament d WHERE d.nume = :nume AND d.organizatie.id = :orgId"
                    "SELECT d FROM Departament d WHERE d.nume = :nume", Departament.class);
            query.setParameter("nume", nume);
            // Dacă te aștepți la mai multe cu același nume (în organizații diferite)
            // și vrei doar primul, folosește getResultStream().findFirst().orElse(null);
            // Dar dacă numele e unic global (ceea ce nu e cazul după ultima modificare), getSingleResult e OK.
            // Având în vedere că numele e unic per organizație, această metodă ar trebui să primească și ID-ul organizației.
            // Voi lăsa getSingleResult, dar cu mențiunea că logica poate necesita ajustare.
            return query.getResultStream().findFirst().orElse(null); // Mai sigur decât getSingleResult dacă pot fi mai multe
        } catch (NoResultException e) { // NoResultException nu se aruncă cu findFirst().orElse(null)
            return null;
        }
    }
    // Adaugă această metodă în clasa DepartamentRepository.java
    public List<Departament> gasesteDepartamenteVoluntari() {
        TypedQuery<Departament> query = this.entityManager.createQuery(
                "SELECT DISTINCT d FROM Departament d LEFT JOIN FETCH d.voluntari", Departament.class);
        return query.getResultList();
    }
    // Metodă ajustată pentru a căuta după nume și ID organizație
    public Departament findByNumeAndOrganizatieId(String nume, Long organizatieId) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în DepartamentRepository");
        }
        try {
            TypedQuery<Departament> query = this.entityManager.createQuery(
                    "SELECT d FROM Departament d WHERE d.nume = :nume AND d.organizatie.id = :organizatieId", Departament.class);
            query.setParameter("nume", nume);
            query.setParameter("organizatieId", organizatieId);
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