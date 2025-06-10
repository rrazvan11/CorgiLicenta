package ro.ong.corgi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "organizatii")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "proiecte"})
@EqualsAndHashCode(of = "id")
public class Organizatie implements Serializable {

    private static final long serialVersionUID = 1L;

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

    @Email(message = "Emailul trebuie să fie valid")
    private String mail;

    @OneToMany(mappedBy = "organizatie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Proiect> proiecte;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true)
    private User user;
}
