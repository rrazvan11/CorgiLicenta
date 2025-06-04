package ro.ong.corgi.mongo;

import ro.ong.corgi.mongo.DocumentMongo;
import ro.ong.corgi.mongo.DocumentMongoService;

import java.time.LocalDate;

/*public class MongoMain {
    public static void main(String[] args) {

        DocumentMongo contract = DocumentMongo.builder()
                .tip("contract_voluntariat")
                .numarDocument("CV-2025-001")
                .dataIntocmire(LocalDate.now())
                .voluntarId("123")  // ID-ul poate fi string (ex: din JPA sau UUID)
                .pathFisier("C:/CorgiDocumente/contracte")
                .descriere("Contract de voluntariat pentru proiectul EduHack")
                .build();

        DocumentMongoService service = new DocumentMongoService();
        boolean rezultat = service.save(contract);

        if (rezultat) {
            System.out.println("✅ Contractul a fost salvat în MongoDB.");
        } else {
            System.out.println("❌ Salvarea contractului a eșuat.");
        }
    }
}
*/