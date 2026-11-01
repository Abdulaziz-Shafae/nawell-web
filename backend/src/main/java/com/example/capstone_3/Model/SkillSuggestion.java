package com.example.capstone_3.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity @Getter @Setter
@Table(name="skill_suggestion")
public class SkillSuggestion {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=100) private String name;
 @Column(length=100,unique=true) private String pendingKey;
 @Column(nullable=false,length=20) private String status="PENDING";
 @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
 @ElementCollection(fetch=FetchType.EAGER)
 @CollectionTable(name="skill_suggestion_requester",joinColumns=@JoinColumn(name="suggestion_id"))
 @Column(name="account_id",nullable=false)
 private Set<Integer> requesterIds=new HashSet<>();
}
