package ro.ong.corgi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import ro.ong.corgi.model.Enums.AnStudiu;
import ro.ong.corgi.model.Enums.Facultate;
import ro.ong.corgi.model.Enums.Status;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "voluntari")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "departament", "taskuri", "participari", "organizatie"}) // Adaugat organizatie
@EqualsAndHashCode(of = "id")
public class Voluntar implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Numele este obligatoriu")
    private String nume;

    @NotBlank(message = "Prenumele este obligatoriu")
    private String prenume;

    @Pattern(regexp = "^\\d{10}$", message = "Telefonul trebuie să conțină exact 10 cifre")
    private String telefon;

    @Enumerated(EnumType.STRING)
    private Facultate facultate;
    private String specializare;

    @Enumerated(EnumType.STRING)
    private AnStudiu anStudiu;

    private LocalDate dataInrolare;

    private Double puncte;

    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departament_id")
    private Departament departament;

    @OneToMany(mappedBy = "voluntar")
    private List<Task> taskuri = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "voluntar", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GrupareVoluntariProiecte> participari = new java.util.ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY, optional = false) // MODIFICAT
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // MODIFICAT
    @JoinColumn(name = "organizatie_id", nullable = false)
    private Organizatie organizatie;

    @Transient
    public String getNumeComplet() {
        return nume + " " + prenume;
    }
}