package com.semosan.api.common.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    @Test
    void gettersReturnAuditingTimestamps() {
        BaseEntity entity = new BaseEntity();
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 6, 13, 40);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 8, 6, 13, 41);

        ReflectionTestUtils.setField(entity, "createdAt", createdAt);
        ReflectionTestUtils.setField(entity, "updatedAt", updatedAt);

        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void baseEntityDeclaresJpaAuditingContract() throws Exception {
        assertThat(BaseEntity.class.isAnnotationPresent(MappedSuperclass.class)).isTrue();
        EntityListeners listeners = BaseEntity.class.getAnnotation(EntityListeners.class);
        assertThat(listeners.value()).containsExactly(AuditingEntityListener.class);

        Field createdAt = BaseEntity.class.getDeclaredField("createdAt");
        assertThat(createdAt.isAnnotationPresent(CreatedDate.class)).isTrue();
        Column createdAtColumn = createdAt.getAnnotation(Column.class);
        assertThat(createdAtColumn.nullable()).isFalse();
        assertThat(createdAtColumn.updatable()).isFalse();

        Field updatedAt = BaseEntity.class.getDeclaredField("updatedAt");
        assertThat(updatedAt.isAnnotationPresent(LastModifiedDate.class)).isTrue();
        Column updatedAtColumn = updatedAt.getAnnotation(Column.class);
        assertThat(updatedAtColumn.nullable()).isFalse();
    }
}
