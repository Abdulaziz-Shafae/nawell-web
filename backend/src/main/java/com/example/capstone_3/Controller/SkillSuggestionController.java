package com.example.capstone_3.Controller;

import com.example.capstone_3.Api.ApiException;
import com.example.capstone_3.Api.ApiResponse;
import com.example.capstone_3.Model.Account;
import com.example.capstone_3.Repository.AccountRepository;
import com.example.capstone_3.Service.SkillSuggestionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/skill-suggestion")
public class SkillSuggestionController {
 private final SkillSuggestionService service;
 private final AccountRepository accounts;
 private void requireAdmin(HttpSession session){
  Integer id=(Integer)session.getAttribute("accountId");
  Account account=id==null?null:accounts.findAccountById(id);
  if(account==null||!"ACTIVE".equals(account.getStatus())||!"ADMIN".equals(account.getAccountType())) throw new ApiException("Administrator access required");
 }
 @GetMapping("/admin") public ResponseEntity<?> list(HttpSession session){
  requireAdmin(session);return ResponseEntity.ok(service.list());
 }
 @PostMapping("/admin/{id}/approve") public ResponseEntity<?> approve(@PathVariable Long id,HttpSession session){
  requireAdmin(session);service.review(id,true);return ResponseEntity.ok(new ApiResponse("Skill approved"));
 }
 @PostMapping("/admin/{id}/reject") public ResponseEntity<?> reject(@PathVariable Long id,HttpSession session){
  requireAdmin(session);service.review(id,false);return ResponseEntity.ok(new ApiResponse("Skill rejected"));
 }
}
