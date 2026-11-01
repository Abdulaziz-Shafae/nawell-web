package com.example.capstone_3.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "pending_signup", indexes = @Index(name = "idx_pending_signup_token", columnList = "tokenHash", unique = true))
public class PendingSignup {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @Column(nullable = false, unique = true, length = 150) private String email;
 @Column(nullable = false, length = 255) private String passwordHash;
 @Column(nullable = false, length = 20) private String accountType;
 @Column(nullable = false, length = 150) private String name;
 @Column(nullable = false, length = 10) private String phone;
 @Column(length = 100) private String city;
 @Column(length = 500) private String description;
 @Column(length = 500) private String imageUrl;
 @Column(nullable = false, length = 64, unique = true) private String tokenHash;
 @Column(nullable = false) private LocalDateTime expiresAt;
 @Column(nullable = false) private LocalDateTime sentAt;
}
