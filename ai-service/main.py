from datetime import datetime, timezone
from typing import Any

from fastapi import FastAPI
from pydantic import BaseModel, Field

app = FastAPI(title="Seongjiduk AI Service", version="0.1.0")


class Spot(BaseModel):
    id: int
    name: str
    city: str | None = None
    recommendedDurationMin: int = 30


class TripConditions(BaseModel):
    durationDays: int = Field(ge=1, le=5)
    budgetLevel: str
    startLocation: str
    travelStyle: str = "PILGRIMAGE_ONLY"


class TripGenerateRequest(BaseModel):
    content: dict[str, Any]
    conditions: TripConditions
    candidateSpots: list[Spot]
    selectedSpotIds: list[int] = []
    excludedSpotIds: list[int] = []


@app.get("/health")
def health() -> dict[str, str]:
    return {
        "status": "UP",
        "checkedAt": datetime.now(timezone.utc).isoformat(),
    }


@app.post("/ai/trips/generate")
def generate_trip(request: TripGenerateRequest) -> dict[str, Any]:
    available_spots = [
        spot for spot in request.candidateSpots
        if spot.id not in set(request.excludedSpotIds)
    ]
    selected_ids = set(request.selectedSpotIds)
    available_spots.sort(key=lambda spot: (spot.id not in selected_ids, spot.id))

    days = []
    duration_days = request.conditions.durationDays
    for day_no in range(1, duration_days + 1):
        day_spots = available_spots[day_no - 1::duration_days][:4]
        stops = []
        for index, spot in enumerate(day_spots, start=1):
            stops.append({
                "sequence": index,
                "spotType": "PILGRIMAGE",
                "spotId": spot.id,
                "name": spot.name,
                "arrivalTime": f"{9 + index}:00",
                "stayMinutes": spot.recommendedDurationMin,
                "reason": f"{spot.name}은 선택한 작품의 성지순례 후보지입니다.",
            })
        days.append({
            "dayNo": day_no,
            "summary": f"Day {day_no} 성지순례 mock route",
            "stops": stops,
        })

    title = f"{request.content.get('title', '성지덕')} {duration_days}일 성지순례"
    return {
        "title": title,
        "days": days,
        "shareText": f"{title} 루트가 생성되었습니다.",
        "provider": "mock",
    }
