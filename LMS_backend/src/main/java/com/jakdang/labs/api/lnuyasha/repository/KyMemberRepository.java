package com.jakdang.labs.api.lnuyasha.repository;

import com.jakdang.labs.entity.MemberEntity;
import com.jakdang.labs.api.lnuyasha.dto.MemberInfoDTO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 회원 정보 조회를 위한 Repository
 * id를 가지고 memberId와 educationId를 조회하는 기능 제공
 */
@Repository
public interface KyMemberRepository extends JpaRepository<MemberEntity, String> {
    
    /**
     * 사용자 ID로 memberId와 educationId를 조회
     * @param id 사용자 ID
     * @return memberId와 educationId를 포함한 회원 정보
     */
    @Query("SELECT new com.jakdang.labs.api.lnuyasha.dto.MemberInfoDTO(m.memberId, m.educationId, m.memberName, m.memberRole, m.memberEmail, m.id) " +
           "FROM MemberEntity m WHERE m.id = :id")
    List<MemberInfoDTO> findMemberInfoById(@Param("id") String id);

    /**
     * educationId가 일치하면서 memberRole=강사인 모든 회원 정보를 조회
     * @param educationId 학원 ID
     */
    @Query("SELECT m FROM MemberEntity m WHERE m.educationId = :educationId AND (m.memberRole = '강사' OR m.memberRole = 'ROLE_INSTRUCTOR' OR m.memberRole = 'INSTRUCTOR')")
    List<MemberEntity> findTeacherByEducationId(@Param("educationId") String educationId);

    /**
     * id와 동일한 educationId가 일치하면서 memberRole 이 학생인 모든 회원 정보를 조회
     * @param id
     * @param educationId 학원 ID
     */
    @Query("SELECT m FROM MemberEntity m WHERE m.id = :id AND m.educationId = :educationId AND m.memberRole = 'ROLE_STUDENT'")
    Optional<MemberEntity> findStudentByIdAndEducationId(@Param("id") String id, @Param("educationId") String educationId);

    /**
     * email로 member 정보를 조회
     * @param email 이메일
     * @return member 정보
     */
    @Query("SELECT new com.jakdang.labs.api.lnuyasha.dto.MemberInfoDTO(m.memberId, m.educationId, m.memberName, m.memberRole, m.memberEmail, m.id) " +
           "FROM MemberEntity m WHERE m.memberEmail = :email")
    List<MemberInfoDTO> findMemberInfoByEmail(@Param("email") String email);

    /**
     * memberId로 member 정보를 조회
     * @param memberId 회원 ID
     * @return member 정보
     */
    @Query("SELECT m FROM MemberEntity m WHERE m.memberId = :memberId")
    List<MemberEntity> findByMemberId(@Param("memberId") String memberId);

    /**
     * id 컬럼으로 member 정보를 조회 (사용자 입력 ID)
     * @param id 사용자 입력 ID
     * @return member 정보
     */
    @Query("SELECT m FROM MemberEntity m WHERE m.id = :id")
    List<MemberEntity> findByIdColumn(@Param("id") String id);
    
    /**
     * memberEmail 컬럼으로 member 정보를 조회
     * @param memberEmail 이메일 주소
     * @return member 정보
     */
    @Query("SELECT m FROM MemberEntity m WHERE m.memberEmail = :memberEmail")
    List<MemberEntity> findByMemberEmail(@Param("memberEmail") String memberEmail);
    
    /**
     * 특정 과정의 특정 역할을 가진 멤버 수 조회
     * @param courseId 과정 ID
     * @param memberRole 멤버 역할
     * @return 멤버 수
     */
    @Query("SELECT COUNT(m) FROM MemberEntity m WHERE m.courseId = :courseId AND m.memberRole = :memberRole")
    int countByCourseIdAndMemberRole(@Param("courseId") String courseId, @Param("memberRole") String memberRole);
    
    /**
     * 특정 과정의 특정 역할을 가진 멤버 목록 조회
     * @param courseId 과정 ID
     * @param memberRole 멤버 역할
     * @return 멤버 목록
     */
    @Query("SELECT m FROM MemberEntity m WHERE m.courseId = :courseId AND m.memberRole = :memberRole")
    List<MemberEntity> findByCourseIdAndMemberRole(@Param("courseId") String courseId, @Param("memberRole") String memberRole);
    
    /**
     * 특정 과정의 학생 목록 조회 (memberExpired가 null인 학생만)
     * @param courseId 과정 ID
     * @return 학생 목록
     */
    @Query("SELECT m FROM MemberEntity m WHERE m.courseId = :courseId AND m.memberRole = 'ROLE_STUDENT' AND m.memberExpired IS NULL")
    List<MemberEntity> findActiveStudentsByCourseId(@Param("courseId") String courseId);
    
    /**
     * userId와 courseId로 memberId를 조회
     * @param userId 사용자 ID (id 컬럼)
     * @param courseId 과정 ID
     * @return memberId
     */
    @Query("SELECT m.memberId FROM MemberEntity m WHERE m.id = :userId AND m.courseId = :courseId")
    String findMemberIdByUserIdAndCourseId(@Param("userId") String userId, @Param("courseId") String courseId);
    
    /**
     * 이메일과 courseId로 member 정보를 조회
     * @param memberEmail 이메일 주소
     * @param courseId 과정 ID
     * @return member 정보
     */
    @Query("SELECT m FROM MemberEntity m WHERE m.memberEmail = :memberEmail AND m.courseId = :courseId")
    MemberEntity findByMemberEmailAndCourseId(@Param("memberEmail") String memberEmail, @Param("courseId") String courseId);
    
} 