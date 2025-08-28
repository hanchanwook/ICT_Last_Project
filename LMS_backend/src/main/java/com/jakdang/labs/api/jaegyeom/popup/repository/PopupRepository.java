package com.jakdang.labs.api.jaegyeom.popup.repository;

import com.jakdang.labs.api.jaegyeom.popup.entity.PopupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PopupRepository extends JpaRepository<PopupEntity, String> {
    List<PopupEntity> findByEducationId(String educationId);
}
