# 성능 실험 기록 템플릿

> 새 실험은 이 파일을 복사해 `infra/perf/<실험명>_OBSERVATION.md`로 작성한다.

## 실험 목적

- 사용자·비용·운영 문제:
- A. 기준선 문제:
- B. 검증할 가설과 해결책:
- C. 성공 기준:

## 고정 조건

| 항목 | 값 |
| --- | --- |
| Git commit | |
| backend replicas | |
| seed | |
| mock 지연·capacity | |
| cache / guard mode | |
| k6 script·RPS·duration | |

## 기준선 (Before)

| 지표 | 값 | 증거 파일 |
| --- | ---: | --- |
| p50 / p95 / p99 | | |
| completed RPS / dropped iterations | | |
| 오류율 | | |
| 외부 API·AI 호출 수 | | |
| 자원·분산 불변식 | | |

## 원인 분석

- 관찰된 사실:
- 원인 가설:
- 검증 방법과 반증 조건:

## 해결책과 대안

| 선택지 | 장점 | 한계·위험 | 선택 여부와 이유 |
| --- | --- | --- | --- |
| 현재 해결책 | | | 선택 |
| 대안 1 | | | |
| 대안 2 | | | |

## 결과 (After)

| 지표 | Before | After | 변화 |
| --- | ---: | ---: | ---: |
| p50 / p95 / p99 | | | |
| completed RPS / dropped iterations | | | |
| 오류율 | | | |
| 외부 API·AI 호출 수 | | | |
| 자원·분산 불변식 | | | |

## 결론과 한계

- 가설 검증 결론:
- 사용자·비용 영향:
- 아직 측정하지 못한 것:
- 다음 실험:

## 재현 명령

```powershell
# build / reset / k6 / mock stats 명령을 적는다.
```
