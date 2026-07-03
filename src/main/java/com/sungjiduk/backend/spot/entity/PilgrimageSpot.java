package com.sungjiduk.backend.spot.entity;

import com.sungjiduk.backend.content.entity.Content;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/*

    PILGRIMAGE_SPOT {
      bigint id PK
      bigint content_id FK
      string name
      string address
      decimal lat
      decimal lng
      string city
      int recommended_duration_min
      string reference_url
    }

    CONTENT ||--o{ PILGRIMAGE_SPOT : contains
    PILGRIMAGE_SPOT ||--o{ VISITED_RECORD : visited
    PILGRIMAGE_SPOT ||--o{ TRIP_STOP : scheduled_as_prilgrimage

    Content "1" --> "*" PilgrimageSpot
    PilgrimageSpot "1" --> "*" VisitedRecord
    PilgrimageSpot "1" --> "*" TripStop
 */

@Entity
@Table(name = "pilgrimage_spots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PilgrimageSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(nullable = false, length = 50)
    private String city;

    @Column(name = "recommended_duration_min", nullable = false)
    private Integer recommendedDurationMin;

    @Column(name = "reference_url", length = 500)
    private String referenceUrl;

    // FK
//    @OneToMany(mappedBy = "spot")
//    private List<VisitedRecord> VisitedRecords = new ArrayList<>();
//
//    @OneToMany(mappedBy = "spot")
//    private List<TripStop> TripStops = new ArrayList<>();
//


    private PilgrimageSpot(
            Content content,
            String name,
            String address,
            BigDecimal lat,
            BigDecimal lng,
            String city,
            Integer recommendedDurationMin,
            String referenceUrl
    ) {
        this.content = content;
        this.name = name;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.city = city;
        this.recommendedDurationMin = recommendedDurationMin;
        this.referenceUrl = referenceUrl;
    }

    public static PilgrimageSpot create(
            Content content,
            String name,
            String address,
            BigDecimal lat,
            BigDecimal lng,
            String city,
            Integer recommendedDurationMin,
            String referenceUrl
    ) {
        return new PilgrimageSpot(content, name, address, lat, lng, city, recommendedDurationMin, referenceUrl);
    }

    public void update(
            Content content,
            String name,
            String address,
            BigDecimal lat,
            BigDecimal lng,
            String city,
            Integer recommendedDurationMin,
            String referenceUrl
    ) {
        this.content = content;
        this.name = name;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.city = city;
        this.recommendedDurationMin = recommendedDurationMin;
        this.referenceUrl = referenceUrl;
    }
}
