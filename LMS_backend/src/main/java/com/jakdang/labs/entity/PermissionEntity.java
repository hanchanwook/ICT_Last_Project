package com.jakdang.labs.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "permissionmanagement")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionEntity {
    
    @Id
    @Column(name = "pmId")
    private Integer pmId;

    @Column(name = "permissionName", length = 100)
    private String permissionName;

    @Column(name = "permissionText", length = 100)
    private String permissionText;

    @Column(name = "type", length = 100)
    private String type;

    @Column(name = "createdAt")
    private LocalDate createdAt;

    @Column(name = "updatedAt")
    private LocalDate updatedAt;

    @Column(name = "selfFk")
    private Integer selfFk;
    
    // Manual getters and setters to fix Lombok compilation issues
    public Integer getPmId() {
        return pmId;
    }
    
    public void setPmId(Integer pmId) {
        this.pmId = pmId;
    }
    
    public String getPermissionName() {
        return permissionName;
    }
    
    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }
    
    public String getPermissionText() {
        return permissionText;
    }
    
    public void setPermissionText(String permissionText) {
        this.permissionText = permissionText;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public LocalDate getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDate getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDate updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Integer getSelfFk() {
        return selfFk;
    }
    
    public void setSelfFk(Integer selfFk) {
        this.selfFk = selfFk;
    }
}
