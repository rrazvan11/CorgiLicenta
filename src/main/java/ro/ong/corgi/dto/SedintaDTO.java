package ro.ong.corgi.dto;

import lombok.Getter;
import lombok.Setter;
import ro.ong.corgi.model.Sedinta;

@Getter
@Setter
public class SedintaDTO {

    private Sedinta sedinta;
    private long numarPrezentiSiOnline;
    private long numarTotalVoluntari;

    public SedintaDTO(Sedinta sedinta, long numarPrezentiSiOnline, long numarTotalVoluntari) {
        this.sedinta = sedinta;
        this.numarPrezentiSiOnline = numarPrezentiSiOnline;
        this.numarTotalVoluntari = numarTotalVoluntari;
    }

    /**
     * Calculează și returnează numărul de voturi necesar pentru 50% + 1.
     * @return int - numărul de voturi
     */
    public int getMajoritateSimpla() {
        if (numarPrezentiSiOnline == 0) return 0;
        return (int) (numarPrezentiSiOnline / 2) + 1;
    }

    /**
     * Calculează și returnează numărul de voturi necesar pentru 65% + 1.
     * @return int - numărul de voturi
     */
    public int getMajoritateCalificata() {
        if (numarPrezentiSiOnline == 0) return 0;
        // Folosim Math.floor pentru a trunchia zecimalele înainte de a aduna 1
        return (int) Math.floor(numarPrezentiSiOnline * 0.65) + 1;
    }

    /**
     * Calculează și returnează numărul de voturi necesar pentru 75% + 1.
     * @return int - numărul de voturi
     */
    public int getMajoritateExtraordinara() {
        if (numarPrezentiSiOnline == 0) return 0;
        return (int) Math.floor(numarPrezentiSiOnline * 0.75) + 1;
    }
}