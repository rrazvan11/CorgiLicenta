package ro.ong.corgi.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;
import ro.ong.corgi.model.GrupareVoluntariProiecte;

import java.util.List;

@ApplicationScoped
public class GrupareVoluntariProiecteRepository extends AbstractRepository<GrupareVoluntariProiecte, Long> {
    public GrupareVoluntariProiecteRepository() {
        super(GrupareVoluntariProiecte.class);
    }


    public List<GrupareVoluntariProiecte> findByProiectIdWithVoluntar(Long proiectId) {
        TypedQuery<GrupareVoluntariProiecte> query = this.entityManager.createQuery(
                "SELECT gvp FROM GrupareVoluntariProiecte gvp LEFT JOIN FETCH gvp.voluntar WHERE gvp.proiect.id = :proiectId",
                GrupareVoluntariProiecte.class
        );
        query.setParameter("proiectId", proiectId);
        return query.getResultList();
    }
}