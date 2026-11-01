package com.example.capstone_3.Config;

import com.example.capstone_3.Model.Skill;
import com.example.capstone_3.Repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SkillCatalogSeeder implements CommandLineRunner {
    private final SkillRepository skills;
    @Override
    @Transactional
    public void run(String... args) {
        Set<String> existing = skills.findAll().stream()
                .map(Skill::getName).filter(n -> n != null)
                .map(n -> n.trim().toLowerCase(java.util.Locale.ROOT))
                .collect(Collectors.toSet());
        List<String[]> defaults = List.of(
                new String[]{"Java", "Programming"}, new String[]{"Python", "Programming"},
                new String[]{"JavaScript", "Programming"}, new String[]{"TypeScript", "Programming"},
                new String[]{"React", "Web Development"}, new String[]{"Spring Boot", "Web Development"},
                new String[]{"HTML", "Web Development"}, new String[]{"CSS", "Web Development"},
                new String[]{"REST APIs", "Web Development"}, new String[]{"SQL", "Databases"},
                new String[]{"MySQL", "Databases"}, new String[]{"PostgreSQL", "Databases"},
                new String[]{"Database Design", "Databases"}, new String[]{"Git", "Tools"},
                new String[]{"GitHub", "Tools"}, new String[]{"Docker", "DevOps"},
                new String[]{"Linux", "DevOps"}, new String[]{"AWS", "Cloud"},
                new String[]{"UI/UX Design", "Design"}, new String[]{"Figma", "Design"},
                new String[]{"Graphic Design", "Design"}, new String[]{"Data Analysis", "Data"},
                new String[]{"Microsoft Excel", "Business"}, new String[]{"Project Management", "Business"},
                new String[]{"Digital Marketing", "Marketing"}, new String[]{"Content Writing", "Communication"},
                new String[]{"English", "Languages"}, new String[]{"Arabic", "Languages"},
                new String[]{"Public Speaking", "Communication"}, new String[]{"Photography", "Creative"}
        );
        for (String[] item : defaults) {
            if (existing.add(item[0].toLowerCase(java.util.Locale.ROOT))) {
                Skill skill = new Skill();
                skill.setName(item[0]);
                skill.setCategory(item[1]);
                skill.setDescription("Learn or share " + item[0]);
                skills.save(skill);
            }
        }
    }
}
