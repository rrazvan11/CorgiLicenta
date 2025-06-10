package ro.ong.corgi.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import ro.ong.corgi.model.Organizatie;
import ro.ong.corgi.model.User;
import ro.ong.corgi.model.Voluntar;

import java.io.ByteArrayOutputStream;
import java.io.IOException; // Importăm IOException pentru a-l putea prinde
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@ApplicationScoped
public class DocumentGenerationService {
    private static final String DEFAULT_NUME_ORGANIZATIE = "Asociația Studenților Corgi";
    private static final String TEMPLATE_FOLDER = "templates/";
    private static final String DEFAULT_REPREZENTANT = "Reprezentant Nespecificat";
    private static final String DEFAULT_FUNCTIE = "Reprezentant";

    public byte[] genereazaCertificatPdf(Voluntar voluntar, Organizatie organizatieEmitenta) {
        if (voluntar == null) {
            throw new IllegalArgumentException("Datele voluntarului sunt necesare pentru a genera certificatul.");
        }

        Map<String, String> data = pregatesteDateCertificat(voluntar, organizatieEmitenta);
        String htmlContent = loadAndPopulateTemplate("template_certificat.html", data);
        return genereazaPdfDinHtml(htmlContent);
    }

    private String loadAndPopulateTemplate(String numeTemplate, Map<String, String> data) {
        String path = "templates/" + numeTemplate;

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path)) {

            if (inputStream == null) {
                System.err.println("EROARE: Template-ul " + path + " nu a fost găsit în classpath!");
                throw new RuntimeException("Template-ul " + path + " lipsește.");
            }

            // Citim direct toți octeții din fișier și îi convertim într-un String folosind UTF-8
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            // Înlocuim placeholder-ele
            for (Map.Entry<String, String> entry : data.entrySet()) {
                String value = (entry.getValue() != null) ? entry.getValue() : "";
                content = content.replace("${" + entry.getKey() + "}", value);
            }

            return content;

        } catch (Exception e) {
            System.err.println("Eroare la încărcarea template-ului " + path + ": " + e.getMessage());
            throw new RuntimeException("Eroare la încărcarea template-ului.", e);
        }
    }
    private byte[] genereazaPdfDinHtml(String htmlContent) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, null);
            builder.toStream(outputStream);

            // Încărcăm fontul pentru diacritice
            URL fontUrl = getClass().getClassLoader().getResource("fonts/DejaVuSans.ttf");
            if (fontUrl != null) {
                builder.useFont(() -> {
                    try {
                        return fontUrl.openStream();
                    } catch (IOException e) {
                        // "Împachetăm" excepția checked într-una unchecked
                        throw new RuntimeException("Nu s-a putut deschide stream-ul pentru font.", e);
                    }
                }, "DejaVu Sans"); // Numele familiei de font pentru CSS

                System.out.println("INFO: Fontul DejaVu Sans a fost setat pentru a fi folosit în PDF.");
            } else {
                System.err.println("AVERTISMENT: Fontul pentru diacritice (fonts/DejaVuSans.ttf) nu a fost găsit în 'resources'.");
            }

            builder.run(); // Generează PDF-ul
            return outputStream.toByteArray();

        } catch (Exception e) {
            e.printStackTrace(); // Afișăm eroarea completă în consolă pentru a o vedea
            throw new RuntimeException("Nu s-a putut crea fișierul PDF.", e);
        }
    }

    /**
     * Pregătește datele pentru completarea certificatului.
     */
    private Map<String, String> pregatesteDateCertificat(Voluntar voluntar, Organizatie organizatie) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        Map<String, String> data = new HashMap<>();

        // Valori default
        String numeOrg = DEFAULT_NUME_ORGANIZATIE;
        String numeReprez = DEFAULT_REPREZENTANT;
        String functie = DEFAULT_FUNCTIE;

        if (organizatie != null) {
            if (organizatie.getNume() != null) numeOrg = organizatie.getNume();
            User user = organizatie.getUser();
            if (user != null) {
                if (user.getUsername() != null) numeReprez = user.getUsername();
                if (user.getRol() != null) functie = user.getRol().toString();
            }
        }

        // Adăugăm datele în map
        data.put("organizatieNume", numeOrg);
        data.put("dataEmitereCertificat", LocalDate.now().format(formatter));
        data.put("voluntarNumeComplet", voluntar.getNume() + " " + voluntar.getPrenume());
        data.put("perioadaVoluntariatStart", voluntar.getDataInrolare().format(formatter));
        data.put("perioadaVoluntariatEnd", "Prezent");
        data.put("listaProiecteSauActivitateGenerica", "diverse activități și proiecte");
        data.put("puncteVoluntar", String.valueOf(voluntar.getPuncte()));
        data.put("numeReprezentant", numeReprez);
        data.put("functieReprezentant", functie);

        return data;
    }

    // TODO: Adaugă metoda genereazaAdeverintaPdf(...)
}