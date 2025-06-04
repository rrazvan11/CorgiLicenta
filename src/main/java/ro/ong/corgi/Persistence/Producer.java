package ro.ong.corgi.persistence; // Sau ro.ong.corgi.config

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

@ApplicationScoped // Acest bean va "trăi" pe durata întregii aplicații
public class Producer {

    @PersistenceUnit(unitName = "corgi-persistence-unit") // Numele unității de persistență din persistence.xml
    private EntityManagerFactory emf;

    @Produces // Indică faptul că această metodă produce un bean CDI
    @RequestScoped // Un EntityManager nou va fi creat pentru fiecare request HTTP și distrus la finalul request-ului
    public EntityManager createEntityManager() {
        return this.emf.createEntityManager();
    }

    // Această metodă este apelată automat de CDI la finalul request-ului
    // pentru a închide EntityManager-ul produs de metoda createEntityManager()
    public void closeEntityManager(@Disposes EntityManager entityManager) {
        if (entityManager.isOpen()) {
            entityManager.close();
        }
    }
}