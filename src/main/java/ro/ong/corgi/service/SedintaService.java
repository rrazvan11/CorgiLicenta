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
import java.util.ArrayList;
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
        sedintaRepository.save(sedinta); // Pas 1: Salvăm ședința pentru a obține un ID. Corect.

        // Pas 2: Iterăm prin prezențe și le salvăm.
        for (Map.Entry<Long, StatusPrezenta> entry : prezenteVoluntari.entrySet()) {
            Long voluntarId = entry.getKey();
            StatusPrezenta status = entry.getValue();

            // Căutăm voluntarul în contextul persistenței curente
            Voluntar voluntar = voluntarRepository.findById(voluntarId);

            if (voluntar != null) {
                // Creăm înregistrarea de prezență
                PrezentaSedinta prezenta = new PrezentaSedinta();
                prezenta.setSedinta(sedinta); // Legăm de ședința proaspăt salvată
                prezenta.setVoluntar(voluntar); // Legăm de voluntarul găsit
                prezenta.setStatusPrezenta(status);

                // Salvăm înregistrarea de prezență. Aici se setează cheia externă.
                prezentaSedintaRepository.save(prezenta);

                // Acordăm puncte dacă este prezent sau online
                if (status == StatusPrezenta.PREZENT || status == StatusPrezenta.ONLINE) {
                    double puncteNoi = (voluntar.getPuncte() == null ? 0 : voluntar.getPuncte()) + PUNCTE_PER_PREZENTA;
                    voluntar.setPuncte(puncteNoi);
                    // NU mai este nevoie de .update(voluntar).
                    // Fiind într-o metodă @Transactional, Hibernate va salva automat modificarea.
                }
            }
        }
    }

    public List<Sedinta> getToateSedintele() {
        return sedintaRepository.findAll();
    }


    @Transactional
    public void actualizeazaPrezenta(Long sedintaId, Map<Long, StatusPrezenta> prezenteNoi) {
        // 1. Preluăm toate înregistrările de prezență vechi pentru această ședință
        List<PrezentaSedinta> prezenteVechi = prezentaSedintaRepository.findBySedintaId(sedintaId);

        // 2. Iterăm prin fiecare prezență veche și vedem dacă s-a schimbat statusul
        for (PrezentaSedinta prezentaExistenta : prezenteVechi) {
            Voluntar voluntar = prezentaExistenta.getVoluntar();
            StatusPrezenta statusVechi = prezentaExistenta.getStatusPrezenta();
            StatusPrezenta statusNou = prezenteNoi.get(voluntar.getId());

            if (statusNou != null && statusVechi != statusNou) {
                // Statusul s-a schimbat, deci actualizăm
                prezentaExistenta.setStatusPrezenta(statusNou);

                // 3. Logica de ajustare a punctelor
                boolean eraPrezentInainte = (statusVechi == StatusPrezenta.PREZENT || statusVechi == StatusPrezenta.ONLINE);
                boolean estePrezentAcum = (statusNou == StatusPrezenta.PREZENT || statusNou == StatusPrezenta.ONLINE);

                if (estePrezentAcum && !eraPrezentInainte) {
                    // A devenit prezent, deci adăugăm puncte
                    voluntar.setPuncte(voluntar.getPuncte() + PUNCTE_PER_PREZENTA);
                } else if (!estePrezentAcum && eraPrezentInainte) {
                    // Nu mai este prezent, deci scădem punctele
                    voluntar.setPuncte(voluntar.getPuncte() - PUNCTE_PER_PREZENTA);
                }
                // Dacă statusul a rămas la fel (ex: PREZENT -> PREZENT) sau nu implică puncte, nu facem nimic.
            }
        }
        // La finalul metodei, Hibernate va salva automat toate modificările pe entitățile
        // 'prezentaExistenta' și 'voluntar' deoarece metoda este @Transactional.
    }
    // Adaugă această metodă nouă în SedintaService.java
    public List<PrezentaSedinta> getPrezentePentruSedinta(Long sedintaId) {
        // Verificare pentru a ne asigura că ședința există, înainte de a căuta prezențe
        if (sedintaRepository.findById(sedintaId) == null) {
            throw new RuntimeException("Ședința cu ID " + sedintaId + " nu există.");
        }
        return prezentaSedintaRepository.findBySedintaId(sedintaId);
    }

    @Transactional
    public void stergeSedintaSiPrezentele(Long sedintaId) {
        // 1. Găsim ședința pe care vrem să o ștergem
        Sedinta sedintaDeSters = sedintaRepository.findById(sedintaId);
        if (sedintaDeSters == null) {
            throw new RuntimeException("Ședința cu ID " + sedintaId + " nu a fost găsită, nu poate fi ștearsă.");
        }

        // 2. Anulăm punctele acordate pentru această ședință
        // Folosim metoda existentă pentru a găsi toate prezențele asociate
        List<PrezentaSedinta> prezenteDeAnulat = prezentaSedintaRepository.findBySedintaId(sedintaId);

        for (PrezentaSedinta prezenta : prezenteDeAnulat) {
            if (prezenta.getStatusPrezenta() == StatusPrezenta.PREZENT || prezenta.getStatusPrezenta() == StatusPrezenta.ONLINE) {
                Voluntar voluntar = prezenta.getVoluntar();
                double puncteActuale = voluntar.getPuncte() != null ? voluntar.getPuncte() : 0.0;
                voluntar.setPuncte(puncteActuale - PUNCTE_PER_PREZENTA);
            }
        }

        // 3. Ștergem ședința. Datorită `cascade = CascadeType.ALL` pe entitatea Sedinta,
        // toate înregistrările de PrezentaSedinta asociate vor fi șterse automat.
        sedintaRepository.delete(sedintaDeSters);
    }

    public List<SedintaDTO> getSedinteInfoPentruDepartament(Long departamentId) {
        // Aflăm câți voluntari sunt în total în acest departament
        long totalVoluntariInDepartament = voluntarRepository.findByField("departament.id", departamentId).size();

        // Găsim toate ședințele care aparțin de acest departament
        List<Sedinta> sedinte = sedintaRepository.findByField("departament.id", departamentId);

        // Transformăm fiecare ședință într-un DTO cu informațiile necesare
        return sedinte.stream().map(sedinta -> {
                    long numarPrezentiSiOnline = prezentaSedintaRepository.adunăPrezentSedințaAndStatusIn(
                            sedinta.getId(), List.of(StatusPrezenta.PREZENT, StatusPrezenta.ONLINE));
                    return new SedintaDTO(sedinta, numarPrezentiSiOnline, totalVoluntariInDepartament);
                })
                .sorted(Comparator.comparing(dto -> dto.getSedinta().getDataSedinta(), Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }
    public List<SedintaDTO> getSedinteAdunareGeneralaPentruOrganizatie(Long organizatieId) {
        // Apelăm noua metodă din repository care filtrează după tip
        List<Sedinta> sedinte = sedintaRepository.findByOrganizatieIdAndTip(organizatieId, TipSedinta.ADUNARE_GENERALĂ);

        // Logica de calculare a prezenților rămâne aceeași
        long totalVoluntari = voluntarRepository.countByOrganizatieId(organizatieId);

        return sedinte.stream().map(sedinta -> {
            long numarPrezentiSiOnline = prezentaSedintaRepository.adunăPrezentSedințaAndStatusIn(
                    sedinta.getId(), List.of(StatusPrezenta.PREZENT, StatusPrezenta.ONLINE));
            return new SedintaDTO(sedinta, numarPrezentiSiOnline, totalVoluntari);
        }).collect(Collectors.toList());
    }
}