package ro.ong.corgi.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.ong.corgi.model.Enums.StatusPrezenta;
import ro.ong.corgi.model.PrezentaSedinta;
import ro.ong.corgi.model.Sedinta;
import ro.ong.corgi.model.Voluntar;
import ro.ong.corgi.repository.PrezentaSedintaRepository;
import ro.ong.corgi.repository.SedintaRepository;
import ro.ong.corgi.repository.VoluntarRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class SedintaService {

    @Inject
    private SedintaRepository sedintaRepository;

    @Inject
    private VoluntarRepository voluntarRepository;

    @Inject
    private PrezentaSedintaRepository prezentaSedintaRepository;

    private static final double PUNCTE_PER_PREZENTA = 1.5;

    @Transactional
    public void creeazaSiInregistreazaPrezenta(Sedinta sedinta, Map<Long, StatusPrezenta> prezenteVoluntari) {
        if (sedinta.getDescriere() == null || sedinta.getDescriere().isBlank()) {
            throw new RuntimeException("Denumirea/descrierea ședinței este obligatorie.");
        }
        sedinta.setDataSedinta(LocalDateTime.now());
        sedintaRepository.save(sedinta); // Salvăm întâi ședința pentru a obține un ID

        List<PrezentaSedinta> listaPrezente = new ArrayList<>();

        for (Map.Entry<Long, StatusPrezenta> entry : prezenteVoluntari.entrySet()) {
            Long voluntarId = entry.getKey();
            StatusPrezenta status = entry.getValue();

            Voluntar voluntar = voluntarRepository.findById(voluntarId);
            if (voluntar != null) {
                // Creăm înregistrarea de prezență
                PrezentaSedinta prezenta = new PrezentaSedinta();
                prezenta.setSedinta(sedinta);
                prezenta.setVoluntar(voluntar);
                prezenta.setStatusPrezenta(status);
                prezentaSedintaRepository.save(prezenta);
                listaPrezente.add(prezenta);

                // Acordăm puncte dacă este prezent
                if (status == StatusPrezenta.PREZENT) {
                    double puncteNoi = (voluntar.getPuncte() == null ? 0 : voluntar.getPuncte()) + PUNCTE_PER_PREZENTA;
                    voluntar.setPuncte(puncteNoi);
                    voluntarRepository.update(voluntar);
                }
            }
        }
        sedinta.setPrezente(listaPrezente);
        sedintaRepository.update(sedinta);
    }

    public List<Sedinta> getToateSedintele() {
        return sedintaRepository.findAll();
    }
}