package com.apptitle.user.entity;

import com.apptitle.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Shared identity/credentials row for every account, regardless of role.
 *
 * Deliberately does NOT hold profile fields (name, LRN, etc.) — those live on
 * Teacher/Student in their own modules (Phase 2), each in a 1:1 relationship
 * with this entity. Keeping auth identity separate from role-specific profile
 * data avoids a wide nullable-everything table and keeps role-specific
 * validation rules (e.g. LRN required only for students) out of this class.
 */
@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt hash — never the raw password. Populated starting Phase 2. */
    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;
}
