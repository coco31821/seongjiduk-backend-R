package com.sungjiduk.backend.spot.service;

import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.spot.dto.response.SpotImportResponse;
import com.sungjiduk.backend.spot.infra.AnitabiClient;
import com.sungjiduk.backend.spot.infra.ReverseGeocoder;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import com.sungjiduk.backend.spot.repository.SpotReferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpotImportService {

    private final ContentRepository contentRepository;
    private final PilgrimageSpotRepository spotRepository;
    private final SpotReferenceRepository referenceRepository;
    private final AnitabiClient anitabiClient;
    private final ReverseGeocoder reverseGeocoder;

    public SpotImportService(
            ContentRepository contentRepository,
            PilgrimageSpotRepository spotRepository,
            SpotReferenceRepository referenceRepository,
            AnitabiClient anitabiClient,
            ReverseGeocoder reverseGeocoder
    ) {
        this.contentRepository = contentRepository;
        this.spotRepository = spotRepository;
        this.referenceRepository = referenceRepository;
        this.anitabiClient = anitabiClient;
        this.reverseGeocoder = reverseGeocoder;
    }

    @Transactional
    public SpotImportResponse importSpots(Long contentId, long bangumiId) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
