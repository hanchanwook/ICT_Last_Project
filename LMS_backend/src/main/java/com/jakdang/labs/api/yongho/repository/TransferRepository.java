package com.jakdang.labs.api.yongho.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jakdang.labs.entity.TransferEntity;

@Repository
public interface TransferRepository extends JpaRepository<TransferEntity, String> {
    List<TransferEntity> findByPmId(Integer pmId);
    List<TransferEntity> findByMemberId(String memberId);
    void deleteByMemberId(String memberId);
    
} 
