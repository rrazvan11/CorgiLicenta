package ro.ong.corgi.repository;

import jakarta.enterprise.context.ApplicationScoped; // Pentru @ApplicationScoped
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import ro.ong.corgi.model.User;

@ApplicationScoped // 1. UserRepository devine un CDI bean
public class UserRepository extends AbstractRepository<User, Long> {

    // 2. Constructorul acum apelează doar super() cu clasa entității
    public UserRepository() {
        super(User.class);
    }

    // Metodele specifice (findByEmail, findByUsername) folosesc this.entityManager
    // care este injectat în clasa părinte AbstractRepository
    public User findByEmail(String email) {
        if (this.entityManager == null) { // Verificare adițională, deși nu ar trebui să fie null
            throw new IllegalStateException("EntityManager nu este injectat în UserRepository");
        }
        try {
            TypedQuery<User> query = this.entityManager.createQuery(
                    "SELECT u FROM User u WHERE u.email = :email AND u.activ = true", User.class);
            query.setParameter("email", email);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public User findByUsername(String username) {
        if (this.entityManager == null) { // Verificare adițională
            throw new IllegalStateException("EntityManager nu este injectat în UserRepository");
        }
        try {
            TypedQuery<User> query = this.entityManager.createQuery(
                    "SELECT u FROM User u WHERE u.username = :username AND u.activ = true", User.class);
            query.setParameter("username", username);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}