package com.jakdang.labs.entity;

import java.time.Instant;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transfer")  // 실제 DB 테이블명
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TransferEntity {

    /** PK: 매핑 ID */
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "tId", nullable = false, length = 100)
    private String tId;

    /** FK: 권한 그룹 ID (permissionmanagement.pmId) */
    @Column(name = "pmId", nullable = false)
    private Integer pmId;

    /** FK: 사용자 ID (users.id 혹은 memberId) */
    @Column(name = "memberId", nullable = false, length = 100)
    private String memberId;

    /** 생성일시 - 데이터베이스 테이블에 맞춰 직접 정의 */
    @Column(name = "createdAt")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
    
    // Manual getters and setters to fix Lombok compilation issues
    public String getTId() {
        return tId;
    }
    
    public void setTId(String tId) {
        this.tId = tId;
    }
    
    public Integer getPmId() {
        return pmId;
    }
    
    public void setPmId(Integer pmId) {
        this.pmId = pmId;
    }
    
    public String getMemberId() {
        return memberId;
    }
    
    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

