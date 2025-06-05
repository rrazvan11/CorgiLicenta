package ro.ong.corgi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import ro.ong.corgi.model.Enums.AnStudiu;
import ro.ong.corgi.model.Enums.Facultate;
import ro.ong.corgi.model.Enums.Status; // Asigură-te că importul este corect

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "voluntari")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voluntar {

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

    @Min(0)
    private Integer puncte;

    @Enumerated(EnumType.STRING) // E o practică bună să specifici STRING pentru enum
    private Status status;


    @ManyToOne
    @JoinColumn(name = "departament_id")
    private Departament departament;

    // 🔹 Taskuri atribuite (nu se șterg dacă se șterge voluntarul)
    @OneToMany(mappedBy = "voluntar")
    private List<Task> taskuri;

    // 🔹 Participările la proiecte (se șterg cu voluntarul)
    @OneToMany(mappedBy = "voluntar", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GrupareVoluntariProiecte> participari;

    @OneToOne(optional = false) // Un voluntar TREBUIE să aibă un cont de utilizator
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true)
    private User user;
}