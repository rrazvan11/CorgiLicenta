package ro.ong.corgi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import ro.ong.corgi.model.Enums.StatusProiect; // Importăm denumirea corectă

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "proiecte")
// --- START MODIFICĂRI ---
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"organizatie", "taskuri", "participari", "coordonatorProiect"}) // Excludem relațiile
@EqualsAndHashCode(of = "id") // Important: egalitatea se bazează doar pe ID
public class Proiect {
// --- FINAL MODIFICĂRI (am înlocuit @Data) ---

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ... restul clasei rămâne la fel
    @NotBlank(message = "Numele proiectului este obligatoriu")
    private String numeProiect;

    @NotBlank(message = "Descrierea este obligatorie")
    private String descriere;

    @Min(value = 1, message = "Trebuie să existe cel puțin un voluntar necesar")
    private Integer necesarVoluntari;

    private LocalDate dataInceput;

    private LocalDate dataSfarsit;

    @Enumerated(EnumType.STRING)
    private StatusProiect status; // Folosim denumirea corectă a enum-ului

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizatie_id")
    private Organizatie organizatie;


    @OneToMany(mappedBy = "proiect")
    private List<Task> taskuri = new java.util.ArrayList<>();


    @OneToMany(mappedBy = "proiect", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GrupareVoluntariProiecte> participari = new java.util.ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordonator_id")
    private Voluntar coordonatorProiect;
}