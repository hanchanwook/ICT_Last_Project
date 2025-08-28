package com.jakdang.labs.api.yongho.service;

import java.util.List;

import com.jakdang.labs.api.yongho.dto.TransferDto;

public interface TransferService {
    List<TransferDto> getByMemberId(String memberId);
    List<TransferDto> updatePermissions(String memberId, List<Integer> pmIds);
    Integer findPmIdByUri(String uri);
    boolean hasPermission(String memberId, Integer pmId);
} 
