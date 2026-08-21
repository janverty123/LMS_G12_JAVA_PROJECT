package com.apptitle.teacher.entity;

import com.apptitle.common.entity.BaseEntity;
import com.apptitle.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Teacher-specific profile data, separate from the auth identity in User.
 * A Teacher owns Sections; ownership checks throughout the app go through
 * this entity rather than User directly.
 */
@Getter
@Setter
@Entity
@Table(name = "teachers")
public class Teacher extends BaseEntity {

    @OneToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String name;
}
