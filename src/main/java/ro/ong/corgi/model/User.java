package ro.ong.corgi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import ro.ong.corgi.model.Enums.Rol;

import java.io.Serializable;

@Entity
@Table(name = "utilizatori")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"voluntar", "organizatie"}) // Excludem relațiile din toString
@EqualsAndHashCode(of = "id") // Folosim doar ID-ul pentru egalitate și hash
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

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
    @Size(min = 12, message = "Parola trebuie să aibă minim 10 caractere")
    private String parola;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Builder.Default
    private boolean activ = true;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Voluntar voluntar;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Organizatie organizatie;
}
