package com.se1908.group01.repository;

import com.se1908.group01.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    boolean existsByUser_UserIdAndFriend_UserId(
            Long userId,
            Long friendId
    );

    List<Friendship> findByUser_UserIdOrFriend_UserId(
            Long userId,
            Long userId2
    );

    Optional<Friendship> findByUser_UserIdAndFriend_UserId(
            Long userId,
            Long friendId
    );

}
