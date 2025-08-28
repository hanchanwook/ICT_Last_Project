package com.jakdang.labs.api.jaegyeom.registration.dto;

import java.util.List;
import java.util.stream.Collectors;


import com.jakdang.labs.entity.MemberEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeopleRegisterBulkDto {
    private List<PeopleRegisterDto> people;

    public List<MemberEntity> toEntityList() {
        return people.stream()
                .map(PeopleRegisterDto::toEntity)
                .collect(Collectors.toList());
    }
}