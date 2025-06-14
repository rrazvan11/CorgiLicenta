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

    @Lob
    private String descriere;

    // CÂMP NOU: Specifică tipul ședinței
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private TipSedinta tipSedinta;

    // O ședință trebuie să aparțină MEREU unei organizații
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizatie_id", nullable = false)
    private Organizatie organizatie;

    // O ședință poate să aparțină unui departament (când e ședință de departament)
    // sau NU (când e adunare generală). Deci, nullable = true
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departament_id", nullable = true) // <-- MODIFICARE CHEIE
    private Departament departament;

    @OneToMany(mappedBy = "sedinta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrezentaSedinta> prezente;
}