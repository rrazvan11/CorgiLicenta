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

    // Calculează 50% + 1
    public boolean MajoritateSimpla() {
        if (numarTotalVoluntari == 0) return false;
        return (2 * numarPrezentiSiOnline) > numarTotalVoluntari;
    }

    // Calculează 75% + 1
    public boolean MajoritateExtraordinara() {
        if (numarTotalVoluntari == 0) return false;
        return (4 * numarPrezentiSiOnline) > (3 * numarTotalVoluntari);
    }
}