package com.se1908.group01.service.impl;

import com.se1908.group01.dto.FriendRequestResponse;
import com.se1908.group01.dto.FriendResponse;
import com.se1908.group01.entity.FriendRequest;
import com.se1908.group01.entity.Friendship;
import com.se1908.group01.entity.User;
import com.se1908.group01.enums.FriendRequestStatus;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.FriendRequestRepository;
import com.se1908.group01.repository.FriendshipRepository;
import com.se1908.group01.repository.UserRepository;
import com.se1908.group01.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FriendServiceImpl implements FriendService {

    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;


    /**
     * Gửi một yêu cầu kết bạn mới từ người dùng hiện tại đến một người dùng khác thông qua email.
     * <p>
     * Các quy tắc kiểm tra hợp lệ:
     * <ul>
     *   <li>Đảm bảo người nhận tồn tại trong cơ sở dữ liệu.</li>
     *   <li>Ngăn chặn người dùng tự gửi yêu cầu kết bạn cho chính mình.</li>
     *   <li>Đảm bảo hai người dùng chưa phải là bạn bè từ trước.</li>
     *   <li>Đảm bảo không có yêu cầu kết bạn nào đang ở trạng thái chờ (PENDING) giữa hai người dùng theo cả hai chiều.</li>
     * </ul>
     *
     * @param senderId ID của người dùng gửi yêu cầu kết bạn
     * @param email    Email của người nhận yêu cầu kết bạn
     * @return một đối tượng {@link FriendRequestResponse} chứa thông tin chi tiết của yêu cầu kết bạn vừa được tạo
     * @throws ResourceNotFoundException nếu không tìm thấy người gửi hoặc người nhận
     * @throws IllegalArgumentException  nếu vi phạm bất kỳ quy tắc kiểm tra hợp lệ nào
     */
    @Override
    public FriendRequestResponse sendFriendRequest(Long senderId, String email) {
        User receiver = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        Long receiverId = receiver.getUserId();

        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("You cannot send friend request to yourself");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        Long user1Id = Math.min(senderId, receiverId);
        Long user2Id = Math.max(senderId, receiverId);

        if (friendshipRepository.existsByUser_UserIdAndFriend_UserId(user1Id, user2Id)) {
            throw new IllegalArgumentException("Users are already friends");
        }

        boolean pendingRequestExists = friendRequestRepository
                .existsBySender_UserIdAndReceiver_UserIdAndStatus(
                        senderId,
                        receiverId,
                        FriendRequestStatus.PENDING
                );

        if (pendingRequestExists) {
            throw new IllegalArgumentException("Friend request already exists");
        }

        boolean reversePendingRequestExists = friendRequestRepository
                .existsBySender_UserIdAndReceiver_UserIdAndStatus(
                        receiverId,
                        senderId,
                        FriendRequestStatus.PENDING
                );

        if (reversePendingRequestExists) {
            throw new IllegalArgumentException("This user has already sent you a friend request");
        }

        FriendRequest friendRequest = FriendRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendRequestStatus.PENDING)
                .build();

        return mapToFriendRequestResponse(
                friendRequestRepository.save(friendRequest)
        );
    }

    /**
     * Chấp nhận một yêu cầu kết bạn đang ở trạng thái chờ (PENDING).
     * <p>
     * Phương thức này thực hiện xác thực quyền sở hữu và trạng thái của yêu cầu kết bạn, tạo một mối quan hệ bạn bè
     * {@link Friendship} mới giữa hai người dùng, cập nhật trạng thái yêu cầu kết bạn thành {@link FriendRequestStatus#ACCEPTED},
     * và ghi nhận thời điểm phản hồi.
     *
     * @param requestId ID của yêu cầu kết bạn cần chấp nhận
     * @param userId    ID của người dùng hiện tại thực hiện chấp nhận yêu cầu (phải là người nhận của yêu cầu đó)
     * @return một đối tượng {@link FriendRequestResponse} với trạng thái đã được cập nhật thành ACCEPTED
     * @throws ResourceNotFoundException nếu không tìm thấy yêu cầu kết bạn hoặc người dùng liên quan
     * @throws IllegalArgumentException  nếu người dùng không có quyền phản hồi, yêu cầu kết bạn không ở trạng thái chờ (PENDING),
     *                                   hoặc hai người dùng đã là bạn bè từ trước
     */
    @Override
    public FriendRequestResponse acceptFriendRequest(Long requestId, Long userId) {
        FriendRequest friendRequest = getPendingRequestForReceiver(requestId, userId);

        Long senderId = friendRequest.getSender().getUserId();
        Long receiverId = friendRequest.getReceiver().getUserId();
        Long user1Id = Math.min(senderId, receiverId);
        Long user2Id = Math.max(senderId, receiverId);

        if (friendshipRepository.existsByUser_UserIdAndFriend_UserId(user1Id, user2Id)) {
            throw new IllegalArgumentException("Users are already friends");
        }

        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Friendship friendship = Friendship.builder()
                .user(user1)
                .friend(user2)
                .build();

        friendshipRepository.save(friendship);

        friendRequest.setStatus(FriendRequestStatus.ACCEPTED);
        friendRequest.setRespondedAt(LocalDateTime.now());

        return mapToFriendRequestResponse(friendRequestRepository.save(friendRequest));
    }

    /**
     * Từ chối lời mời kết bạn đang ở trạng thái chờ xử lý (PENDING).
     * <p>
     * Phương thức tiến hành kiểm tra tính hợp lệ và quyền sở hữu yêu cầu, sau đó chuyển trạng thái
     * của yêu cầu kết bạn thành {@link FriendRequestStatus#REJECTED} và lưu thời gian từ chối.
     *
     * @param requestId ID của yêu cầu kết bạn cần từ chối
     * @param userId    ID của người dùng hiện tại thực hiện từ chối (bắt buộc phải là người nhận lời mời)
     * @return đối tượng {@link FriendRequestResponse} chứa thông tin yêu cầu kết bạn sau khi từ chối
     * @throws ResourceNotFoundException nếu không tìm thấy bản ghi yêu cầu kết bạn trong hệ thống
     * @throws IllegalArgumentException  nếu người dùng không có quyền thao tác hoặc yêu cầu không còn ở trạng thái PENDING
     */
    @Override
    public FriendRequestResponse rejectFriendRequest(Long requestId, Long userId) {
        FriendRequest friendRequest = getPendingRequestForReceiver(requestId, userId);

        friendRequest.setStatus(FriendRequestStatus.REJECTED);
        friendRequest.setRespondedAt(LocalDateTime.now());

        return mapToFriendRequestResponse(friendRequestRepository.save(friendRequest));
    }

    /**
     * Hủy bỏ một yêu cầu kết bạn đã gửi đi đang ở trạng thái chờ (PENDING).
     * <p>
     * Phương thức thực hiện kiểm tra quyền sở hữu (người thực hiện hủy phải là người gửi yêu cầu)
     * và trạng thái của yêu cầu kết bạn. Nếu hợp lệ, cập nhật trạng thái thành {@link FriendRequestStatus#CANCELLED}
     * và ghi nhận mốc thời gian phản hồi.
     *
     * @param requestId ID của yêu cầu kết bạn cần hủy bỏ
     * @param userId    ID của người dùng hiện tại đang đăng nhập thực hiện hủy yêu cầu
     * @return đối tượng {@link FriendRequestResponse} chứa thông tin chi tiết yêu cầu kết bạn sau khi hủy
     * @throws ResourceNotFoundException nếu không tìm thấy bản ghi yêu cầu kết bạn trong hệ thống
     * @throws IllegalArgumentException  nếu người dùng hiện tại không phải là người gửi yêu cầu, hoặc trạng thái của yêu cầu không phải là PENDING
     */
    @Override
    public FriendRequestResponse cancelFriendRequest(Long requestId, Long userId) {
        FriendRequest friendRequest = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (!friendRequest.getSender().getUserId().equals(userId)) {
            throw new IllegalArgumentException("You are not allowed to cancel this request");
        }

        if (friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalArgumentException("Friend request is not pending");
        }

        friendRequest.setStatus(FriendRequestStatus.CANCELLED);
        friendRequest.setRespondedAt(LocalDateTime.now());

        return mapToFriendRequestResponse(friendRequestRepository.save(friendRequest));
    }

    /**
     * Lấy danh sách tất cả các lời mời kết bạn gửi đến đang ở trạng thái chờ (PENDING) của người dùng.
     * <p>
     * Dữ liệu trả về được sắp xếp theo thứ tự thời gian tạo giảm dần (yêu cầu mới nhất được xếp lên đầu).
     *
     * @param userId ID của người dùng nhận lời mời
     * @return danh sách các đối tượng DTO {@link FriendRequestResponse} đại diện cho các lời mời kết bạn gửi đến
     */
    @Override
    public List<FriendRequestResponse> getIncomingRequests(Long userId) {
        return friendRequestRepository
                .findByReceiver_UserIdAndStatusOrderByCreatedAtDesc(userId, FriendRequestStatus.PENDING)
                .stream()
                .map(this::mapToFriendRequestResponse)
                .toList();
    }

    /**
     * Lấy danh sách tất cả các yêu cầu kết bạn đã gửi đi đang ở trạng thái chờ (PENDING) của người dùng.
     * <p>
     * Dữ liệu trả về được sắp xếp theo thứ tự thời gian tạo giảm dần (yêu cầu mới nhất được xếp lên đầu).
     *
     * @param userId ID của người dùng gửi lời mời
     * @return danh sách các đối tượng DTO {@link FriendRequestResponse} đại diện cho các yêu cầu kết bạn đã gửi đi
     */
    @Override
    public List<FriendRequestResponse> getOutgoingRequests(Long userId) {
        return friendRequestRepository
                .findBySender_UserIdAndStatusOrderByCreatedAtDesc(userId, FriendRequestStatus.PENDING)
                .stream()
                .map(this::mapToFriendRequestResponse)
                .toList();
    }

    /**
     * Xóa mối quan hệ bạn bè hiện tại giữa người dùng hiện tại và một người dùng khác.
     * <p>
     * Hệ thống sẽ sắp xếp ID của hai người dùng theo quy tắc chuẩn hóa để định vị chính xác
     * thực thể liên kết trong bảng {@code friendships}, sau đó tiến hành xóa bỏ mối quan hệ này.
     *
     * @param userId   ID của người dùng hiện tại thực hiện thao tác hủy kết bạn
     * @param friendId ID của người bạn cần xóa khỏi danh sách bạn bè
     * @throws ResourceNotFoundException nếu không tìm thấy bản ghi quan hệ bạn bè giữa hai người
     * @throws IllegalArgumentException  nếu ID của người hủy và người bị hủy trùng nhau
     */
    @Override
    public void unfriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("You cannot unfriend yourself");
        }

        Long user1Id = Math.min(userId, friendId);
        Long user2Id = Math.max(userId, friendId);

        Friendship friendship = friendshipRepository
                .findByUser_UserIdAndFriend_UserId(user1Id, user2Id)
                .orElseThrow(() -> new ResourceNotFoundException("Friendship not found"));

        friendshipRepository.delete(friendship);
    }

    /**
     * Lấy danh sách toàn bộ bạn bè của một người dùng cụ thể.
     * <p>
     * Truy vấn này sẽ lấy ra tất cả các mối quan hệ từ bảng {@code friendships} mà người dùng hiện tại
     * tham gia với tư cách là người khởi tạo (user) hoặc người nhận (friend), sau đó ánh xạ thông tin
     * của người bạn tương ứng thành đối tượng DTO {@link FriendResponse}.
     *
     * @param userId ID của người dùng cần lấy danh sách bạn bè
     * @return một {@link List} chứa các đối tượng DTO {@link FriendResponse} đại diện cho bạn bè của người dùng
     */
    @Override
    public List<FriendResponse> getFriends(Long userId) {
        List<Friendship> friendships = friendshipRepository
                .findByUser_UserIdOrFriend_UserId(userId, userId);

        return friendships.stream()
                .map(friendship -> {
                    User friend = friendship.getUser().getUserId().equals(userId)
                            ? friendship.getFriend()
                            : friendship.getUser();

                    return FriendResponse.builder()
                            .friendshipId(friendship.getFriendshipId())
                            .userId(friend.getUserId())
                            .fullName(friend.getFullName())
                            .email(friend.getEmail())
                            .createdAt(friendship.getCreatedAt())
                            .build();
                })
                .toList();
    }

    /**
     * Lấy yêu cầu kết bạn đang ở trạng thái chờ xử lý (PENDING) dành cho người nhận cụ thể và thực hiện xác thực bảo mật.
     * <p>
     * Phương thức này thực hiện các bước kiểm tra an toàn:
     * <ul>
     *   <li>Đảm bảo bản ghi yêu cầu kết bạn có tồn tại trong hệ thống.</li>
     *   <li>Đảm bảo người nhận của yêu cầu kết bạn trùng khớp với người dùng đang thực hiện thao tác (chống giả mạo).</li>
     *   <li>Đảm bảo trạng thái hiện tại của yêu cầu là {@link FriendRequestStatus#PENDING}.</li>
     * </ul>
     *
     * @param requestId ID của yêu cầu kết bạn cần lấy
     * @param userId    ID của người nhận yêu cầu kết bạn thực hiện thao tác
     * @return đối tượng {@link FriendRequest} hợp lệ
     * @throws ResourceNotFoundException nếu không tìm thấy bản ghi yêu cầu kết bạn
     * @throws IllegalArgumentException  nếu người dùng không có quyền phản hồi hoặc yêu cầu không ở trạng thái PENDING
     */
    private FriendRequest getPendingRequestForReceiver(Long requestId, Long userId) {
        FriendRequest friendRequest = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (!friendRequest.getReceiver().getUserId().equals(userId)) {
            throw new IllegalArgumentException("You are not allowed to respond to this request");
        }

        if (friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalArgumentException("Friend request is not pending");
        }

        return friendRequest;
    }

    /**
     * Chuyển đổi đối tượng thực thể {@link FriendRequest} sang đối tượng DTO {@link FriendRequestResponse}.
     * <p>
     * Phương thức này giúp ẩn đi thông tin nhạy cảm của người dùng và định dạng lại cấu trúc dữ liệu trả về cho client.
     *
     * @param friendRequest thực thể yêu cầu kết bạn cần chuyển đổi
     * @return đối tượng DTO {@link FriendRequestResponse} chứa dữ liệu sạch trả về
     */
    private FriendRequestResponse mapToFriendRequestResponse(FriendRequest friendRequest) {
        return FriendRequestResponse.builder()
                .requestId(friendRequest.getRequestId())
                .senderId(friendRequest.getSender().getUserId())
                .senderName(friendRequest.getSender().getFullName())
                .senderEmail(friendRequest.getSender().getEmail())
                .receiverId(friendRequest.getReceiver().getUserId())
                .receiverName(friendRequest.getReceiver().getFullName())
                .receiverEmail(friendRequest.getReceiver().getEmail())
                .status(friendRequest.getStatus().name())
                .createdAt(friendRequest.getCreatedAt())
                .respondedAt(friendRequest.getRespondedAt())
                .build();
    }
}
