package ro.ong.corgi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
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

    // Emailul este preluat din User sau poate fi un email de contact separat?
    // Momentan, îl las aici, dar poate fi redundant dacă User.email e cel principal.
    // Dacă User.email e cel principal, acest câmp poate fi eliminat sau redenumit.
    @Email(message = "Emailul trebuie să fie valid")
    @NotBlank(message = "Emailul este obligatoriu")
    private String email; // Acest email s-ar putea să fie redundant dacă User.email e cel folosit pentru login

    @Pattern(regexp = "^\\d{10}$", message = "Telefonul trebuie să conțină exact 10 cifre")
    private String telefon;

    private String facultate;
    private String specializare;

    @Min(value = 1, message = "Anul de studiu trebuie să fie cel puțin 1")
    @Max(value = 5, message = "Anul de studiu nu poate fi mai mare de 5")
    private Integer anStudiu;

    private LocalDate dataInrolare;

    @Min(0)
    private Integer oreDeVoluntariat;

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