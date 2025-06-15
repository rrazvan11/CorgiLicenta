package ro.ong.corgi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ro.ong.corgi.model.Enums.TipSedinta; // Importăm noul Enum

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "sedinte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"departament", "organizatie", "prezente"})
@EqualsAndHashCode(of = "id")
public class Sedinta implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dataSedinta;

    private String descriere;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private TipSedinta tipSedinta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizatie_id", nullable = false)
    private Organizatie organizatie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departament_id", nullable = true)
    private Departament departament;

    @OneToMany(mappedBy = "sedinta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrezentaSedinta> prezente;
}