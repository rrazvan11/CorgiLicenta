package ro.ong.corgi.Persistence; // Sau ro.ong.corgi.config

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

@ApplicationScoped
public class Producer {

    @PersistenceUnit(unitName = "corgi-persistence-unit")
    private EntityManagerFactory emf;

    @Produces
    @RequestScoped // Un EntityManager nou va fi creat pentru fiecare request HTTP și distrus la finalul request-ului
    public EntityManager createEntityManager() {
        return this.emf.createEntityManager();
    }

    public void closeEntityManager(@Disposes EntityManager entityManager) {
        if (entityManager.isOpen()) {
            entityManager.close();
        }
    }
}