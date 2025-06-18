package ro.ong.corgi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import ro.ong.corgi.model.Enums.TaskStatus;

import java.time.LocalDate;

@Entity
@Table(name = "taskuri")
// --- START MODIFICĂRI ---
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"voluntar", "proiect", "skillDobandit"})
@EqualsAndHashCode(of = "id")
public class Task {
// --- FINAL MODIFICĂRI (am înlocuit @Data) ---

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ... restul clasei rămâne la fel
    @NotBlank(message = "Titlul taskului este obligatoriu")
    private String titlu;

    @NotBlank(message = "Descrierea este obligatorie")
    private String descriere;

    @FutureOrPresent(message = "Deadlineul trebuie să fie în viitor sau azi")
    private LocalDate deadline;

    @Enumerated(EnumType.STRING) // Adaugă această linie dacă TaskStatus este un enum
    @NotNull(message = "Statusul este obligatoriu") // Am schimbat din @NotBlank în @NotNull pentru Enum
    private TaskStatus status; // exemplu: "IN_PROGRESS", "FINALIZAT", "PENDING"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voluntar_id")
    private Voluntar voluntar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proiect_id")
    private Proiect proiect;

    @Min(value = 0, message = "Punctele pentru task nu pot fi negative")
    @Column(name = "puncte_task")
    private Double puncteTask;

}