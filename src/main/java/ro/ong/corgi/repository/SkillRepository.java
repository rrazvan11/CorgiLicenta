package ro.ong.corgi.repository;

import jakarta.enterprise.context.ApplicationScoped;
import ro.ong.corgi.model.Skill;

@ApplicationScoped
public class SkillRepository extends AbstractRepository<Skill, Long> {
    public SkillRepository() {
        super(Skill.class);
    }
}