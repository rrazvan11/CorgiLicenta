package ro.ong.corgi.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;
import ro.ong.corgi.model.Enums.StatusProiect;
import ro.ong.corgi.model.GrupareVoluntariProiecte;
import ro.ong.corgi.model.Proiect;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProiectRepository extends AbstractRepository<Proiect, Long> {

    public ProiectRepository() {
        super(Proiect.class);
    }

    public List<Proiect> findByNameAndOrg(String numeProiect, Long organizatieCif) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în ProiectRepository");
        }
        TypedQuery<Proiect> q = this.entityManager.createQuery(
                "SELECT p FROM Proiect p WHERE p.numeProiect = :nume AND p.organizatie.id = :orgCif",
                Proiect.class
        );
        q.setParameter("nume", numeProiect);
        q.setParameter("orgCif", organizatieCif);
        return q.getResultList();
    }

    // METODA findByNume A FOST ȘTEARSĂ

    public List<Proiect> findByStatus(StatusProiect status) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în ProiectRepository");
        }
        TypedQuery<Proiect> query = this.entityManager.createQuery(
                "SELECT p FROM Proiect p WHERE p.status = :status ORDER BY p.dataInceput DESC", Proiect.class);
        query.setParameter("status", status);
        return query.getResultList();
    }

    public List<Proiect> findByOrganizatieId(Long organizatieCif) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în ProiectRepository");
        }

        List<Proiect> proiecte = this.entityManager.createQuery(
                        "SELECT DISTINCT p FROM Proiect p LEFT JOIN FETCH p.coordonatorProiect WHERE p.organizatie.id = :orgCif", Proiect.class)
                .setParameter("orgCif", organizatieCif)
                .getResultList();

        if (proiecte.isEmpty()) {
            return proiecte;
        }

        List<GrupareVoluntariProiecte> allParticipari = this.entityManager.createQuery(
                        "SELECT gvp FROM GrupareVoluntariProiecte gvp LEFT JOIN FETCH gvp.voluntar WHERE gvp.proiect IN :proiecte", GrupareVoluntariProiecte.class)
                .setParameter("proiecte", proiecte)
                .getResultList();

        Map<Long, List<GrupareVoluntariProiecte>> participariByProiectId = allParticipari.stream()
                .collect(Collectors.groupingBy(gvp -> gvp.getProiect().getId()));

        for (Proiect p : proiecte) {
            List<GrupareVoluntariProiecte> participariPentruAcestProiect = participariByProiectId.getOrDefault(p.getId(), Collections.emptyList());

            p.getParticipari().clear();
            p.getParticipari().addAll(participariPentruAcestProiect);
        }

        return proiecte;
    }

    public List<Proiect> findByVoluntarId(Long voluntarId) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în ProiectRepository");
        }
        TypedQuery<Proiect> query = this.entityManager.createQuery(
                "SELECT gvp.proiect FROM GrupareVoluntariProiecte gvp WHERE gvp.voluntar.id = :voluntarId AND gvp.statusAplicatie = :status", Proiect.class);

        query.setParameter("voluntarId", voluntarId);
        query.setParameter("status", ro.ong.corgi.model.Enums.StatusAplicari.ACCEPTAT);

        return query.getResultList();
    }
}