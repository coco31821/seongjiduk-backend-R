package com.sungjiduk.backend.spot.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*


    SPOT_REFERENCE {
      bigint id PK
      bigint spot_id FK
      string title
      string url
      string source_name
    }

        PILGRIMAGE_SPOT ||--o{ SPOT_REFERENCE : has

 */

@Entity
@Table(name = "spot_references")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spot_id", nullable = false)
    private PilgrimageSpot spot;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "source_name", length = 100)
    private String sourceName;

    private SpotReference(PilgrimageSpot spot, String title, String url, String sourceName) {
        this.spot = spot;
        this.title = title;
        this.url = url;
        this.sourceName = sourceName;
    }

    public static SpotReference create(PilgrimageSpot spot, String title, String url, String sourceName) {
        return new SpotReference(spot, title, url, sourceName);
    }

    public void update(String title, String url, String sourceName) {
        this.title = title;
        this.url = url;
        this.sourceName = sourceName;
    }
}
