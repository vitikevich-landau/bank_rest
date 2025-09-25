package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    @Enumerated(EnumType.STRING)
    private RoleName name;

    @Column(length = 100)
    private String description;

    public enum RoleName {
        ROLE_USER,
        ROLE_ADMIN
    }
}