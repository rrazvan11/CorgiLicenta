package ro.ong.corgi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "organizatii")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organizatie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Numele organizației este obligatoriu")
    @Column(unique = true, nullable = false)
    private String nume;

    @NotBlank(message = "Adresa este obligatorie")
    private String adresa;

    @NotNull(message = "CIF-ul este obligatoriu")
    @Column(unique = true)
    private Long cif;

    // Parola a fost eliminată

    @Email(message = "Emailul trebuie să fie valid")
    private String mail; // Acesta poate fi emailul public de contact al organizației

    @OneToMany(mappedBy = "organizatie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Proiect> proiecte;

    @OneToOne(optional = false) // O organizație TREBUIE să aibă un cont de utilizator principal
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true)
    private User user;
}