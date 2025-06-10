package ro.ong.corgi.repository;

import jakarta.enterprise.context.ApplicationScoped;
import ro.ong.corgi.model.Sedinta;

@ApplicationScoped
public class SedintaRepository extends AbstractRepository<Sedinta, Long> {
    public SedintaRepository() {
        super(Sedinta.class);
    }
}