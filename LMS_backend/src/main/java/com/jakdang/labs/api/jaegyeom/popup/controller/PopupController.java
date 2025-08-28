package com.jakdang.labs.api.jaegyeom.popup.controller;

import com.jakdang.labs.api.jaegyeom.popup.dto.PopupDto;
import com.jakdang.labs.api.jaegyeom.popup.service.PopupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/popup")
@RequiredArgsConstructor
@Slf4j
public class PopupController {

    private final PopupService popupService;

    @PostMapping("/create")
    public ResponseEntity<PopupDto> createPopup(@RequestBody PopupDto dto) {
        // 이미지 크기 검증 (base64 데이터가 너무 큰 경우)
        if (dto.getImageUrl() != null && dto.getImageUrl().length() > 1000000) { // 약 1MB 제한
            log.warn("이미지 크기가 너무 큽니다. 크기: {} bytes", dto.getImageUrl().length());
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(popupService.createPopup(dto));
    }

    @GetMapping("/list")
    public ResponseEntity<List<PopupDto>> getPopupList(@RequestParam String educationId) {
        return ResponseEntity.ok(popupService.getPopupList(educationId));
    }

    @PutMapping("/update")
    public ResponseEntity<PopupDto> updatePopup(@RequestBody PopupDto dto) {
        return ResponseEntity.ok(popupService.updatePopup(dto));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deletePopup(@RequestParam String id) {
        popupService.deletePopup(id);
        return ResponseEntity.noContent().build();
    }
}
