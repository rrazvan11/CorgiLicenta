package ro.ong.corgi.repository;

import jakarta.inject.Inject; // Pentru @Inject
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
// import jakarta.annotation.PostConstruct; // Opțional, pentru verificare

import java.util.List;

public abstract class AbstractRepository<T, ID> {

    @Inject // Injectăm EntityManager-ul produs de clasa ta Producer
    protected EntityManager entityManager;

    protected final Class<T> entityClass;

    // Constructorul primește acum doar clasa entității
    // EntityManager-ul va fi injectat automat de CDI înainte ca orice metodă de business să fie apelată
    public AbstractRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    // Opțional: metodă de inițializare pentru a verifica dacă injectarea a avut loc
    // Poate fi utilă în faza de dezvoltare pentru a prinde erori de configurare CDI.
    // @PostConstruct
    // public void init() {
    //     if (this.entityManager == null) {
    //         throw new IllegalStateException("EntityManager was not injected into AbstractRepository for " + entityClass.getSimpleName() +
    //                                        ". Verifică clasa Producer și beans.xml.");
    //     }
    // }

    public void save(T entity) {
        // Verificăm dacă entityManager este null pentru a oferi un mesaj mai clar în caz de probleme de injectare
        if (entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în " + this.getClass().getSimpleName() + " pentru entitatea " + entityClass.getSimpleName());
        }
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.persist(entity);
            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Eroare la salvarea entității " + entityClass.getSimpleName(), e);
        }
    }

    public void update(T entity) {
        if (entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în " + this.getClass().getSimpleName() + " pentru entitatea " + entityClass.getSimpleName());
        }
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.merge(entity);
            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Eroare la actualizarea entității " + entityClass.getSimpleName(), e);
        }
    }

    public T findById(ID id) {
        if (entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în " + this.getClass().getSimpleName() + " pentru entitatea " + entityClass.getSimpleName());
        }
        return entityManager.find(entityClass, id);
    }

    public List<T> findAll() {
        if (entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în " + this.getClass().getSimpleName() + " pentru entitatea " + entityClass.getSimpleName());
        }
        String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e";
        TypedQuery<T> query = entityManager.createQuery(jpql, entityClass);
        return query.getResultList();
    }

    public void delete(T entity) {
        if (entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în " + this.getClass().getSimpleName() + " pentru entitatea " + entityClass.getSimpleName());
        }
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Eroare la ștergerea entității " + entityClass.getSimpleName(), e);
        }
    }

    public void deleteById(ID id) {
        T entity = findById(id); // findById va verifica și el entityManager
        if (entity != null) {
            delete(entity);
        }
    }

    public List<T> findByField(String fieldName, Object value) {
        if (entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în " + this.getClass().getSimpleName() + " pentru entitatea " + entityClass.getSimpleName());
        }
        String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e WHERE e." + fieldName + " = :value";
        TypedQuery<T> query = entityManager.createQuery(jpql, entityClass);
        query.setParameter("value", value);
        return query.getResultList();
    }

    public T findSingleByField(String fieldName, Object value) {
        if (entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în " + this.getClass().getSimpleName() + " pentru entitatea " + entityClass.getSimpleName());
        }
        String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e WHERE e." + fieldName + " = :value";
        TypedQuery<T> query = entityManager.createQuery(jpql, entityClass);
        query.setParameter("value", value);
        try {
            return query.getSingleResult();
        } catch (Exception e) { // Ideal ar fi să prinzi NoResultException specific
            return null;
        }
    }
}