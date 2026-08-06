package com.semosan.api.common.base;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    @Test
    void gettersReturnAuditingTimestamps() {
        TestEntity entity = new TestEntity();
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 6, 13, 40);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 8, 6, 13, 41);

        ReflectionTestUtils.setField(entity, "createdAt", createdAt);
        ReflectionTestUtils.setField(entity, "updatedAt", updatedAt);

        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
    }

    private static class TestEntity extends BaseEntity {
    }
}
