package com.sungjiduk.backend.user.entity;

import com.sungjiduk.backend.content.entity.Content;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*

    USER_PREFERENCE {
      bigint id PK
      bigint user_id FK
      bigint favorite_content_id FK
      string travel_style
      string budget_level
    }

    USER ||--o{ USER_PREFERENCE : has

    User "1" --> "*" UserPreference

 */

@Entity
@Table(name = "user_preferences")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "favorite_content_id")
    private Content favoriteContent;

    @Column(name = "travel_style", length = 50)
    private String travelStyle;

    @Column(name = "budget_level", length = 30)
    private String budgetLevel;

    private UserPreference(User user, Content favoriteContent, String travelStyle, String budgetLevel) {
        this.user = user;
        this.favoriteContent = favoriteContent;
        this.travelStyle = travelStyle;
        this.budgetLevel = budgetLevel;
    }

    public static UserPreference create(User user, Content favoriteContent, String travelStyle, String budgetLevel) {
        return new UserPreference(user, favoriteContent, travelStyle, budgetLevel);
    }

    public void update(Content favoriteContent, String travelStyle, String budgetLevel) {
        this.favoriteContent = favoriteContent;
        this.travelStyle = travelStyle;
        this.budgetLevel = budgetLevel;
    }
}
