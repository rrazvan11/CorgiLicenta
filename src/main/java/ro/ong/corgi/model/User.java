package ro.ong.corgi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import ro.ong.corgi.model.Enums.Rol; // Asigură-te că importul este corect

@Entity
@Table(name = "utilizatori") // Am redenumit tabela la "utilizatori" (plural), cum e și în persistence.xml
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Email(message = "Emailul trebuie să fie valid")
    @NotBlank(message = "Emailul este obligatoriu")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Username-ul este obligatoriu")
    @Column(unique = true, nullable = false)
    private String username;

    @NotBlank(message = "Parola este obligatorie")
    @Size(min = 6, message = "Parola trebuie să aibă minim 6 caractere")
    private String parola; // Parola rămâne aici

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Builder.Default
    private boolean activ = true;

    // Relație opțională: userul POATE fi legat de un voluntar
    // Dacă un User ESTE un Voluntar, atunci `optional = false` ar fi pe partea Voluntarului
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Voluntar voluntar;

    // Relație opțională: userul POATE fi legat de o organizație (contul principal al organizației)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Organizatie organizatie;

}