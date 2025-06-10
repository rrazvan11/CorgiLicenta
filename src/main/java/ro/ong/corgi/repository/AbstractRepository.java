package ro.ong.corgi.repository;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import java.util.List;

/**
 * Repository abstract pentru operațiuni CRUD de bază.
 * Această versiune este adaptată pentru tranzacții gestionate de container (JTA),
 * deci nu mai conține management manual al tranzacțiilor (begin, commit, rollback).
 */
public abstract class AbstractRepository<T, ID> {

    @Inject // EntityManager-ul este injectat de container (via Producer.java)
    protected EntityManager entityManager;

    protected final Class<T> entityClass;

    public AbstractRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    /**
     * Salvează o entitate nouă în baza de date.
     * Tranzacția este gestionată de server (prin @Transactional în serviciu).
     */
    public void save(T entity) {
        if (entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat.");
        }
        entityManager.persist(entity);
    }

    /**
     * Actualizează o entitate existentă în baza de date.
     * Tranzacția este gestionată de server (prin @Transactional în serviciu).
     */
    public void update(T entity) {
        if (entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat.");
        }
        entityManager.merge(entity);
    }

    /**
     * Șterge o entitate din baza de date.
     * Tranzacția este gestionată de server (prin @Transactional în serviciu).
     */
    public void delete(T entity) {
        if (entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat.");
        }
        // Asigură-te că entitatea este gestionată ("managed") înainte de a o șterge
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    // --- Metodele de citire nu necesită tranzacții de scriere ---

    public T findById(ID id) {
        if (entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat.");
        }
        return entityManager.find(entityClass, id);
    }

    public List<T> findAll() {
        if (entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat.");
        }
        String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e";
        TypedQuery<T> query = entityManager.createQuery(jpql, entityClass);
        return query.getResultList();
    }

    public List<T> findByField(String fieldName, Object value) {
        if (entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat.");
        }
        String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e WHERE e." + fieldName + " = :value";
        TypedQuery<T> query = entityManager.createQuery(jpql, entityClass);
        query.setParameter("value", value);
        return query.getResultList();
    }

    public T findSingleByField(String fieldName, Object value) {
        if (entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat.");
        }
        String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e WHERE e." + fieldName + " = :value";
        TypedQuery<T> query = entityManager.createQuery(jpql, entityClass);
        query.setParameter("value", value);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
