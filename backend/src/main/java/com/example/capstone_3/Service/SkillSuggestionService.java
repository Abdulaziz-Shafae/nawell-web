package com.example.capstone_3.Service;

import com.example.capstone_3.Api.ApiException;
import com.example.capstone_3.Model.*;
import com.example.capstone_3.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class SkillSuggestionService {
 private final SkillSuggestionRepository suggestions;
 private final SkillRepository skills;
 private final AccountRepository accounts;
 private final AccountSkillRepository accountSkills;
 public List<SkillSuggestion> list(){return suggestions.findAll();}
 @Transactional
 public void request(Integer accountId,String name){
  if(accounts.findAccountById(accountId)==null) throw new ApiException("Login required");
  if(name==null||name.isBlank()||name.trim().length()>100) throw new ApiException("Invalid skill name");
  String display=name.trim().replaceAll("\\s+"," ");
  if(skills.findSkillByNameIgnoreCase(display)!=null) throw new ApiException("Skill already exists");
  String key=display.toLowerCase(Locale.ROOT);
  SkillSuggestion suggestion=suggestions.findByPendingKey(key);
  if(suggestion==null){suggestion=new SkillSuggestion();suggestion.setName(display);suggestion.setPendingKey(key);}
  suggestion.getRequesterIds().add(accountId);
  suggestions.save(suggestion);
 }
 @Transactional
 public void review(Long id,boolean approve){
  SkillSuggestion suggestion=suggestions.findById(id).orElseThrow(()->new ApiException("Suggestion not found"));
  if(!"PENDING".equals(suggestion.getStatus())) throw new ApiException("Suggestion already reviewed");
  if(approve){
   Skill skill=skills.findSkillByNameIgnoreCase(suggestion.getName());
   if(skill==null){skill=new Skill();skill.setName(suggestion.getName());skill.setCategory("General");skill=skills.saveAndFlush(skill);}
   for(Integer userId:suggestion.getRequesterIds()){
    Account user=accounts.findAccountById(userId);
    if(user!=null&&accountSkills.findAccountSkillByAccountAndSkill(user,skill)==null){
     AccountSkill item=new AccountSkill();item.setAccount(user);item.setSkill(skill);item.setLevel("BEGINNER");item.setVerified(false);accountSkills.save(item);
    }
   }
  }
  suggestion.setStatus(approve?"APPROVED":"REJECTED");
  suggestion.setPendingKey(null);
  suggestions.save(suggestion);
 }
}
