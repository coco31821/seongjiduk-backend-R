package com.sungjiduk.backend.content.entity;

import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/*

CONTENT {
      bigint id PK
      string title
      string category
      string country
      string description
    }

    CONTENT ||--o{ USER_FAVORITE_CONTENT : contains
    CONTENT ||--o{ PILGRIMAGE_SPOT : contains
    CONTENT ||--o{ TRIP_PLAN : selected_for
    CONTENT ||--o{ SPOT_REPORT : reported_for

    Content "1" --> "*" PilgrimageSpot
    Content "1" --> "*" TripPlan
    Content "1" --> "*" UserFavoriteContent
    Content "1" --> "*" SpotReport

 */
@Entity
@Table(name = "contents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false, length = 50)
    private String country;

    @Column(columnDefinition = "TEXT")
    private String description;

    // FK
    @OneToMany(mappedBy = "content")
    private List<PilgrimageSpot> pilgrimageSpots = new ArrayList<>();

//  @OneToMany(mappedBy = "content")
//  private List<TripPlan> tripPlans = new ArrayList<>();

//  @OneToMany(mappedBy = "content")
//  private List<SpotReport> spotReports = new ArrayList<>();

//  @OneToMany(mappedBy = "content")
//  private List<UserFavoriteContent> UserFavoriteContents = new ArrayList<>();

    private Content(String title, String category, String country, String description) {
        this.title = title;
        this.category = category;
        this.country = country;
        this.description = description;
    }

    public static Content create(String title, String category, String country, String description) {
        return new Content(title, category, country, description);
    }

    public void update(String title, String category, String country, String description) {
        this.title = title;
        this.category = category;
        this.country = country;
        this.description = description;
    }
}
