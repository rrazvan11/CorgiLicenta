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

}