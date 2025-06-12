package ro.ong.corgi.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import ro.ong.corgi.model.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DocumentGenerationService {
    private static final String DEFAULT_NUME_ORGANIZATIE = "Organizație";
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

    public byte[] genereazaRaportActivitatePdf(Voluntar voluntar, Map<Proiect, List<Task>> activitati) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        Map<String, String> data = new HashMap<>();

        String numeOrganizatie = DEFAULT_NUME_ORGANIZATIE;
        if (voluntar.getDepartament() != null && voluntar.getDepartament().getOrganizatie() != null) {
            numeOrganizatie = voluntar.getDepartament().getOrganizatie().getNume();
        }

        String departamentNume = "N/A";
        if (voluntar.getDepartament() != null) {
            departamentNume = voluntar.getDepartament().getNume();
        }

        data.put("numeOrganizatie", numeOrganizatie);
        data.put("departamentNume", departamentNume);
        data.put("voluntarNumeComplet", voluntar.getNume() + " " + voluntar.getPrenume());
        data.put("voluntarEmail", voluntar.getUser().getEmail());
        data.put("dataEmitere", LocalDate.now().format(formatter));
        data.put("dataInrolare", voluntar.getDataInrolare().format(formatter));

        StringBuilder activitatiHtml = new StringBuilder();
        for (Map.Entry<Proiect, List<Task>> entry : activitati.entrySet()) {
            Proiect proiect = entry.getKey();
            List<Task> taskuri = entry.getValue();

            activitatiHtml.append("<h3>Proiect: ").append(escapeHtml(proiect.getNumeProiect())).append(":</h3>");

            activitatiHtml.append("<ul>");
            for (Task task : taskuri) {
                String skillName = (task.getSkillDobandit() != null) ? task.getSkillDobandit().getDenumireSkill() : "Nespecificat";
                activitatiHtml.append("<li>")
                        .append(escapeHtml(task.getTitlu()))
                        .append(" - skill dobândit/demonstrat: <strong>")
                        .append(escapeHtml(skillName))
                        .append("</strong></li>");
            }
            activitatiHtml.append("</ul>");
        }
        data.put("listaActivitati", activitatiHtml.toString());

        String htmlContent = loadAndPopulateTemplate("template_RaportProiecteVoluntar.html", data);

        return genereazaPdfDinHtml(htmlContent);
    }

    /**
     * Metoda nouă pentru a genera un raport PDF cu membrii unui departament.
     * Construiește un HTML dinamic, fără a folosi un template extern.
     *
     * @param departament Departamentul pentru care se generează raportul.
     * @param voluntari Lista de voluntari din departament.
     * @return un array de bytes ce reprezintă fișierul PDF.
     */
    public byte[] genereazaRaportDepartamentPdf(Departament departament, List<Voluntar> voluntari) {
        StringBuilder htmlBuilder = new StringBuilder();

        // Antetul HTML și stilurile
        htmlBuilder.append("<!DOCTYPE html><html lang='ro'><head><meta charset='UTF-8'/><title>Raport Departament</title>");
        htmlBuilder.append("<style>body{font-family: 'DejaVu Sans', Arial, sans-serif; margin: 30px; font-size: 10pt;} h1, h2 {color: #2c3e50;} table{width: 100%; border-collapse: collapse; margin-top: 20px;} th, td{border: 1px solid #ddd; padding: 8px; text-align: left;} th{background-color: #f2f2f2;}</style>");
        htmlBuilder.append("</head><body>");

        // Titlul și informațiile raportului
        htmlBuilder.append("<h1>Raport de Membri</h1>");
        htmlBuilder.append("<h2>Departament: ").append(escapeHtml(departament.getNume())).append("</h2>");
        htmlBuilder.append("<p>Data generării: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))).append("</p>");

        // Tabelul cu voluntari
        htmlBuilder.append("<table><thead><tr><th>Nume Complet</th><th>Email</th><th>Status</th><th>Puncte</th></tr></thead><tbody>");
        for (Voluntar v : voluntari) {
            htmlBuilder.append("<tr>");
            htmlBuilder.append("<td>").append(escapeHtml(v.getNumeComplet())).append("</td>");
            htmlBuilder.append("<td>").append(escapeHtml(v.getUser().getEmail())).append("</td>");
            htmlBuilder.append("<td>").append(escapeHtml(v.getStatus().toString())).append("</td>");
            // Folosim String.format pentru a afișa punctele cu două zecimale
            htmlBuilder.append("<td>").append(String.format("%.2f", v.getPuncte() != null ? v.getPuncte() : 0.0)).append("</td>");
            htmlBuilder.append("</tr>");
        }
        htmlBuilder.append("</tbody></table>");
        htmlBuilder.append("</body></html>");

        return genereazaPdfDinHtml(htmlBuilder.toString());
    }


    private String loadAndPopulateTemplate(String numeTemplate, Map<String, String> data) {
        String path = TEMPLATE_FOLDER + numeTemplate;

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                System.err.println("EROARE: Template-ul " + path + " nu a fost găsit în classpath!");
                throw new RuntimeException("Template-ul " + path + " lipsește.");
            }
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            for (Map.Entry<String, String> entry : data.entrySet()) {
                String key = "${" + entry.getKey() + "}";
                String value = (entry.getValue() != null) ? entry.getValue() : "";
                content = content.replace(key, value);
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

            URL fontUrl = getClass().getClassLoader().getResource("fonts/DejaVuSans.ttf");
            if (fontUrl != null) {
                builder.useFont(() -> {
                    try {
                        return fontUrl.openStream();
                    } catch (IOException e) {
                        throw new RuntimeException("Nu s-a putut deschide stream-ul pentru font.", e);
                    }
                }, "DejaVu Sans");
            } else {
                System.err.println("AVERTISMENT: Fontul pentru diacritice (fonts/DejaVuSans.ttf) nu a fost găsit în 'resources'.");
            }

            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Nu s-a putut crea fișierul PDF.", e);
        }
    }

    private Map<String, String> pregatesteDateCertificat(Voluntar voluntar, Organizatie organizatie) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        Map<String, String> data = new HashMap<>();

        String numeOrg = DEFAULT_NUME_ORGANIZATIE;
        if (organizatie != null) {
            if (organizatie.getNume() != null) numeOrg = organizatie.getNume();
        }

        data.put("organizatieNume", numeOrg);
        data.put("dataEmitereCertificat", LocalDate.now().format(formatter));
        data.put("voluntarNumeComplet", voluntar.getNume() + " " + voluntar.getPrenume());
        data.put("perioadaVoluntariatStart", voluntar.getDataInrolare().format(formatter));
        data.put("perioadaVoluntariatEnd", "Prezent");
        data.put("listaProiecteSauActivitateGenerica", "diverse activități și proiecte");
        data.put("puncteVoluntar", String.valueOf(voluntar.getPuncte()));
        data.put("numeReprezentant", DEFAULT_REPREZENTANT);
        data.put("functieReprezentant", DEFAULT_FUNCTIE);

        return data;
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}