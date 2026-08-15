package com.se1908.group01.controller;

import com.se1908.group01.dto.ApiResponse;
import com.se1908.group01.dto.FriendRequestResponse;
import com.se1908.group01.dto.FriendResponse;
import com.se1908.group01.dto.SendFriendRequestRequest;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;
    private final CurrentUserService currentUserService;

    /**
     * API gửi yêu cầu kết bạn mới tới một người dùng khác thông qua email.
     *
     * @param request đối tượng DTO chứa email của người nhận yêu cầu kết bạn
     * @return một {@link ApiResponse} chứa thông tin chi tiết của yêu cầu kết bạn vừa tạo
     */
    @PostMapping("/request")
    public ApiResponse<FriendRequestResponse> sendFriendRequest(
            @Valid @RequestBody SendFriendRequestRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUserId();

        var response = friendService.sendFriendRequest(currentUserId, request.getEmail());
        return ApiResponse.success("Send friend request successfully", response);
    }

    /**
     * API lấy danh sách toàn bộ lời mời kết bạn đang ở trạng thái chờ (PENDING) gửi đến cho người dùng hiện tại.
     *
     * @return một {@link ApiResponse} chứa danh sách các lời mời kết bạn gửi đến
     */
    @GetMapping("/requests/incoming")
    public ApiResponse<List<FriendRequestResponse>> getIncomingRequests() {
        Long currentUserId = currentUserService.getCurrentUserId();

        var response = friendService.getIncomingRequests(currentUserId);
        return ApiResponse.success("Get incoming friend requests successfully", response);
    }

    /**
     * API lấy danh sách toàn bộ yêu cầu kết bạn đang ở trạng thái chờ (PENDING) mà người dùng hiện tại đã gửi đi.
     *
     * @return một {@link ApiResponse} chứa danh sách các yêu cầu kết bạn đã gửi đi
     */
    @GetMapping("/requests/outgoing")
    public ApiResponse<List<FriendRequestResponse>> getOutgoingRequests() {
        Long currentUserId = currentUserService.getCurrentUserId();

        var response = friendService.getOutgoingRequests(currentUserId);
        return ApiResponse.success("Get outgoing friend requests successfully", response);
    }

    /**
     * API chấp nhận một yêu cầu kết bạn đang ở trạng thái chờ (PENDING).
     *
     * @param requestId ID của yêu cầu kết bạn cần chấp nhận
     * @return một {@link ApiResponse} chứa thông tin yêu cầu kết bạn sau khi được chấp nhận thành công
     */
    @PostMapping("/requests/{requestId}/accept")
    public ApiResponse<FriendRequestResponse> acceptFriendRequest(
            @PathVariable Long requestId
    ) {
        Long currentUserId = currentUserService.getCurrentUserId();

        var response = friendService.acceptFriendRequest(requestId, currentUserId);
        return ApiResponse.success("Accept friend request successfully", response);
    }

    /**
     * API từ chối một lời mời kết bạn đang ở trạng thái chờ (PENDING).
     *
     * @param requestId ID của yêu cầu kết bạn cần từ chối
     * @return một {@link ApiResponse} chứa thông tin yêu cầu kết bạn sau khi từ chối thành công
     */
    @PostMapping("/requests/{requestId}/reject")
    public ApiResponse<FriendRequestResponse> rejectFriendRequest(
            @PathVariable Long requestId
    ) {
        Long currentUserId = currentUserService.getCurrentUserId();

        var response = friendService.rejectFriendRequest(requestId, currentUserId);
        return ApiResponse.success("Reject friend request successfully", response);
    }

    /**
     * API hủy bỏ một yêu cầu kết bạn đã gửi đi trước đó (đang ở trạng thái PENDING).
     *
     * @param requestId ID của yêu cầu kết bạn cần hủy bỏ
     * @return một {@link ApiResponse} chứa thông tin yêu cầu kết bạn sau khi hủy thành công
     */
    @DeleteMapping("/requests/{requestId}/cancel")
    public ApiResponse<FriendRequestResponse> cancelFriendRequest(
            @PathVariable Long requestId
    ) {
        Long currentUserId = currentUserService.getCurrentUserId();

        var response = friendService.cancelFriendRequest(requestId, currentUserId);
        return ApiResponse.success("Cancel friend request successfully", response);
    }

    /**
     * API hủy bỏ quan hệ bạn bè hiện tại giữa người dùng hiện tại và một người bạn cụ thể.
     *
     * @param friendId ID của người bạn cần hủy kết bạn
     * @return một {@link ApiResponse} phản hồi thông báo hủy kết bạn thành công
     */
    @DeleteMapping("/{friendId}")
    public ApiResponse<Void> unfriend(
            @PathVariable Long friendId
    ) {
        Long currentUserId = currentUserService.getCurrentUserId();

        friendService.unfriend(currentUserId, friendId);

        return ApiResponse.success("Unfriend successfully", null);
    }

    /**
     * API lấy danh sách toàn bộ bạn bè của người dùng hiện tại đang đăng nhập.
     *
     * @return một {@link ApiResponse} chứa danh sách các bạn bè hiện tại
     */
    @GetMapping
    public ApiResponse<List<FriendResponse>> getFriends() {
        Long currentUserId = currentUserService.getCurrentUserId();

        var response = friendService.getFriends(currentUserId);
        return ApiResponse.success("Get friends successfully", response);
    }
}
