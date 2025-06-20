package ro.ong.corgi.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.ong.corgi.dto.SedintaDTO;
import ro.ong.corgi.model.Enums.StatusPrezenta;
import ro.ong.corgi.model.Enums.TipSedinta;
import ro.ong.corgi.model.PrezentaSedinta;
import ro.ong.corgi.model.Sedinta;
import ro.ong.corgi.model.Voluntar;
import ro.ong.corgi.repository.PrezentaSedintaRepository;
import ro.ong.corgi.repository.SedintaRepository;
import ro.ong.corgi.repository.VoluntarRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


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
        sedintaRepository.save(sedinta);

        for (Map.Entry<Long, StatusPrezenta> entry : prezenteVoluntari.entrySet()) {
            Long voluntarId = entry.getKey();
            StatusPrezenta status = entry.getValue();

            Voluntar voluntar = voluntarRepository.findById(voluntarId);

            if (voluntar != null) {
                PrezentaSedinta prezenta = new PrezentaSedinta();
                prezenta.setSedinta(sedinta);
                prezenta.setVoluntar(voluntar);
                prezenta.setStatusPrezenta(status);

                prezentaSedintaRepository.save(prezenta);

                if (status == StatusPrezenta.PREZENT || status == StatusPrezenta.ONLINE) {
                    double puncteNoi = (voluntar.getPuncte() == null ? 0 : voluntar.getPuncte()) + PUNCTE_PER_PREZENTA;
                    voluntar.setPuncte(puncteNoi);
                }
            }
        }
    }

    // METODA getToateSedintele A FOST ȘTEARSĂ

    @Transactional
    public void actualizeazaPrezenta(Long sedintaId, Map<Long, StatusPrezenta> prezenteNoi) {
        List<PrezentaSedinta> prezenteVechi = prezentaSedintaRepository.findBySedintaId(sedintaId);

        for (PrezentaSedinta prezentaExistenta : prezenteVechi) {
            Voluntar voluntar = prezentaExistenta.getVoluntar();
            StatusPrezenta statusVechi = prezentaExistenta.getStatusPrezenta();
            StatusPrezenta statusNou = prezenteNoi.get(voluntar.getId());

            if (statusNou != null && statusVechi != statusNou) {
                prezentaExistenta.setStatusPrezenta(statusNou);

                boolean eraPrezentInainte = (statusVechi == StatusPrezenta.PREZENT || statusVechi == StatusPrezenta.ONLINE);
                boolean estePrezentAcum = (statusNou == StatusPrezenta.PREZENT || statusNou == StatusPrezenta.ONLINE);

                if (estePrezentAcum && !eraPrezentInainte) {
                    voluntar.setPuncte(voluntar.getPuncte() + PUNCTE_PER_PREZENTA);
                } else if (!estePrezentAcum && eraPrezentInainte) {
                    voluntar.setPuncte(voluntar.getPuncte() - PUNCTE_PER_PREZENTA);
                }
            }
        }
    }

    public List<PrezentaSedinta> getPrezentePentruSedinta(Long sedintaId) {
        if (sedintaRepository.findById(sedintaId) == null) {
            throw new RuntimeException("Ședința cu ID " + sedintaId + " nu există.");
        }
        return prezentaSedintaRepository.findBySedintaId(sedintaId);
    }

    @Transactional
    public void stergeSedintaSiPrezentele(Long sedintaId) {
        Sedinta sedintaDeSters = sedintaRepository.findById(sedintaId);
        if (sedintaDeSters == null) {
            throw new RuntimeException("Ședința cu ID " + sedintaId + " nu a fost găsită, nu poate fi ștearsă.");
        }

        List<PrezentaSedinta> prezenteDeAnulat = prezentaSedintaRepository.findBySedintaId(sedintaId);

        for (PrezentaSedinta prezenta : prezenteDeAnulat) {
            if (prezenta.getStatusPrezenta() == StatusPrezenta.PREZENT || prezenta.getStatusPrezenta() == StatusPrezenta.ONLINE) {
                Voluntar voluntar = prezenta.getVoluntar();
                double puncteActuale = voluntar.getPuncte() != null ? voluntar.getPuncte() : 0.0;
                voluntar.setPuncte(puncteActuale - PUNCTE_PER_PREZENTA);
            }
        }

        sedintaRepository.delete(sedintaDeSters);
    }

    public List<SedintaDTO> getSedinteInfoPentruDepartament(Long departamentId) {
        long totalVoluntariInDepartament = voluntarRepository.findByField("departament.id", departamentId).size();
        List<Sedinta> sedinte = sedintaRepository.findByField("departament.id", departamentId);

        return sedinte.stream().map(sedinta -> {
                    long numarPrezentiSiOnline = prezentaSedintaRepository.adunăPrezentSedințaAndStatusIn(
                            sedinta.getId(), List.of(StatusPrezenta.PREZENT, StatusPrezenta.ONLINE));
                    return new SedintaDTO(sedinta, numarPrezentiSiOnline, totalVoluntariInDepartament);
                })
                .sorted(Comparator.comparing(dto -> dto.getSedinta().getDataSedinta(), Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    public List<SedintaDTO> getSedinteAdunareGeneralaPentruOrganizatie(Long organizatieCif) { // Am redenumit parametrul
        List<Sedinta> sedinte = sedintaRepository.findByOrganizatieIdAndTip(organizatieCif, TipSedinta.ADUNARE_GENERALĂ);
        long totalVoluntari = voluntarRepository.countByOrganizatieId(organizatieCif);

        return sedinte.stream().map(sedinta -> {
            long numarPrezentiSiOnline = prezentaSedintaRepository.adunăPrezentSedințaAndStatusIn(
                    sedinta.getId(), List.of(StatusPrezenta.PREZENT, StatusPrezenta.ONLINE));
            return new SedintaDTO(sedinta, numarPrezentiSiOnline, totalVoluntari);
        }).collect(Collectors.toList());
    }
}