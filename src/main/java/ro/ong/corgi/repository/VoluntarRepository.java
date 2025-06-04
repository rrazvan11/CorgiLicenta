package ro.ong.corgi.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery; // Asigură-te că acest import este prezent
import ro.ong.corgi.model.Voluntar;
import java.util.List; // Asigură-te că acest import este prezent

@ApplicationScoped
public class VoluntarRepository extends AbstractRepository<Voluntar, Long> {

    public VoluntarRepository() {
        super(Voluntar.class);
    }

    public Voluntar findFirstByEmail(String email) {
        List<Voluntar> rezultate = super.findByField("email", email);
        return rezultate.isEmpty() ? null : rezultate.get(0);
    }

    // --- METODĂ NOUĂ ADĂUGATĂ ---
    public List<Voluntar> findByOrganizatieId(Long organizatieId) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în VoluntarRepository");
        }
        TypedQuery<Voluntar> query = this.entityManager.createQuery(
                "SELECT v FROM Voluntar v WHERE v.departament.organizatie.id = :organizatieId", Voluntar.class);
        query.setParameter("organizatieId", organizatieId);
        return query.getResultList();
    }
    // --- SFÂRȘIT METODĂ NOUĂ ---
}