package com.example.capstone_3.DtoOut;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssessmentResultDtoOut {
    private String skillName;
    private Integer score;
    private String assessedLevel;
    private Boolean passed;
    private java.util.List<java.util.Map<String,Object>> questionResults;

}
