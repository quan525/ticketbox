package com.example.ticketbox.entity;

import com.example.ticketbox.enums.UserRoles;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(columnNames = "username")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Users extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Email
    private String email;

    @Column(name = "google_subject", unique = true)
    private String googleSubject;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String username;

    @NotBlank
    @Column(nullable = false)
    private String password;

    private LocalDate dob;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRoles role;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean customPermissions = false;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false)
    private java.util.Set<com.example.ticketbox.enums.UserPermission> permissions = new java.util.HashSet<>();

    public java.util.Set<com.example.ticketbox.enums.UserPermission> getEffectivePermissions() {
        if (role == UserRoles.ADMIN || (!customPermissions && role == UserRoles.MODERATOR))
            return java.util.EnumSet.allOf(com.example.ticketbox.enums.UserPermission.class);
        var effective = java.util.EnumSet.noneOf(com.example.ticketbox.enums.UserPermission.class);
        if (customPermissions && permissions != null) effective.addAll(permissions);
        if (effective.contains(com.example.ticketbox.enums.UserPermission.VIEW_OPERATIONS)) {
            for (var permission : com.example.ticketbox.enums.UserPermission.values())
                if (permission.name().startsWith("VIEW_")) effective.add(permission);
        }
        for (var permission : java.util.Set.copyOf(effective)) effective.add(permission.requiredView());
        if (!effective.isEmpty()) effective.add(com.example.ticketbox.enums.UserPermission.VIEW_OPERATIONS);
        return effective;
    }

    public boolean hasPermission(com.example.ticketbox.enums.UserPermission permission) {
        return getEffectivePermissions().contains(permission);
    }
}
