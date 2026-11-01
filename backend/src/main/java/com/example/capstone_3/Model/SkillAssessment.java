package com.example.capstone_3.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class SkillAssessment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Score is required")
    @Min(value = 0, message = "Score must be at least 0")
    @Max(value = 100, message = "Score must be at most 100")
    @Column(columnDefinition = "int not null")
    private Integer score;

    @Pattern(regexp = "^(BEGINNER|INTERMEDIATE|ADVANCED|EXPERT)$",
            message = "Assessed level must be BEGINNER, INTERMEDIATE, ADVANCED or EXPERT")
    @Check(constraints = "assessed_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT')")
    @Column(columnDefinition = "varchar(20) not null")
    private String assessedLevel;

    @Column(columnDefinition = "datetime not null")
    private LocalDateTime attemptedAt;


    @Lob
    @Column(name = "review_details", columnDefinition = "LONGTEXT")
    private String reviewDetails;

    @ManyToOne
    @JoinColumn(name = "account_skill_id", referencedColumnName = "id")
    @JsonIgnore
    private AccountSkill accountSkill;



}
