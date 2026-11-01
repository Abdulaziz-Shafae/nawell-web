package com.example.capstone_3.Repository;

import com.example.capstone_3.Model.SkillSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillSuggestionRepository extends JpaRepository<SkillSuggestion,Long>{
 SkillSuggestion findByPendingKey(String pendingKey);
}
