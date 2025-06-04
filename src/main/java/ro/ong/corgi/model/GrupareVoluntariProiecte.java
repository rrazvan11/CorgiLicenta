package ro.ong.corgi.model;

import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne
    @JoinColumn(name = "voluntar_id")
    private Voluntar voluntar;

    @ManyToOne
    @JoinColumn(name = "proiect_id")
    private Proiect proiect;
}
