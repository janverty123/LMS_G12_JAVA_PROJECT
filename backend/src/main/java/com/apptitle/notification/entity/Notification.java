package com.apptitle.notification.entity;

import com.apptitle.common.entity.BaseEntity;
import com.apptitle.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "notifications", uniqueConstraints = @UniqueConstraint(
        columnNames = {"recipient_id", "type", "reference_key"}))
public class Notification extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String message;

    @Column(name = "reference_key", nullable = false, length = 150)
    private String referenceKey;

    @Column(nullable = false)
    private boolean read;
}
