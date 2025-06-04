package ro.ong.corgi.mongo;

import ro.ong.corgi.model.Enums.tipDocument;
import ro.ong.corgi.model.Enums.tipDocument;

import java.util.List;

public class DocumentMongoService {

    private final DocumentMongoRepository repo;

    public DocumentMongoService(DocumentMongoRepository repo) {
        this.repo = repo;
    }

    public void save(DocumentMongo doc) {
        // ⭕️ tip already a TipDocument enum
        if (doc.getTip() == null) {
            throw new RuntimeException("Trebuie să alegi un tip de document.");
        }

        // ⭕️ numarDocument non-blank
        if (doc.getNumarDocument() == null || doc.getNumarDocument().isBlank()) {
            throw new RuntimeException("Numărul documentului nu poate fi gol.");
        }

        // ⭕️ unique numarDocument
        if (repo.findByNumarDocument(doc.getNumarDocument()).isPresent()) {
            throw new RuntimeException("Document cu numărul „"
                    + doc.getNumarDocument() + "” există deja.");
        }

        repo.save(doc);
    }

    public DocumentMongo findById(String id) {
        return repo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Document inexistent: " + id));
    }

    public void deleteById(String id) {
        if (!repo.existsById(id)) {
            throw new RuntimeException("Nu există document cu id = " + id);
        }
        repo.deleteById(id);
    }

    public List<DocumentMongo> findAll() {
        return repo.findAll();
    }

    public List<DocumentMongo> findByTip(tipDocument tip) {
        return repo.findByTip(tip);
    }

    public List<DocumentMongo> findBonuriCuValoareMinima(double minValoare) {
        return repo.findByTipAndValoareTotalaGreaterThan(tipDocument.BON, minValoare);
    }
}
