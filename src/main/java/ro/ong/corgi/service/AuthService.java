package ro.ong.corgi.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.ong.corgi.model.User;
import ro.ong.corgi.model.Enums.Rol;
import ro.ong.corgi.repository.UserRepository;

@ApplicationScoped
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    @Inject
    public AuthService(UserRepository userRepository, PasswordService passwordService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    protected AuthService(){ // Constructor pentru proxy-uri CDI, dacă este necesar
        this(null,null);
    }

    public User login(String email, String password) {
        User user = userRepository.findByEmail(email); // Presupune că findByEmail verifică și dacă user.isActiv()
        if (user == null) { // Sau if (user == null || !user.isActiv())
            throw new RuntimeException("Email sau parolă greșite (utilizator negasit sau inactiv)");
        }
        if (!passwordService.verifyPassword(password, user.getParola())) {
            throw new RuntimeException("Email sau parolă greșite (parola incorecta)");
        }
        return user;
    }
    @Transactional
    public User register(String username, String email, String password, Rol rol) {
        if (userRepository.findByEmail(email) != null) {
            throw new RuntimeException("Există deja un utilizator cu acest email: " + email);
        }
        if (userRepository.findByUsername(username) != null) {
            throw new RuntimeException("Există deja un utilizator cu acest username: " + username);
        }

        User newUser = User.builder()
                .username(username)
                .email(email)
                .parola(passwordService.hashPassword(password))
                .rol(rol)
                .activ(true) // Utilizatorii noi sunt activi by default
                .build();

        userRepository.save(newUser);
        return newUser;
    }

    /**
     * Caută un utilizator pe baza adresei de email.
     * @param email Adresa de email a utilizatorului căutat.
     * @return Obiectul User dacă este găsit, altfel null.
     */
    public User cautaDupaEmail(String email) {
        if (email == null || email.isBlank()) {
            // O bună practică este să loghezi astfel de cazuri sau să gestionezi intrarea invalidă.
            // System.err.println("AuthService: Încercare de a căuta user cu email null sau gol.");
            return null;
        }
        // Metoda findByEmail din UserRepository ar trebui să returneze null dacă nu găsește userul.
        return userRepository.findByEmail(email);
    }


    public boolean hasRole(User user, Rol rol) {
        return user != null && user.getRol() == rol;
    }

    public boolean canAccessResource(User user, String resource) {
        if (user == null || !user.isActiv()) {
            return false;
        }
        // Logica de acces poate deveni mai complexă
        return switch (user.getRol()) {
            case SECRETAR -> true; // Secretarul are acces la tot (simplificare)
            case COORDONATOR -> resource.startsWith("proiecte") || resource.startsWith("voluntari_departament") || resource.startsWith("taskuri");
            case VOLUNTAR -> resource.startsWith("profil") || resource.startsWith("taskuri_personale");
            // default -> false; // Pentru orice alte roluri sau dacă nu se potrivește niciun caz
        };
    }
}