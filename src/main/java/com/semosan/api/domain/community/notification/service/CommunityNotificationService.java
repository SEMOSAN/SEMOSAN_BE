package com.semosan.api.domain.community.notification.service;

import com.semosan.api.domain.community.comment.entity.Comment;
import com.semosan.api.domain.community.post.entity.Post;
import com.semosan.api.domain.notification.enums.NotificationType;
import com.semosan.api.domain.notification.service.NotificationService;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CommunityNotificationService {

    private static final int COMMENT_PREVIEW_MAX_LENGTH = 50;

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public void sendCommentNotification(Post post, User author, Comment comment) {
        User receiver = post.getAuthor();
        sendIfEligible(
                receiver,
                author,
                NotificationType.COMMUNITY_COMMENT,
                Map.of(
                        "actorId", author.getId(),
                        "actorName", author.displayName(),
                        "postId", post.getId(),
                        "commentId", comment.getId(),
                        "commentPreview", preview(comment.getContent())
                )
        );
    }

    public void sendReplyNotification(Post post, User author, Comment parent, User mentionedUser, Comment reply) {
        Set<Long> sentReceiverIds = new HashSet<>();
        sendReplyNotificationToReceiver(post, author, parent, reply, parent.getAuthor(), sentReceiverIds);
        if (mentionedUser != null) {
            sendReplyNotificationToReceiver(post, author, parent, reply, mentionedUser, sentReceiverIds);
        }
    }

    public void sendPostLikeNotification(Post post, User user) {
        User receiver = post.getAuthor();
        sendIfEligible(
                receiver,
                user,
                NotificationType.COMMUNITY_POST_LIKE,
                Map.of(
                        "actorId", user.getId(),
                        "actorName", user.displayName(),
                        "postId", post.getId()
                )
        );
    }

    private void sendReplyNotificationToReceiver(
            Post post,
            User author,
            Comment parent,
            Comment reply,
            User receiver,
            Set<Long> sentReceiverIds
    ) {
        if (!sentReceiverIds.add(receiver.getId())) {
            return;
        }

        sendIfEligible(
                receiver,
                author,
                NotificationType.COMMUNITY_REPLY,
                Map.of(
                        "actorId", author.getId(),
                        "actorName", author.displayName(),
                        "postId", post.getId(),
                        "parentCommentId", parent.getId(),
                        "commentId", reply.getId(),
                        "commentPreview", preview(reply.getContent())
                )
        );
    }

    private void sendIfEligible(User receiver, User actor, NotificationType type, Map<String, Object> params) {
        if (receiver.getId().equals(actor.getId())) {
            return;
        }
        if (!userRepository.existsByIdAndDeletedFalse(receiver.getId())) {
            return;
        }

        notificationService.send(receiver.getId(), type, params);
    }

    private String preview(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() <= COMMENT_PREVIEW_MAX_LENGTH) {
            return content;
        }
        return content.substring(0, COMMENT_PREVIEW_MAX_LENGTH) + "...";
    }
}
