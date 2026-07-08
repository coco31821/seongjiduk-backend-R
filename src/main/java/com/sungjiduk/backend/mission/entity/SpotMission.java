package com.sungjiduk.backend.mission.entity;

import com.sungjiduk.backend.mission.constants.MissionOrigin;
import com.sungjiduk.backend.mission.constants.MissionType;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "spot_missions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spot_id", nullable = false)
    private PilgrimageSpot spot;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type", nullable = false, length = 20)
    private MissionType missionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MissionOrigin origin;

    @Column(nullable = false)
    private boolean active = true;

    @Builder
    private SpotMission(PilgrimageSpot spot, String title, String description,
                        MissionType missionType, MissionOrigin origin) {
        this.spot = spot;
        this.title = title;
        this.description = description;
        this.missionType = missionType;
        this.origin = origin;
        this.active = true;
    }

    public static SpotMission create(PilgrimageSpot spot, String title, String description,
                                     MissionType missionType, MissionOrigin origin) {
        return new SpotMission(spot, title, description, missionType, origin);
    }

    public void update(String title, String description, MissionType missionType, boolean active) {
        this.title = title;
        this.description = description;
        this.missionType = missionType;
        this.active = active;
    }
}
