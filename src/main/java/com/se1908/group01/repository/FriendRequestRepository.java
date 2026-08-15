package com.se1908.group01.repository;

import com.se1908.group01.entity.FriendRequest;
import com.se1908.group01.enums.FriendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    boolean existsBySender_UserIdAndReceiver_UserIdAndStatus(
            Long senderId,
            Long receiverId,
            FriendRequestStatus status
    );

    Optional<FriendRequest> findBySender_UserIdAndReceiver_UserIdAndStatus(
            Long senderId,
            Long receiverId,
            FriendRequestStatus status
    );

    List<FriendRequest> findByReceiver_UserIdAndStatusOrderByCreatedAtDesc(
            Long receiverId,
            FriendRequestStatus status
    );

    List<FriendRequest> findBySender_UserIdAndStatusOrderByCreatedAtDesc(
            Long senderId,
            FriendRequestStatus status
    );
}
