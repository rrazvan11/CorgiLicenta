package ro.ong.corgi.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.ong.corgi.model.Skill;
import ro.ong.corgi.repository.SkillRepository;
import java.util.List;

@ApplicationScoped
public class SkillService {

    @Inject
    private SkillRepository skillRepository;

    public List<Skill> findAll() {
        return skillRepository.findAll();
    }

    @Transactional
    public void save(Skill skill) {
        // Aici se pot adăuga validări, ex: să nu existe deja un skill cu același nume
        skillRepository.save(skill);
    }

    @Transactional
    public void delete(Skill skill) {
        skillRepository.delete(skill);
    }
}