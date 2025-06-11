package ro.ong.corgi.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.ong.corgi.model.Enums.Rol;
import ro.ong.corgi.model.User;
import ro.ong.corgi.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
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

    public void changeUserRole(Long userId, Rol newRole) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new RuntimeException("Utilizator inexistent cu ID: " + userId);
        }
        user.setRol(newRole);
        userRepository.update(user);
        System.out.println("Rolul pentru user-ul " + user.getUsername() + " a fost schimbat în " + newRole);
    }
}