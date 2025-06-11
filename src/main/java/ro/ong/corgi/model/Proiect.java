package ro.ong.corgi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "proiecte")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proiect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Numele proiectului este obligatoriu")
    private String numeProiect;

    @NotBlank(message = "Descrierea este obligatorie")
    private String descriere;

    @Min(value = 1, message = "Trebuie să existe cel puțin un voluntar necesar")
    private Integer necesarVoluntari;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizatie_id")
    private Organizatie organizatie;


    @OneToMany(mappedBy = "proiect")
    private List<Task> taskuri;


    @OneToMany(mappedBy = "proiect", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GrupareVoluntariProiecte> participari;
}
