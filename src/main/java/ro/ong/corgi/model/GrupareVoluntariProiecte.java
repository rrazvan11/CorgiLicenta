package ro.ong.corgi.model;

import jakarta.persistence.*;
import lombok.*;
import ro.ong.corgi.model.Enums.StatusAplicari;

import java.time.LocalDateTime;

@Entity
@Table(name = "grupare_voluntari_proiecte")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrupareVoluntariProiecte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // AICI ESTE MODIFICAREA CHEIE: FetchType.EAGER
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "voluntar_id")
    private Voluntar voluntar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proiect_id")
    private Proiect proiect;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_aplicatie")
    private StatusAplicari statusAplicatie;

    @Column(name = "data_aplicatie")
    private LocalDateTime dataAplicatie;
}