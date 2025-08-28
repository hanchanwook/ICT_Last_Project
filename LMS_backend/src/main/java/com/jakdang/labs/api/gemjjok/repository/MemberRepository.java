package com.jakdang.labs.api.gemjjok.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jakdang.labs.entity.MemberEntity;

@Repository("gemjjokMemberRepository")
public interface MemberRepository extends JpaRepository<MemberEntity, String> {
    
    List<MemberEntity> findByMemberNameContainingIgnoreCaseOrMemberEmailContainingIgnoreCase(String name, String email);
    
    List<MemberEntity> findByMemberNameContainingIgnoreCaseOrIdContainingIgnoreCase(String name, String id);
    List<MemberEntity> findByMemberNameContainingIgnoreCaseOrIdContainingIgnoreCaseAndMemberRole(String name, String id, String role);
    
    List<MemberEntity> findByMemberNameContainingIgnoreCase(String name);
    
    List<MemberEntity> findByIdIn(List<String> Ids);
    
    Optional<MemberEntity> findById(String Id);
    
    // JWT의 userId로 검색 (member 테이블의 id 필드) - @Query 사용
    @Query("SELECT m FROM MemberEntity m WHERE m.id = :userId")
    Optional<MemberEntity> findByUserId(@Param("userId") String userId);
    
    // 이메일로 검색
    Optional<MemberEntity> findByMemberEmail(String email);
    
    // 이메일로 검색 (JPA 메서드명 규칙)
    Optional<MemberEntity> findByMemberEmailIgnoreCase(String email);
    
    // courseId와 memberRole로 학생 수 카운트
    @Query("SELECT COUNT(m) FROM MemberEntity m WHERE m.courseId = :courseId AND m.memberRole = :memberRole")
    int countByCourseIdAndMemberRole(@Param("courseId") String courseId, @Param("memberRole") String memberRole);
    
    // courseId와 memberRole로 학생 목록 조회
    @Query("SELECT m FROM MemberEntity m WHERE m.courseId = :courseId AND m.memberRole = :memberRole")
    List<MemberEntity> findByCourseIdAndMemberRole(@Param("courseId") String courseId, @Param("memberRole") String memberRole);
    
    // educationId와 memberRole로 학생 목록 조회
    @Query("SELECT m FROM MemberEntity m WHERE m.educationId = :educationId AND m.memberRole = :memberRole")
    List<MemberEntity> findByEducationIdAndMemberRole(@Param("educationId") String educationId, @Param("memberRole") String memberRole);
    
    // userId로 학생 member 조회 (ROLE_STUDENT인 경우만)
    @Query("SELECT m FROM MemberEntity m WHERE m.id = :userId AND m.memberRole = 'ROLE_STUDENT'")
    Optional<MemberEntity> findStudentByUserId(@Param("userId") String userId);
} 