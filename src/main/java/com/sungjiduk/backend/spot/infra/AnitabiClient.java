package com.sungjiduk.backend.spot.infra;

import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.spot.infra.dto.AnitabiPoint;
import com.sungjiduk.backend.spot.infra.dto.AnitabiWork;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Anitabi(api.anitabi.cn) 성지 데이터 클라이언트. 응답 형식은 내부 DTO로 은닉한다.
 * 라이선스: CC BY-NC-SA 4.0 (비상업) — 출처 표기 필수, 이미지 재호스팅 금지(링크만).
 */
@Component
public class AnitabiClient {

    private final RestClient restClient;

    public AnitabiClient(@Value("${seongjiduk.anitabi.base-url:https://api.anitabi.cn}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /** 작품 메타(제목·city). city는 지오코딩 fallback 용. */
    public AnitabiWork fetchWork(long bangumiId) {
        try {
            return restClient.get()
                    .uri("/bangumi/{id}/lite", bangumiId)
                    .retrieve()
                    .body(AnitabiWork.class);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.ANITABI_FETCH_FAILED);
        }
    }

    /** 이미지가 있는 성지 포인트 목록. */
    public List<AnitabiPoint> fetchPoints(long bangumiId) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/bangumi/{id}/points/detail")
                            .queryParam("haveImage", true)
                            .build(bangumiId))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<AnitabiPoint>>() {});
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.ANITABI_FETCH_FAILED);
        }
    }
}
