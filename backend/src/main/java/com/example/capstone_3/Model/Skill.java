package com.example.capstone_3.Model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Skill {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotEmpty(message = "Skill name is required")
  @Size(max = 100, message = "Skill name must be at most 100 characters")
  @Column(columnDefinition = "varchar(100) not null unique")
  private String name;



  @Size(max = 500, message = "Description must be at most 500 characters")
  @Column(columnDefinition = "varchar(500)")
  private String description;


  @NotEmpty(message = "Category is required")
  @Size(max = 100, message = "Category must be at most 100 characters")
  @Column(columnDefinition = "varchar(100) not null")
  private String category;

 @JsonIgnore

 @OneToMany(cascade =CascadeType.ALL,mappedBy = "skill")
 private Set<AccountSkill> accountSkills;

 @JsonIgnore

 @OneToMany(cascade=CascadeType.ALL,mappedBy = "skill")
 private Set<LearningRequest>learningRequests;

 @JsonIgnore

 @OneToMany(cascade = CascadeType.ALL,mappedBy = "skill")
 private Set<SkillOffer>skillOffers;











}
