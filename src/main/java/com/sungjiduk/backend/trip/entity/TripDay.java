package com.sungjiduk.backend.trip.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** 일정의 하루치(Day N) */
@Entity
@Table(name = "trip_day")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_plan_id", nullable = false)
    private TripPlan tripPlan;

    @Column(name = "day_no", nullable = false)
    private int dayNo;

    @Column(length = 500)
    private String summary;

    @OneToMany(mappedBy = "tripDay", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripStop> stops = new ArrayList<>();

    @Builder
    private TripDay(int dayNo, String summary) {
        this.dayNo = dayNo;
        this.summary = summary;
    }

    void assignPlan(TripPlan tripPlan) {
        this.tripPlan = tripPlan;
    }

    public void addStop(TripStop stop) {
        this.stops.add(stop);
        stop.assignDay(this);
    }
}
