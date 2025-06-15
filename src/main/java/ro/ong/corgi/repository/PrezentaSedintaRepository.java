package ro.ong.corgi.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;
import ro.ong.corgi.model.Enums.StatusPrezenta;
import ro.ong.corgi.model.PrezentaSedinta;

import java.util.List;

@ApplicationScoped
public class PrezentaSedintaRepository extends AbstractRepository<PrezentaSedinta, Long> {
    public PrezentaSedintaRepository() {
        super(PrezentaSedinta.class);
    }

    public long adunăPrezentSedințaAndStatusIn(Long sedintaId, List<StatusPrezenta> statuses) {
        TypedQuery<Long> query = this.entityManager.createQuery(
                "SELECT COUNT(p) FROM PrezentaSedinta p WHERE p.sedinta.id = :sedintaId AND p.statusPrezenta IN :statuses", Long.class);
        query.setParameter("sedintaId", sedintaId);
        query.setParameter("statuses", statuses);
        return query.getSingleResult();
    }

    public List<PrezentaSedinta> findBySedintaId(Long sedintaId) {
        TypedQuery<PrezentaSedinta> query = this.entityManager.createQuery(
                "SELECT p FROM PrezentaSedinta p JOIN FETCH p.voluntar WHERE p.sedinta.id = :sedintaId", PrezentaSedinta.class);
        query.setParameter("sedintaId", sedintaId);
        return query.getResultList();
    }
}