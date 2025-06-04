package ro.ong.corgi.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import ro.ong.corgi.model.Organizatie;

@ApplicationScoped
public class OrganizatieRepository extends AbstractRepository<Organizatie, Long> {

    public OrganizatieRepository() {
        super(Organizatie.class);
    }

    public Organizatie findByCif(Long cif) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în OrganizatieRepository");
        }
        try {
            TypedQuery<Organizatie> query = this.entityManager.createQuery(
                    "SELECT o FROM Organizatie o WHERE o.cif = :cif", Organizatie.class);
            query.setParameter("cif", cif);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public Organizatie findByEmail(String email) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în OrganizatieRepository");
        }
        try {
            TypedQuery<Organizatie> query = this.entityManager.createQuery(
                    "SELECT o FROM Organizatie o WHERE o.mail = :email", Organizatie.class);
            query.setParameter("email", email);
            // Presupunem că și emailul organizației ar trebui să fie unic, deci getSingleResult
            // Dacă nu, folosește getResultStream().findFirst().orElse(null);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}