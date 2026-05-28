package com.semosan.api.domain.semofeed.service;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.semofeed.dto.SemoFeedResponse;
import com.semosan.api.domain.semofeed.entity.SemoFeed;
import com.semosan.api.domain.semofeed.enums.SemoFeedEmojiType;
import com.semosan.api.domain.semofeed.repository.SemoFeedEmojiRepository;
import com.semosan.api.domain.semofeed.repository.SemoFeedRepository;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SemoFeedService {

    private final SemoFeedRepository semoFeedRepository;
    private final SemoFeedEmojiRepository semoFeedEmojiRepository;
    private final UserRepository userRepository;

    @Transactional
    public SemoFeedResponse create(Long userId, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        SemoFeed semoFeed = SemoFeed.create(user, imageUrl);
        return SemoFeedResponse.from(semoFeedRepository.save(semoFeed));
    }

    public List<SemoFeedResponse> listMine(Long userId) {
        List<SemoFeed> semoFeeds = semoFeedRepository.findByUserId(userId);
        SemoFeedResponseAssembler assembler = createResponseAssembler(semoFeeds, userId);
        return semoFeeds.stream()
                .map(assembler::toResponse)
                .toList();
    }

    public Page<SemoFeedResponse> listPublic(Pageable pageable, Long userId) {
        Page<SemoFeed> semoFeeds = semoFeedRepository.findPublic(pageable);
        return toResponsePage(semoFeeds, userId);
    }

    @Transactional
    public boolean togglePublic(Long userId, Long semoFeedId) {
        SemoFeed semoFeed = findOwned(userId, semoFeedId);
        semoFeed.togglePublic();
        return semoFeed.isPublic();
    }

    @Transactional
    public void delete(Long userId, Long semoFeedId) {
        SemoFeed semoFeed = findOwned(userId, semoFeedId);
        semoFeedRepository.delete(semoFeed);
    }

    private SemoFeed findOwned(Long userId, Long semoFeedId) {
        SemoFeed semoFeed = semoFeedRepository.findById(semoFeedId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.SEMOFEED_NOT_FOUND));
        if (!semoFeed.isOwnedBy(userId)) {
            throw new GeneralException(ErrorStatus.SEMOFEED_FORBIDDEN);
        }
        return semoFeed;
    }

    private Page<SemoFeedResponse> toResponsePage(Page<SemoFeed> semoFeeds, Long userId) {
        SemoFeedResponseAssembler assembler = createResponseAssembler(semoFeeds.getContent(), userId);
        return semoFeeds.map(assembler::toResponse);
    }

    private SemoFeedResponseAssembler createResponseAssembler(List<SemoFeed> semoFeeds, Long userId) {
        List<Long> semoFeedIds = semoFeeds.stream()
                .map(SemoFeed::getId)
                .toList();

        Map<Long, Map<SemoFeedEmojiType, Long>> emojiCounts = findEmojiCounts(semoFeedIds);
        Map<Long, Set<SemoFeedEmojiType>> reactedTypes = findReactedTypes(semoFeedIds, userId);

        return new SemoFeedResponseAssembler(userId, emojiCounts, reactedTypes);
    }

    private Map<Long, Map<SemoFeedEmojiType, Long>> findEmojiCounts(List<Long> semoFeedIds) {
        if (semoFeedIds.isEmpty()) {
            return Map.of();
        }

        return semoFeedEmojiRepository.countBySemoFeedIdsGrouped(semoFeedIds).stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.toMap(
                                row -> (SemoFeedEmojiType) row[1],
                                row -> (Long) row[2]
                        )
                ));
    }

    private Map<Long, Set<SemoFeedEmojiType>> findReactedTypes(List<Long> semoFeedIds, Long userId) {
        if (semoFeedIds.isEmpty() || userId == null) {
            return Map.of();
        }

        return semoFeedEmojiRepository.findReactedTypesBySemoFeedIdsAndUserId(semoFeedIds, userId).stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(row -> (SemoFeedEmojiType) row[1], Collectors.toSet())
                ));
    }

    private record SemoFeedResponseAssembler(
            Long userId,
            Map<Long, Map<SemoFeedEmojiType, Long>> emojiCounts,
            Map<Long, Set<SemoFeedEmojiType>> reactedTypes
    ) {

        private SemoFeedResponse toResponse(SemoFeed semoFeed) {
            return SemoFeedResponse.of(
                    semoFeed,
                    completeEmojiCounts(emojiCounts.get(semoFeed.getId())),
                    completeReactedByMe(reactedTypes.get(semoFeed.getId())),
                    userId != null && semoFeed.isOwnedBy(userId)
            );
        }

        private Map<SemoFeedEmojiType, Long> completeEmojiCounts(Map<SemoFeedEmojiType, Long> counts) {
            Map<SemoFeedEmojiType, Long> completeCounts = new EnumMap<>(SemoFeedEmojiType.class);
            for (SemoFeedEmojiType emojiType : SemoFeedEmojiType.values()) {
                completeCounts.put(emojiType, counts == null ? 0L : counts.getOrDefault(emojiType, 0L));
            }
            return completeCounts;
        }

        private Map<SemoFeedEmojiType, Boolean> completeReactedByMe(Set<SemoFeedEmojiType> reactedTypes) {
            Map<SemoFeedEmojiType, Boolean> completeReactedTypes = new EnumMap<>(SemoFeedEmojiType.class);
            for (SemoFeedEmojiType emojiType : SemoFeedEmojiType.values()) {
                completeReactedTypes.put(emojiType, reactedTypes != null && reactedTypes.contains(emojiType));
            }
            return completeReactedTypes;
        }
    }
}
