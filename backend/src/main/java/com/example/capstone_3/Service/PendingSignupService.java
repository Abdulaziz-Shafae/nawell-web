package com.example.capstone_3.Service;

import com.example.capstone_3.Api.ApiException;
import com.example.capstone_3.DtoIn.RegisterCompanyDtoIn;
import com.example.capstone_3.DtoIn.RegisterIndividualDtoIn;
import com.example.capstone_3.Model.*;
import com.example.capstone_3.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PendingSignupService {
 private final PendingSignupRepository pending;
 private final AccountRepository accounts;
 private final IndividualProfileRepository individuals;
 private final CompanyProfileRepository companies;
 private final BrevoEmailService emailService;
 private final SecureRandom random = new SecureRandom();
 private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

 @Value("${app.public-base-url}") private String publicBaseUrl;

 public void startIndividual(RegisterIndividualDtoIn dto) {
  start(dto.getEmail(),dto.getPassword(),"INDIVIDUAL",dto.getName(),dto.getPhone(),dto.getCity(),dto.getBio(),dto.getProfileImage());
 }
 public void startCompany(RegisterCompanyDtoIn dto) {
  start(dto.getEmail(),dto.getPassword(),"COMPANY",dto.getName(),dto.getPhone(),dto.getCity(),dto.getDescription(),dto.getLogo());
 }

 @Transactional
 public void start(String email,String password,String type,String name,String phone,String city,String description,String image) {
  String normalized=email.trim().toLowerCase(Locale.ROOT);
  if(accounts.findAccountByEmail(normalized)!=null) throw new ApiException("Email already exists");
  if("INDIVIDUAL".equals(type) && individuals.existsByPhone(phone) ||
     "COMPANY".equals(type) && companies.existsByPhone(phone)) throw new ApiException("Phone already exists");
  PendingSignup old=pending.findByEmailIgnoreCase(normalized);
  LocalDateTime now=LocalDateTime.now();
  if(old!=null && now.isBefore(old.getSentAt().plusSeconds(60))) throw new ApiException("Please wait 60 seconds before requesting another email");
  byte[] raw=new byte[32];random.nextBytes(raw);
  String token=HexFormat.of().formatHex(raw);
  PendingSignup signup=old==null?new PendingSignup():old;
  signup.setEmail(normalized);
  signup.setPasswordHash(encoder.encode(password));
  signup.setAccountType(type);
  signup.setName(name);
  signup.setPhone(phone);
  signup.setCity(city);
  signup.setDescription(description);
  signup.setImageUrl(image);
  signup.setTokenHash(digest(token));
  signup.setSentAt(now);
  signup.setExpiresAt(now.plusHours(24));
  // Keep the pending registration until the email link is verified.
  pending.saveAndFlush(signup);
  String link=publicBaseUrl.replaceAll("/+$","")+"/verify-signup?token="+token;
  emailService.sendVerificationEmail(normalized,link);
 }

 @Transactional
 public Integer confirm(String token) {
  if(token==null||!token.matches("[a-f0-9]{64}")) throw new ApiException("Invalid verification link");
  PendingSignup signup=pending.findByTokenHash(digest(token));
  if(signup==null||!LocalDateTime.now().isBefore(signup.getExpiresAt())) throw new ApiException("Invalid or expired verification link");
  if(accounts.findAccountByEmail(signup.getEmail())!=null) throw new ApiException("Email already registered");
  if("INDIVIDUAL".equals(signup.getAccountType()) && individuals.existsByPhone(signup.getPhone()) ||
     "COMPANY".equals(signup.getAccountType()) && companies.existsByPhone(signup.getPhone())) throw new ApiException("Phone already registered");
  Account account=new Account();
  account.setEmail(signup.getEmail());
  account.setPassword(signup.getPasswordHash());
  account.setAccountType(signup.getAccountType());
  account.setStatus("ACTIVE");
  account.setTokenBalance(3);
  account.setEmailVerified(true);
  account.setCreatedAt(LocalDateTime.now());
  if("INDIVIDUAL".equals(signup.getAccountType())) {
   IndividualProfile profile=new IndividualProfile();
   profile.setName(signup.getName());
   profile.setPhone(signup.getPhone());
   profile.setCity(signup.getCity());
   profile.setBio(signup.getDescription());
   profile.setProfileImage(signup.getImageUrl());
   profile.setAccount(account);
   account.setIndividualProfile(profile);
  }else{
   CompanyProfile profile=new CompanyProfile();
   profile.setName(signup.getName());
   profile.setPhone(signup.getPhone());
   profile.setCity(signup.getCity());
   profile.setDescription(signup.getDescription());
   profile.setLogo(signup.getImageUrl());
   profile.setVerified(false);
   profile.setAccount(account);
   account.setCompanyProfile(profile);
  }
  Account saved=accounts.saveAndFlush(account);
  pending.delete(signup);
  return saved.getId();
 }

 private String digest(String token){
  try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}
  catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
}
