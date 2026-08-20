package com.semosan.api.domain.mountain.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse.CourseInfo;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse.MountainInfo;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse.RestaurantInfo;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse.RestaurantSectionInfo;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse.ReviewInfo;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse.TransportationGroup;
import com.semosan.api.domain.mountain.dto.response.MountainDetailResponse.TransportationItem;
import com.semosan.api.domain.mountain.enums.AmenityType;
import com.semosan.api.domain.mountain.enums.Difficulty;
import com.semosan.api.domain.mountain.enums.TransportationType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MountainDetailQueryRepository {

    private static final int REVIEW_LIMIT = 20;
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public Optional<MountainDetailResponse> findDetailByMountainId(Long mountainId) {
        List<MountainDetailResponse> responses = jdbcTemplate.query("""
                SELECT
                    m.id AS mountain_id,
                    m.name AS mountain_name,
                    m.address AS address,
                    m.altitude AS altitude,
                    m.difficulty AS difficulty,
                    m.duration AS duration,
                    m.image_urls::text AS image_urls,
                    m.latitude AS latitude,
                    m.longitude AS longitude,
                    COALESCE((
                        SELECT jsonb_agg(jsonb_build_object(
                            'courseId', c.id,
                            'name', c.name,
                            'difficulty', c.difficulty,
                            'distance', c.distance,
                            'duration', c.duration,
                            'startName', c.start_name,
                            'endName', c.end_name
                        ) ORDER BY c.id)
                        FROM courses c
                        WHERE c.mountain_id = m.id
                    ), '[]'::jsonb)::text AS courses_json,
                    COALESCE((
                        SELECT jsonb_agg(jsonb_build_object(
                            'transportationId', t.id,
                            'type', t.type,
                            'direction', t.direction,
                            'name', t.name,
                            'description', t.description
                        ) ORDER BY t.id)
                        FROM transportations t
                        WHERE t.mountain_id = m.id
                    ), '[]'::jsonb)::text AS transportations_json,
                    COALESCE((
                        SELECT jsonb_agg(jsonb_build_object(
                            'type', a.type,
                            'direction', a.direction
                        ) ORDER BY a.id)
                        FROM amenities a
                        WHERE a.mountain_id = m.id
                    ), '[]'::jsonb)::text AS amenities_json,
                    COALESCE((
                        SELECT jsonb_agg(jsonb_build_object(
                            'title', rs.title,
                            'restaurants', COALESCE((
                                SELECT jsonb_agg(jsonb_build_object(
                                    'restaurantId', r.id,
                                    'name', r.name,
                                    'category', r.category,
                                    'imageUrl', r.image_url,
                                    'mapUrl', r.map_url
                                ) ORDER BY r.id)
                                FROM restaurants r
                                WHERE r.section_id = rs.id
                            ), '[]'::jsonb)
                        ) ORDER BY rs.id)
                        FROM restaurant_sections rs
                        WHERE rs.mountain_id = m.id
                    ), '[]'::jsonb)::text AS restaurant_sections_json,
                    COALESCE((
                        SELECT jsonb_agg(jsonb_build_object(
                            'reviewId', rv.id,
                            'imageUrl', rv.image_url,
                            'authorName', u.name,
                            'content', rv.content,
                            'difficulty', rv.difficulty,
                            'courseName', c.name
                        ) ORDER BY rv.created_at DESC, rv.id DESC)
                        FROM (
                            SELECT *
                            FROM reviews
                            WHERE mountain_id = m.id
                            ORDER BY created_at DESC, id DESC
                            LIMIT ?
                        ) rv
                        JOIN users u ON u.id = rv.user_id
                        LEFT JOIN courses c ON c.id = rv.course_id
                    ), '[]'::jsonb)::text AS reviews_json
                FROM mountains m
                WHERE m.id = ?
                """, (rs, rowNum) -> mapDetail(rs), REVIEW_LIMIT, mountainId);

        return responses.stream().findFirst();
    }

    private MountainDetailResponse mapDetail(ResultSet rs) throws SQLException {
        return new MountainDetailResponse(
                new MountainInfo(
                        rs.getLong("mountain_id"),
                        rs.getString("mountain_name"),
                        rs.getString("address"),
                        getDouble(rs, "altitude"),
                        Difficulty.valueOf(rs.getString("difficulty")),
                        rs.getObject("duration", Integer.class),
                        parseImageUrls(rs.getString("image_urls")),
                        getDouble(rs, "latitude"),
                        getDouble(rs, "longitude")
                ),
                parseCourses(rs.getString("courses_json")),
                parseTransportations(rs.getString("transportations_json")),
                parseAmenities(rs.getString("amenities_json")),
                parseRestaurantSections(rs.getString("restaurant_sections_json")),
                parseReviews(rs.getString("reviews_json"))
        );
    }

    private List<String> parseImageUrls(String rawJson) {
        if (rawJson == null) {
            return null;
        }
        try {
            return objectMapper.readValue(rawJson, STRING_LIST_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse mountain image_urls", e);
        }
    }

    private List<CourseInfo> parseCourses(String rawJson) {
        List<CourseInfo> courses = new ArrayList<>();
        for (JsonNode node : parseArray(rawJson)) {
            courses.add(new CourseInfo(
                    node.path("courseId").asLong(),
                    textOrNull(node, "name"),
                    enumValue(Difficulty.class, node, "difficulty"),
                    doubleOrNull(node, "distance"),
                    intOrNull(node, "duration"),
                    textOrNull(node, "startName"),
                    textOrNull(node, "endName")
            ));
        }
        return courses;
    }

    private TransportationGroup parseTransportations(String rawJson) {
        Map<String, List<TransportationItem>> publicTransport = new LinkedHashMap<>();
        Map<String, List<TransportationItem>> parking = new LinkedHashMap<>();

        for (JsonNode node : parseArray(rawJson)) {
            TransportationType type = enumValue(TransportationType.class, node, "type");
            String direction = textOrNull(node, "direction");
            TransportationItem item = new TransportationItem(
                    node.path("transportationId").asLong(),
                    type,
                    textOrNull(node, "name"),
                    textOrNull(node, "description")
            );

            Map<String, List<TransportationItem>> target =
                    type == TransportationType.PARKING ? parking : publicTransport;
            target.computeIfAbsent(direction, ignored -> new ArrayList<>()).add(item);
        }

        return new TransportationGroup(publicTransport, parking);
    }

    private Map<String, List<AmenityType>> parseAmenities(String rawJson) {
        Map<String, List<AmenityType>> amenities = new LinkedHashMap<>();
        for (JsonNode node : parseArray(rawJson)) {
            String direction = textOrNull(node, "direction");
            AmenityType type = enumValue(AmenityType.class, node, "type");
            amenities.computeIfAbsent(direction, ignored -> new ArrayList<>()).add(type);
        }
        return amenities;
    }

    private List<RestaurantSectionInfo> parseRestaurantSections(String rawJson) {
        List<RestaurantSectionInfo> sections = new ArrayList<>();
        for (JsonNode sectionNode : parseArray(rawJson)) {
            List<RestaurantInfo> restaurants = new ArrayList<>();
            for (JsonNode restaurantNode : sectionNode.path("restaurants")) {
                restaurants.add(new RestaurantInfo(
                        restaurantNode.path("restaurantId").asLong(),
                        textOrNull(restaurantNode, "name"),
                        textOrNull(restaurantNode, "category"),
                        textOrNull(restaurantNode, "imageUrl"),
                        textOrNull(restaurantNode, "mapUrl")
                ));
            }
            sections.add(new RestaurantSectionInfo(textOrNull(sectionNode, "title"), restaurants));
        }
        return sections;
    }

    private List<ReviewInfo> parseReviews(String rawJson) {
        List<ReviewInfo> reviews = new ArrayList<>();
        for (JsonNode node : parseArray(rawJson)) {
            reviews.add(new ReviewInfo(
                    node.path("reviewId").asLong(),
                    textOrNull(node, "imageUrl"),
                    textOrNull(node, "authorName"),
                    textOrNull(node, "content"),
                    enumValue(Difficulty.class, node, "difficulty"),
                    textOrNull(node, "courseName")
            ));
        }
        return reviews;
    }

    private JsonNode parseArray(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse mountain detail JSON", e);
        }
    }

    private String textOrNull(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field == null || field.isNull() ? null : field.asText();
    }

    private Double doubleOrNull(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field == null || field.isNull() ? null : field.asDouble();
    }

    private Integer intOrNull(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field == null || field.isNull() ? null : field.asInt();
    }

    private Double getDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private <E extends Enum<E>> E enumValue(Class<E> enumType, JsonNode node, String fieldName) {
        return Enum.valueOf(enumType, textOrNull(node, fieldName));
    }
}
