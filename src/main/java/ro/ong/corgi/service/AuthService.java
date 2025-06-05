package ro.ong.corgi.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
    protected AuthService(){
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

    public User register(String username, String email, String password, Rol rol) {
        // Aici ar trebui adăugată și logica pentru "codAsociatie" dacă înregistrarea
        // unui VOLUNTAR se face direct prin acest serviciu general de "register".
        // Sau, metoda register specifică pentru voluntari va fi în VoluntarService.

        if (userRepository.findByEmail(email) != null) {
            throw new RuntimeException("Există deja un utilizator cu acest email");
        }
        if (userRepository.findByUsername(username) != null) {
            throw new RuntimeException("Există deja un utilizator cu acest username");
        }

        User newUser = User.builder()
                .username(username)
                .email(email)
                .parola(passwordService.hashPassword(password))
                .rol(rol)
                .activ(true)
                .build();

        userRepository.save(newUser);
        // TODO: Dacă rolul este VOLUNTAR, ar trebui creată și entitatea Voluntar
        // și legată de acest User. Similar pentru ORGANIZATIE.
        // Acest lucru ar trebui probabil gestionat într-un serviciu mai specific
        // sau printr-un flux post-înregistrare.
        return newUser;
    }

    public boolean hasRole(User user, Rol rol) {
        return user != null && user.getRol() == rol;
    }

    public boolean canAccessResource(User user, String resource) {
        if (user == null || !user.isActiv()) {
            return false;
        }
        return switch (user.getRol()) {
            case SECRETAR -> true;
            case COORDONATOR -> resource.startsWith("proiecte") || resource.startsWith("voluntari_departament");
            case VOLUNTAR -> resource.startsWith("profil") || resource.startsWith("taskuri_personale");
        };
    }
}