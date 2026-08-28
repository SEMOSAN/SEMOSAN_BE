package com.semosan.api.domain.user.service;

import com.semosan.api.domain.community.comment.repository.CommentRepository;
import com.semosan.api.domain.community.post.entity.FreePost;
import com.semosan.api.domain.community.post.repository.FreePostRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.DeviceType;
import com.semosan.api.domain.user.repository.UserBlockRepository;
import com.semosan.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * findBlockedUserIdsByBlocker_Id 캐시가 실제 Spring 프록시로 걸리는지, 그리고
 * block/blockByPost/blockByComment self-invocation 경로에서도 evict가 빠지지 않는지 확인한다. (#227)
 *
 * repository 빈은 @Cacheable 때문에 Spring이 캐시 프록시로 감싸므로, ctx.getBean()으로 받은
 * 참조는 프록시다. 스터빙/검증은 AopTestUtils로 원본 mock을 꺼내서 하고, 실제 호출은
 * UserBlockService를 통해 프록시를 거치도록 한다.
 */
@ExtendWith(SpringExtension.class)
class UserBlockCacheTest {

    @Configuration
    @EnableCaching
    static class CacheTestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("blockedUserIds");
        }

        @Bean
        UserBlockRepository userBlockRepository() {
            return mock(UserBlockRepository.class);
        }

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        FreePostRepository freePostRepository() {
            return mock(FreePostRepository.class);
        }

        @Bean
        CommentRepository commentRepository() {
            return mock(CommentRepository.class);
        }

        @Bean
        UserBlockService userBlockService(UserRepository userRepository, UserBlockRepository userBlockRepository,
                                           FreePostRepository freePostRepository, CommentRepository commentRepository) {
            return new UserBlockService(userRepository, userBlockRepository, freePostRepository, commentRepository);
        }
    }

    @Test
    void repeatedLookupHitsCacheUntilBlockEvictsIt() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(CacheTestConfig.class)) {
            UserBlockRepository repositoryProxy = ctx.getBean(UserBlockRepository.class);
            UserBlockRepository repositoryMock = AopTestUtils.getUltimateTargetObject(repositoryProxy);
            UserRepository userRepository = ctx.getBean(UserRepository.class);
            UserBlockService service = ctx.getBean(UserBlockService.class);

            when(repositoryMock.findBlockedUserIdsByBlocker_Id(1L)).thenReturn(List.of(2L));

            assertThat(repositoryProxy.findBlockedUserIdsByBlocker_Id(1L)).containsExactly(2L);
            assertThat(repositoryProxy.findBlockedUserIdsByBlocker_Id(1L)).containsExactly(2L);
            verify(repositoryMock, times(1)).findBlockedUserIdsByBlocker_Id(1L); // 두 번째 호출은 캐시 hit

            when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user(1L)));
            when(userRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(user(3L)));
            when(repositoryMock.existsByBlocker_IdAndBlockedUser_Id(1L, 3L)).thenReturn(false);

            service.block(1L, 3L);

            repositoryProxy.findBlockedUserIdsByBlocker_Id(1L);
            verify(repositoryMock, times(2)).findBlockedUserIdsByBlocker_Id(1L); // evict 되어 다시 DB 조회
        }
    }

    @Test
    void blockByPostEvictsCacheDespiteCallingBlockInternally() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(CacheTestConfig.class)) {
            UserBlockRepository repositoryProxy = ctx.getBean(UserBlockRepository.class);
            UserBlockRepository repositoryMock = AopTestUtils.getUltimateTargetObject(repositoryProxy);
            UserRepository userRepository = ctx.getBean(UserRepository.class);
            FreePostRepository freePostRepository = ctx.getBean(FreePostRepository.class);
            UserBlockService service = ctx.getBean(UserBlockService.class);

            when(repositoryMock.findBlockedUserIdsByBlocker_Id(1L)).thenReturn(List.of());
            repositoryProxy.findBlockedUserIdsByBlocker_Id(1L); // 캐시 채우기

            User author = user(3L);
            when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user(1L)));
            when(userRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(author));
            when(repositoryMock.existsByBlocker_IdAndBlockedUser_Id(1L, 3L)).thenReturn(false);
            when(freePostRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(freePost(author)));

            service.blockByPost(1L, 10L); // block() 을 self-invocation 으로 호출 — 그래도 evict 되어야 함

            when(repositoryMock.findBlockedUserIdsByBlocker_Id(1L)).thenReturn(List.of(3L));
            assertThat(repositoryProxy.findBlockedUserIdsByBlocker_Id(1L)).containsExactly(3L); // stale 캐시가 아니어야 함
        }
    }

    private User user(Long id) {
        User u = User.createTestUser("user" + id, DeviceType.IOS);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private FreePost freePost(User author) {
        try {
            Constructor<FreePost> constructor = FreePost.class.getDeclaredConstructor(User.class, String.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(author, "제목", "본문");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
