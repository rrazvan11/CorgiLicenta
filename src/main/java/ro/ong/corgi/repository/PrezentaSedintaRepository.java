package ro.ong.corgi.repository;

import jakarta.enterprise.context.ApplicationScoped;
import ro.ong.corgi.model.PrezentaSedinta;

@ApplicationScoped
public class PrezentaSedintaRepository extends AbstractRepository<PrezentaSedinta, Long> {
    public PrezentaSedintaRepository() {
        super(PrezentaSedinta.class);
    }
}