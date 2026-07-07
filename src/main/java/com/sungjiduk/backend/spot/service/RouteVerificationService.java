package com.sungjiduk.backend.spot.service;

import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.spot.dto.response.RouteVerificationResponse;
import com.sungjiduk.backend.spot.infra.AiRouteVerifyClient;
import com.sungjiduk.backend.spot.infra.BlogPostFetcher;
import com.sungjiduk.backend.spot.infra.NaverBlogClient;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RouteVerificationService {

    private final ContentRepository contentRepository;
    private final PilgrimageSpotRepository spotRepository;
    private final NaverBlogClient naverBlogClient;
    private final BlogPostFetcher postFetcher;
    private final AiRouteVerifyClient aiRouteVerifyClient;

    public RouteVerificationService(
            ContentRepository contentRepository,
            PilgrimageSpotRepository spotRepository,
            NaverBlogClient naverBlogClient,
            BlogPostFetcher postFetcher,
            AiRouteVerifyClient aiRouteVerifyClient
    ) {
        this.contentRepository = contentRepository;
        this.spotRepository = spotRepository;
        this.naverBlogClient = naverBlogClient;
        this.postFetcher = postFetcher;
        this.aiRouteVerifyClient = aiRouteVerifyClient;
    }

    public RouteVerificationResponse verify(Long contentId) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
