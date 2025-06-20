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
        User user = userRepository.findByEmail(email);
        if (user == null) {
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

    public User cautaDupaEmail(String email) {
        if (email == null || email.isBlank()) {
            // O bună practică este să loghezi astfel de cazuri sau să gestionezi intrarea invalidă.
            // System.err.println("AuthService: Încercare de a căuta user cu email null sau gol.");
            return null;
        }
        // Metoda findByEmail din UserRepository ar trebui să returneze null dacă nu găsește userul.
        return userRepository.findByEmail(email);
    }
}