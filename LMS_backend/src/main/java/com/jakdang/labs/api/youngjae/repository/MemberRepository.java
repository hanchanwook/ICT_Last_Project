package com.jakdang.labs.api.youngjae.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jakdang.labs.entity.MemberEntity;


@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, String>{
    
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
    
    // 이메일과 역할로 검색 (학생 역할만 조회)
    @Query("SELECT m FROM MemberEntity m WHERE m.memberEmail = :email AND m.memberRole = 'ROLE_STUDENT'")
    List<MemberEntity> findByMemberEmailAndStudentRole(@Param("email") String email);
    
    // 이메일로 모든 Member 조회 (여러 명일 수 있음)
    @Query("SELECT m FROM MemberEntity m WHERE m.memberEmail = :email")
    List<MemberEntity> findAllByMemberEmail(@Param("email") String email);
    
    // courseId와 memberRole로 학생 수 카운트
    @Query("SELECT COUNT(m) FROM MemberEntity m WHERE m.courseId = :courseId AND m.memberRole = :memberRole AND (m.memberExpired IS NULL OR m.memberExpired > CURRENT_TIMESTAMP)")
    int countByCourseIdAndMemberRole(@Param("courseId") String courseId, @Param("memberRole") String memberRole );
    
    // courseId와 memberRole로 멤버 조회
    @Query("SELECT m FROM MemberEntity m WHERE m.courseId = :courseId AND m.memberRole = :memberRole AND (m.memberExpired IS NULL OR m.memberExpired > CURRENT_TIMESTAMP)")
    List<MemberEntity> findByCourseIdAndMemberRole(@Param("courseId") String courseId, @Param("memberRole") String memberRole);
    
    // userId로 학생 member 조회 (ROLE_STUDENT인 경우만)
    @Query("SELECT m FROM MemberEntity m WHERE m.id = :userId AND m.memberRole = 'ROLE_STUDENT' AND (m.memberExpired IS NULL OR m.memberExpired > CURRENT_TIMESTAMP)")
    List<MemberEntity> findStudentByUserId(@Param("userId") String userId);
    
    // userId와 courseId로 학생 member 조회 (ROLE_STUDENT인 경우만)
    @Query("SELECT m FROM MemberEntity m WHERE m.id = :userId AND m.courseId = :courseId AND m.memberRole = 'ROLE_STUDENT' AND (m.memberExpired IS NULL OR m.memberExpired > CURRENT_TIMESTAMP)")
    Optional<MemberEntity> findByUserIdAndCourseIdAndStudentRole(@Param("userId") String userId, @Param("courseId") String courseId);

    // memberId로 Member 조회 (memberId는 MemberEntity의 primary key)
    Optional<MemberEntity> findByMemberId(String memberId);

}
