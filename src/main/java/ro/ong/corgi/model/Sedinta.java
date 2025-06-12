package ro.ong.corgi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "sedinte")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sedinta implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dataSedinta;

    @Lob // Pentru texte mai lungi, cum ar fi o descriere detaliată sau o minută
    private String descriere;


    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "departament_id", nullable = false)
    private Departament departament;

    @OneToMany(mappedBy = "sedinta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrezentaSedinta> prezente;
}