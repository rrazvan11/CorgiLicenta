package ro.ong.corgi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import ro.ong.corgi.model.Enums.TaskStatus;

import java.time.LocalDate;

@Entity
@Table(name = "taskuri")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Titlul taskului este obligatoriu")
    private String titlu;

    @NotBlank(message = "Descrierea este obligatorie")
    private String descriere;

    @FutureOrPresent(message = "Deadlineul trebuie să fie în viitor sau azi")
    private LocalDate deadline;

    @Enumerated(EnumType.STRING) // Adaugă această linie dacă TaskStatus este un enum
    @NotNull(message = "Statusul este obligatoriu") // Am schimbat din @NotBlank în @NotNull pentru Enum
    private TaskStatus status; // exemplu: "IN_PROGRESS", "FINALIZAT", "PENDING"

    @ManyToOne
    @JoinColumn(name = "voluntar_id")
    private Voluntar voluntar;

    @ManyToOne
    @JoinColumn(name = "proiect_id")
    private Proiect proiect;

    @Min(value = 0, message = "Punctele pentru task nu pot fi negative")
    @Column(name = "puncte_task")
    private Integer puncteTask;

    @ManyToOne
    @JoinColumn(name = "skill_id")
    private Skill skillDobandit;
}
