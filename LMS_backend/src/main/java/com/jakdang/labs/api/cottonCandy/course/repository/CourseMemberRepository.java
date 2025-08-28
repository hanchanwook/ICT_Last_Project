package com.jakdang.labs.api.cottonCandy.course.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jakdang.labs.entity.MemberEntity;

public interface CourseMemberRepository extends JpaRepository<MemberEntity, String> {
    // 강사, 학생 수 조회
    List<MemberEntity> findByEducationIdAndMemberRole(String educationId, String memberRole);

    List<MemberEntity> findByCourseId(String courseId);
}
