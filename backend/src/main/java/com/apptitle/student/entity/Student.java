package com.apptitle.student.entity;

import com.apptitle.common.entity.BaseEntity;
import com.apptitle.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Student-specific profile data, separate from the auth identity in User.
 *
 * LRN is stored here (not on User) since it's a student-only concept.
 * NOT marked @Column(unique = true): a real-world LRN should be unique per
 * student, but enforcing a DB-level unique constraint here is riskier than
 * it looks — if two teachers' master lists have a typo'd LRN for the same
 * student, or the same LRN is legitimately re-entered before the first
 * registration completes, a hard constraint would produce a confusing
 * failure at registration time rather than a clear "no match found" message.
 * Uniqueness is enforced procedurally in AuthService instead (one Student
 * account per LRN, checked before account creation).
 */
@Getter
@Setter
@Entity
@Table(name = "students")
public class Student extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String lrn;
}
