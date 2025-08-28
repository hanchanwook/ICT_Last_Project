package com.jakdang.labs.api.gemjjok.controller;

import com.jakdang.labs.api.gemjjok.DTO.RubricDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instructor")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@RequiredArgsConstructor
public class RubricTemplateController {
    
    // 루브릭 템플릿 목록 조회 (재사용 가능한 템플릿)
    @GetMapping("/rubric-templates")
    public ResponseEntity<List<RubricDTO>> getRubricTemplates() {
        try {
            // TODO: 루브릭 템플릿 조회 로직 구현
            return ResponseEntity.ok(List.of());
        } catch (Exception e) {
            System.err.println("루브릭 템플릿 조회 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 루브릭 템플릿 저장
    @PostMapping("/rubric-templates")
    public ResponseEntity<RubricDTO> saveRubricTemplate(@RequestBody RubricDTO templateData) {
        try {
            // TODO: 루브릭 템플릿 저장 로직 구현
            return ResponseEntity.ok(templateData);
        } catch (Exception e) {
            System.err.println("루브릭 템플릿 저장 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
} 