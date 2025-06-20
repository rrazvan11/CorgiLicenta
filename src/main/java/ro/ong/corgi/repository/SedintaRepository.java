package ro.ong.corgi.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;
import ro.ong.corgi.model.Enums.TipSedinta;
import ro.ong.corgi.model.Sedinta;

import java.util.List;

@ApplicationScoped
public class SedintaRepository extends AbstractRepository<Sedinta, Long> {
    public SedintaRepository() {
        super(Sedinta.class);
    }

    public List<Sedinta> findByOrganizatieIdAndTip(Long organizatieCif, TipSedinta tip) {
        TypedQuery<Sedinta> query = this.entityManager.createQuery(
                "SELECT s FROM Sedinta s WHERE s.organizatie.id = :orgCif AND s.tipSedinta = :tip ORDER BY s.dataSedinta DESC", Sedinta.class);
        query.setParameter("orgCif", organizatieCif);
        query.setParameter("tip", tip);
        return query.getResultList();
    }

}