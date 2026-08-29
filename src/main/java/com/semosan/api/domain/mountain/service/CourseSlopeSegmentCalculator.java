package com.semosan.api.domain.mountain.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.semosan.api.domain.mountain.dto.response.SlopeSegmentResponse;
import com.semosan.api.domain.mountain.enums.SlopeGrade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 코스 polyline(GeoJSON LineString) + 점별 altitudes 를 받아 경사 등급 segments 로 변환.
 *
 * 처리 단계:
 *  1) altitudes 이동평균(window={@value #SMOOTHING_WINDOW}) smoothing 으로 GPS 노이즈 완화.
 *  2) 인접 점 수평거리(Haversine) 와 고도차로 경사도(%) 산출.
 *  3) {@link SlopeGrade} 로 등급 분류.
 *  4) 같은 등급 연속 segment 묶기.
 *
 * 응답 좌표 인덱스(startIdx, endIdx) 는 polyline.coordinates 의 양 끝 포함(inclusive).
 *
 * 코스의 polyline/altitudes 는 생성 후 수정 API가 없어 사실상 불변이므로, courseId 기준으로
 * 결과를 캐싱한다 (evict 트리거 없이 공통 TTL만으로 충분).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseSlopeSegmentCalculator {

    static final int SMOOTHING_WINDOW = 5;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private final ObjectMapper objectMapper;

    @Cacheable(cacheNames = "courseSlopeSegments", key = "#courseId")
    public List<SlopeSegmentResponse> calculate(Long courseId, String polylineJson, String altitudesJson) {
        if (polylineJson == null || altitudesJson == null) {
            return List.of();
        }
        try {
            JsonNode coords = objectMapper.readTree(polylineJson).get("coordinates");
            if (coords == null || !coords.isArray() || coords.size() < 2) {
                return List.of();
            }
            List<Double> altitudes = objectMapper.readValue(altitudesJson, new TypeReference<>() {});
            // altitudesJson 이 "null" (문자열) 인 경우 readValue 가 null 을 반환한다. 명시적으로 가드한다.
            if (altitudes == null || altitudes.size() != coords.size()) {
                return List.of();
            }

            List<Double> smoothed = movingAverage(altitudes, SMOOTHING_WINDOW);

            List<SlopeSegmentResponse> result = new ArrayList<>();
            int n = coords.size();
            int currentStart = 0;
            SlopeGrade currentGrade = null;

            for (int i = 0; i < n - 1; i++) {
                double lng1 = coords.get(i).get(0).asDouble();
                double lat1 = coords.get(i).get(1).asDouble();
                double lng2 = coords.get(i + 1).get(0).asDouble();
                double lat2 = coords.get(i + 1).get(1).asDouble();
                double distance = haversine(lat1, lng1, lat2, lng2);

                double gradePercent;
                Double a1 = smoothed.get(i);
                Double a2 = smoothed.get(i + 1);
                if (distance == 0.0 || a1 == null || a2 == null) {
                    gradePercent = 0.0;
                } else {
                    gradePercent = (a2 - a1) / distance * 100.0;
                }
                SlopeGrade grade = SlopeGrade.classify(gradePercent);

                if (currentGrade == null) {
                    currentGrade = grade;
                    currentStart = i;
                } else if (currentGrade != grade) {
                    result.add(new SlopeSegmentResponse(currentStart, i, currentGrade));
                    currentGrade = grade;
                    currentStart = i;
                }
            }
            if (currentGrade != null) {
                result.add(new SlopeSegmentResponse(currentStart, n - 1, currentGrade));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to compute slope segments — returning empty list. polylineLen={} altLen={}",
                    polylineJson.length(), altitudesJson.length(), e);
            return List.of();
        }
    }

    private static List<Double> movingAverage(List<Double> altitudes, int window) {
        int n = altitudes.size();
        int half = window / 2;
        List<Double> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int from = Math.max(0, i - half);
            int to = Math.min(n - 1, i + half);
            double sum = 0.0;
            int cnt = 0;
            for (int j = from; j <= to; j++) {
                Double v = altitudes.get(j);
                if (v != null) {
                    sum += v;
                    cnt++;
                }
            }
            out.add(cnt > 0 ? sum / cnt : null);
        }
        return out;
    }

    private static double haversine(double lat1, double lng1, double lat2, double lng2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
