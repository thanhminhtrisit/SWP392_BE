package com.se1908.group01.service;

import com.se1908.group01.dto.FriendRequestResponse;
import com.se1908.group01.dto.FriendResponse;

import java.util.List;

public interface FriendService {

    FriendRequestResponse sendFriendRequest(Long senderId, String email);

    FriendRequestResponse acceptFriendRequest(Long requestId, Long userId);

    FriendRequestResponse rejectFriendRequest(Long requestId, Long userId);

    FriendRequestResponse cancelFriendRequest(Long requestId, Long userId);

    List<FriendRequestResponse> getIncomingRequests(Long userId);

    List<FriendRequestResponse> getOutgoingRequests(Long userId);

    void unfriend(Long userId, Long friendId);

    List<FriendResponse> getFriends(Long userId);

}
