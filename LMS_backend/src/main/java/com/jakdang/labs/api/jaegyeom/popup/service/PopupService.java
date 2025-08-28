package com.jakdang.labs.api.jaegyeom.popup.service;

import com.jakdang.labs.api.jaegyeom.popup.dto.PopupDto;
import com.jakdang.labs.api.jaegyeom.popup.entity.PopupEntity;
import com.jakdang.labs.api.jaegyeom.popup.repository.PopupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PopupService {

    private final PopupRepository popupRepository;

    @Transactional
    public PopupDto createPopup(PopupDto dto) {
        PopupEntity popup = new PopupEntity();
        popup.setEducationId(dto.getEducationId());
        popup.setContent(dto.getContent());
        popup.setImageUrl(dto.getImageUrl());
        popup.setValidFrom(dto.getValidFrom());
        popup.setValidTo(dto.getValidTo());
        popup.setCreatedBy(dto.getCreatedBy());
        popup.setEducationId(dto.getEducationId()); // educationId 저장

        return toDto(popupRepository.save(popup));
    }

    @Transactional(readOnly = true)
    public List<PopupDto> getPopupList(String educationId) {
        return popupRepository.findByEducationId(educationId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public PopupDto updatePopup(PopupDto dto) {
        PopupEntity popup = popupRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("팝업을 찾을 수 없습니다."));
        popup.setContent(dto.getContent());
        popup.setImageUrl(dto.getImageUrl());
        popup.setValidFrom(dto.getValidFrom());
        popup.setValidTo(dto.getValidTo());
        popup.setUpdatedBy(dto.getUpdatedBy());
        popup.setEducationId(dto.getEducationId());

        return toDto(popup);
    }

    @Transactional
    public void deletePopup(String id) {
        popupRepository.deleteById(id);
    }

    private PopupDto toDto(PopupEntity popup) {
        return PopupDto.builder()
                .id(popup.getId())
                .content(popup.getContent())
                .imageUrl(popup.getImageUrl())
                .validFrom(popup.getValidFrom())
                .validTo(popup.getValidTo())
                .createdBy(popup.getCreatedBy())
                .updatedBy(popup.getUpdatedBy())
                .createdAt(popup.getCreatedAt())
                .updatedAt(popup.getUpdatedAt())
                .educationId(popup.getEducationId())
                .build();
    }
}
