package ro.ong.corgi.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.ong.corgi.model.*;
import ro.ong.corgi.model.Enums.StatusPrezenta;
import ro.ong.corgi.repository.PrezentaSedintaRepository;
import ro.ong.corgi.repository.SedintaRepository;
import ro.ong.corgi.repository.VoluntarRepository;

import java.util.List;

@ApplicationScoped
public class SedintaService {

    @Inject
    private SedintaRepository sedintaRepository;

    @Inject
    private PrezentaSedintaRepository prezentaSedintaRepository;

    @Inject
    private VoluntarRepository voluntarRepository;

    protected SedintaService() {}

    @Transactional
    public Sedinta creeazaSedintaCuPrezente(Sedinta sedinta, List<PrezentaSedinta> prezente) {
        if (sedinta == null || sedinta.getDataSedinta() == null || sedinta.getOrganizatie() == null) {
            throw new RuntimeException("Data ședinței și organizația sunt obligatorii.");
        }

        sedintaRepository.save(sedinta);

        final double PUNCTE_PREZENTA = 1.5;

        for (PrezentaSedinta prezenta : prezente) {
            prezenta.setSedinta(sedinta);
            prezentaSedintaRepository.save(prezenta);

            if (prezenta.getStatusPrezenta() == StatusPrezenta.PREZENT) {
                Voluntar voluntarDeActualizat = voluntarRepository.findById(prezenta.getVoluntar().getId());
                if (voluntarDeActualizat != null) {
                    double puncteCurente = (voluntarDeActualizat.getPuncte() != null) ? voluntarDeActualizat.getPuncte() : 0.0;
                    voluntarDeActualizat.setPuncte(puncteCurente + PUNCTE_PREZENTA);
                    voluntarRepository.update(voluntarDeActualizat);
                }
            }
        }
        return sedinta;
    }

    public List<Sedinta> getSedinteByOrganizatie(Long organizatieId) {
        // MODIFICAT: Apelează metoda din repository în loc să construiască query-ul aici.
        return sedintaRepository.findByOrganizatieId(organizatieId);
    }
}