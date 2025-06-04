package ro.ong.corgi.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ro.ong.corgi.model.User;
import ro.ong.corgi.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class UserService {

    private final UserRepository userRepository;
    private final PasswordService passwordService; // Asigură-te că este injectat

    @Inject
    public UserService(UserRepository userRepository, PasswordService passwordService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }
    protected UserService(){
        this(null,null);
    }

    // Metoda registerUser este foarte similară cu AuthService.register.
    // Ar trebui să decidem unde locuiește logica principală de înregistrare.
    // O voi comenta aici pentru a evita duplicarea cu AuthService.
    // Dacă AuthService.register devine mai complex (cu creare Voluntar/Organizatie),
    // atunci poate registerUser aici e doar pentru admini care creează useri simpli.
    /*
    public void registerUser(String username, String email, String parola, Rol rol) {
        if (userRepository.findByEmail(email) != null) {
            throw new RuntimeException("Există deja un cont cu acest email.");
        }
        if (userRepository.findByUsername(username) != null) {
            throw new RuntimeException("Există deja un cont cu acest username.");
        }
        String parolaHashuita = passwordService.hashPassword(parola);
        User user = User.builder()
                .username(username)
                .email(email)
                .parola(parolaHashuita)
                .rol(rol)
                .activ(true)
                .build();
        userRepository.save(user);
    }
    */

    // Metoda login este duplicat cu AuthService.login. O voi comenta.
    /*
    public User login(String email, String parola) {
        User user = userRepository.findByEmail(email);
        if (user == null || !user.isActiv()) {
            throw new RuntimeException("Cont inexistent sau inactiv.");
        }
        if (!passwordService.verifyPassword(parola, user.getParola())) {
            throw new RuntimeException("Parolă incorectă.");
        }
        return user;
    }
    */

    public void dezactiveazaCont(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new RuntimeException("Utilizator inexistent.");
        }
        user.setActiv(false);
        userRepository.update(user);
    }

    public void reactiveazaCont(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new RuntimeException("Utilizator inexistent.");
        }
        user.setActiv(true);
        userRepository.update(user);
    }

    public void schimbaParola(Long userId, String parolaNoua) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new RuntimeException("Utilizator inexistent.");
        }
        String hashNou = passwordService.hashPassword(parolaNoua);
        user.setParola(hashNou);
        userRepository.update(user);
    }

    public User cautaDupaEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User cautaDupaUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User cautaDupaId(Long id) {
        return userRepository.findById(id);
    }

    public List<User> totiUtilizatoriiActivi() {
        return userRepository.findAll().stream()
                .filter(User::isActiv)
                .collect(Collectors.toList());
    }

    public List<User> totiUtilizatorii() {
        return userRepository.findAll();
    }
}