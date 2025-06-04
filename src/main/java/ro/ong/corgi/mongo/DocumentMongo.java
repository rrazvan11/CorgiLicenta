package ro.ong.corgi.mongo;

import lombok.*;
import ro.ong.corgi.model.Enums.tipDocument;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentMongo {

    private String id;               // MongoDB ObjectId

    private tipDocument tip;             // "contract_voluntariat", "factura", "bon"
    private String numarDocument;   // ex: "CV-2024-013", "FCT-0045"

    private LocalDate dataIntocmire;

    private Double valoareTotala;   // doar pt. bonuri / facturi

    private String pathFisier;      // unde e salvat PDF-ul

    private String voluntarId;      // doar pentru contractele voluntarilor
    private String organizatieId;   // pentru facturi/bonuri

    private String descriere;       // opțional (scop, furnizor etc.)
}
