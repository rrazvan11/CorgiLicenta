package ro.ong.corgi.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.activation.DataSource;
import jakarta.mail.util.ByteArrayDataSource;

import java.util.Properties;

@ApplicationScoped
public class EmailService {
    private final String SMTP_HOST = "smtp.mail.yahoo.com";
    private final String SMTP_PORT = "465";
    private final String SMTP_USER = "razvi.razvi17@yahoo.com";
    private final String SMTP_PASSWORD = "mkopqkummilyypcd";
    private final String FROM_ADDRESS = "razvi.razvi17@yahoo.com";

    public void sendEmailWithAttachment(String toEmail, String subject, byte[] attachmentData, String attachmentFilename) {

        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.socketFactory.port", SMTP_PORT);
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_ADDRESS));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);

            MimeBodyPart attachmentPart = new MimeBodyPart();
            DataSource source = new ByteArrayDataSource(attachmentData, "application/pdf");
            attachmentPart.setDataHandler(new jakarta.activation.DataHandler(source));
            attachmentPart.setFileName(attachmentFilename);

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(attachmentPart);


            message.setContent(multipart);

            Transport.send(message);

            System.out.println("INFO: Email-ul a fost trimis cu succes către " + toEmail);

        } catch (Exception e) {
            System.err.println("EROARE la trimiterea email-ului: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Nu s-a putut trimite email-ul.", e);
        }
    }
}