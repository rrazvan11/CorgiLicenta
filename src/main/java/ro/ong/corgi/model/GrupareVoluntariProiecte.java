package ro.ong.corgi.model;

import jakarta.persistence.*;
import lombok.*;
import ro.ong.corgi.model.Enums.StatusAplicari;

import java.time.LocalDateTime;

@Entity
@Table(name = "grupare_voluntari_proiecte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"voluntar", "proiect"})
@EqualsAndHashCode(of = "id")
public class GrupareVoluntariProiecte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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