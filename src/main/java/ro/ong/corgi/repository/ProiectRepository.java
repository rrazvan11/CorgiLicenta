package ro.ong.corgi.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;
import ro.ong.corgi.model.Enums.StatusProiect;
import ro.ong.corgi.model.GrupareVoluntariProiecte;
import ro.ong.corgi.model.Proiect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProiectRepository extends AbstractRepository<Proiect, Long> {

    public ProiectRepository() {
        super(Proiect.class);
    }

    // ... alte metode existente ...

    public List<Proiect> findByNameAndOrg(String numeProiect, Long organizatieId) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în ProiectRepository");
        }
        TypedQuery<Proiect> q = this.entityManager.createQuery(
                "SELECT p FROM Proiect p WHERE p.numeProiect = :nume AND p.organizatie.id = :orgId",
                Proiect.class
        );
        q.setParameter("nume", numeProiect);
        q.setParameter("orgId", organizatieId);
        return q.getResultList();
    }

    public Proiect findByNume(String numeProiect) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în ProiectRepository");
        }
        TypedQuery<Proiect> query = this.entityManager.createQuery(
                "SELECT p FROM Proiect p WHERE p.numeProiect = :nume", Proiect.class);
        query.setParameter("nume", numeProiect);
        return query.getResultStream().findFirst().orElse(null);
    }
    public List<Proiect> findByStatus(StatusProiect status) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în ProiectRepository");
        }
        TypedQuery<Proiect> query = this.entityManager.createQuery(
                "SELECT p FROM Proiect p WHERE p.status = :status ORDER BY p.dataInceput DESC", Proiect.class);
        query.setParameter("status", status);
        return query.getResultList();
    }
    // În fișierul ProiectRepository.java
// Înlocuiește metoda existentă cu aceasta
// ÎNLOCUIEȘTE complet metoda findByOrganizatieId cu aceasta:

    public List<Proiect> findByOrganizatieId(Long organizatieId) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în ProiectRepository");
        }

        // Pasul 1: Aduce proiectele distincte și coordonatorii lor.
        List<Proiect> proiecte = this.entityManager.createQuery(
                        "SELECT DISTINCT p FROM Proiect p LEFT JOIN FETCH p.coordonatorProiect WHERE p.organizatie.id = :orgId", Proiect.class)
                .setParameter("orgId", organizatieId)
                .getResultList();

        if (proiecte.isEmpty()) {
            return proiecte;
        }

        // Pasul 2: Aduce TOATE participările pentru proiectele găsite, cu voluntarii lor atașați.
        List<GrupareVoluntariProiecte> allParticipari = this.entityManager.createQuery(
                        "SELECT gvp FROM GrupareVoluntariProiecte gvp LEFT JOIN FETCH gvp.voluntar WHERE gvp.proiect IN :proiecte", GrupareVoluntariProiecte.class)
                .setParameter("proiecte", proiecte)
                .getResultList();

        // Pasul 3: Grupează manual participările găsite, având ca cheie ID-ul proiectului.
        Map<Long, List<GrupareVoluntariProiecte>> participariByProiectId = allParticipari.stream()
                .collect(Collectors.groupingBy(gvp -> gvp.getProiect().getId()));

        // --- AICI ESTE MODIFICAREA CRITICĂ ---
        // Pasul 4: Populăm colecția existentă, FĂRĂ a o înlocui.
        for (Proiect p : proiecte) {
            List<GrupareVoluntariProiecte> participariPentruAcestProiect = participariByProiectId.getOrDefault(p.getId(), Collections.emptyList());

            // p.getParticipari() va inițializa colecția lazy-loaded.
            p.getParticipari().clear();
            p.getParticipari().addAll(participariPentruAcestProiect);
        }

        return proiecte;
    }

    public List<Proiect> findByVoluntarId(Long voluntarId) {
        if (this.entityManager == null) {
            throw new IllegalStateException("EntityManager nu este injectat în ProiectRepository");
        }
        // Corecție: Folosim un parametru numit ':status' pentru valoarea Enum
        TypedQuery<Proiect> query = this.entityManager.createQuery(
                "SELECT gvp.proiect FROM GrupareVoluntariProiecte gvp WHERE gvp.voluntar.id = :voluntarId AND gvp.statusAplicatie = :status", Proiect.class);

        query.setParameter("voluntarId", voluntarId);
        // Setăm valoarea pentru parametrul ':status' folosind clasa Enum
        query.setParameter("status", ro.ong.corgi.model.Enums.StatusAplicari.ACCEPTAT);

        return query.getResultList();
    }


}