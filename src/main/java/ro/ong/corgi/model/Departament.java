package ro.ong.corgi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "departamente",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"nume", "organizatie_id"}, name = "UK_departament_nume_organizatie")
        }
)
@Data
@ToString(exclude = {"voluntari", "coordonator", "organizatie"})
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Departament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Numele departamentului este obligatoriu")
    @Size(max = 100, message = "Numele departamentului nu poate avea mai mult de 100 de caractere")
    @Column(nullable = false) // Am eliminat unique = true de aici
    private String nume;

    @Size(max = 255, message = "Descrierea nu poate depăși 255 de caractere")
    private String descriere;

    @OneToOne
    @JoinColumn(name = "coordonator_id")
    private Voluntar coordonator;

    @OneToMany(mappedBy = "departament")
    private List<Voluntar> voluntari = new java.util.ArrayList<>();

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "organizatie_id", nullable = false)
    private Organizatie organizatie;
}