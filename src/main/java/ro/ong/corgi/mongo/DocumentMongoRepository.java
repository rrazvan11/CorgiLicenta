package ro.ong.corgi.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.types.ObjectId;
import ro.ong.corgi.model.Enums.tipDocument;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DocumentMongoRepository {

    private final MongoCollection<org.bson.Document> collection;

    public DocumentMongoRepository(MongoCollection<org.bson.Document> collection) {
        this.collection = collection;
    }

    public void save(DocumentMongo doc) {
        org.bson.Document b = new org.bson.Document()
                .append("_id", doc.getId() != null ? new ObjectId(doc.getId()) : new ObjectId())
                .append("tip", doc.getTip().name())
                .append("numarDocument", doc.getNumarDocument())
                .append("dataIntocmire", doc.getDataIntocmire().toString())
                .append("valoareTotala", doc.getValoareTotala())
                .append("pathFisier", doc.getPathFisier())
                .append("voluntarId", doc.getVoluntarId())
                .append("organizatieId", doc.getOrganizatieId())
                .append("descriere", doc.getDescriere());
        collection.insertOne(b);
        doc.setId(b.getObjectId("_id").toHexString());
    }

    public Optional<DocumentMongo> findById(String id) {
        org.bson.Document b = collection.find(Filters.eq("_id", new ObjectId(id))).first();
        return b == null ? Optional.empty() : Optional.of(fromBson(b));
    }

    public boolean existsById(String id) {
        return collection.countDocuments(Filters.eq("_id", new ObjectId(id))) > 0;
    }

    public Optional<DocumentMongo> findByNumarDocument(String numar) {
        org.bson.Document b = collection.find(Filters.eq("numarDocument", numar)).first();
        return b == null ? Optional.empty() : Optional.of(fromBson(b));
    }

    public List<DocumentMongo> findAll() {
        return collection.find()
                .map(this::fromBson)
                .into(new java.util.ArrayList<>());
    }

    public List<DocumentMongo> findByTip(tipDocument tip) {
        return collection.find(Filters.eq("tip", tip.name()))
                .map(this::fromBson)
                .into(new java.util.ArrayList<>());
    }

    public List<DocumentMongo> findByTipAndValoareTotalaGreaterThan(tipDocument tip, double valoare) {
        return collection.find(Filters.and(
                        Filters.eq("tip", tip.name()),
                        Filters.gt("valoareTotala", valoare)))
                .map(this::fromBson)
                .into(new java.util.ArrayList<>());
    }

    public void deleteById(String id) {
        collection.deleteOne(Filters.eq("_id", new ObjectId(id)));
    }

    private DocumentMongo fromBson(org.bson.Document b) {
        return DocumentMongo.builder()
                .id(b.getObjectId("_id").toHexString())
                .tip(tipDocument.valueOf(b.getString("tip")))
                .numarDocument(b.getString("numarDocument"))
                .dataIntocmire(LocalDate.parse(b.getString("dataIntocmire")))
                .valoareTotala(b.getDouble("valoareTotala"))
                .pathFisier(b.getString("pathFisier"))
                .voluntarId(b.getString("voluntarId"))
                .organizatieId(b.getString("organizatieId"))
                .descriere(b.getString("descriere"))
                .build();
    }
}
