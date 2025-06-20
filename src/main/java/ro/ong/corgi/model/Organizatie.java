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
@ToString(exclude = {"proiecte", "voluntari", "user"})
@EqualsAndHashCode(of = "cif")
public class Organizatie implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @NotNull(message = "CIF-ul este obligatoriu")
    @Column(nullable = false, unique = true)
    private Long cif;

    @NotBlank(message = "Numele organizației este obligatoriu")
    @Column(unique = true, nullable = false)
    private String nume;

    @NotBlank(message = "Adresa este obligatorie")
    private String adresa;

    @Email(message = "Emailul trebuie să fie valid")
    private String mail;

    @OneToMany(mappedBy = "organizatie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Proiect> proiecte = new java.util.ArrayList<>();

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true)
    private User user;

    @OneToMany(mappedBy = "organizatie")
    private List<Voluntar> voluntari = new java.util.ArrayList<>();
}
