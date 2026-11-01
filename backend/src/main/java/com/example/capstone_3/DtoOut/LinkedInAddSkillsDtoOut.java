package com.example.capstone_3.DtoOut;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LinkedInAddSkillsDtoOut {

    private Integer accountId;
    private List<String> createdSkills;
    private List<String> addedSkills;
    private List<String> alreadyOwnedSkills;
    private List<String> pendingSkills;
}