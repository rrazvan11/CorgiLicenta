package ro.ong.corgi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ro.ong.corgi.model.Enums.StatusPrezenta; // Vom crea acest enum

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "prezente_sedinte")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrezentaSedinta implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "voluntar_id", nullable = false)
    private Voluntar voluntar;


    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sedinta_id", nullable = false)
    private Sedinta sedinta;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPrezenta statusPrezenta;

    @Builder.Default
    private LocalDateTime dataInregistrare = LocalDateTime.now();
}