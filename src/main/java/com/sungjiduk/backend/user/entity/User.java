package com.sungjiduk.backend.user.entity;

import com.sungjiduk.backend.common.BaseEntity;
import com.sungjiduk.backend.user.constants.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
    USER {
      bigint id PK
      string email UK
      string password_hash
      string nickname
      string role
      datetime created_at
    }

    USER ||--o{ USER_PREFERENCE : has
    USER ||--o{ USER_FAVORITE_CONTENT : likes
    USER ||--o{ TRIP_PLAN : creates
    USER ||--o{ VISIT_RECORD : writes
    USER ||--o{ SPOT_REPORT : submits
    USER ||--o{ AI_REQUEST_LOG : triggers
    USER ||--o{ USAGE_EVENT : generates

    User "1" --> "*" UserPreference
    User "1" --> "*" UserFavoriteContent
    User "1" --> "*" TripPlan
    User "1" --> "*" VisitRecord
    User "1" --> "*" SpotReport
    User "1" --> "*" AiRequestLog
    User "1" --> "*" UsageEvent


 */

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    //FK

//  @OneToMany(mappedBy = "user")
//  private List<UserPreference> userPreferences = new ArrayList<>();

//  @OneToMany(mappedBy = "user")
//  private List<UserFavoriteContent> userFavoriteContents = new ArrayList<>();

//  @OneToMany(mappedBy = "user")
//  private List<TripPlan> tripPlans = new ArrayList<>();

//  @OneToMany(mappedBy = "user")
//  private List<VisitRecord> visitRecords = new ArrayList<>();

//  @OneToMany(mappedBy = "user")
//  private List<SpotReport> SpotReports = new ArrayList<>();

//  @OneToMany(mappedBy = "user")
//  private List<AiRequestLog> aiRequestLogs = new ArrayList<>();

//  @OneToMany(mappedBy = "user")
//  private List<UsageEvent> usageEvents = new ArrayList<>();



    @Builder
    public User(
        String email,
        String passwordHash,
        String nickname
    ) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        role = Role.USER;
    }

}
