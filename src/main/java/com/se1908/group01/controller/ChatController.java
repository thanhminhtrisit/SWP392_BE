package com.se1908.group01.controller;

import com.se1908.group01.dto.ApiResponse;
import com.se1908.group01.dto.ChatAskRequest;
import com.se1908.group01.dto.ChatAskResponse;
import com.se1908.group01.dto.ChatMessageListResponse;
import com.se1908.group01.dto.ChatMessageResponse;
import com.se1908.group01.dto.ChatSessionResponse;
import com.se1908.group01.dto.CreateChatSessionRequest;
import com.se1908.group01.dto.MultiChatAskRequest;
import com.se1908.group01.dto.MultiChatAskResponse;
import com.se1908.group01.dto.SendChatMessageRequest;
import com.se1908.group01.dto.UpdateChatSessionRequest;
import com.se1908.group01.service.ChatSessionService;
import com.se1908.group01.service.ChatService;
import com.se1908.group01.service.MultiChatService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
/**
 * REST entry point cho toàn bộ chức năng hỏi đáp AI.
 *
 * Luồng chính của single-document:
 * FE DocumentChat -> endpoint session -> ChatSessionService -> RAG/LLM -> ApiResponse.
 * Endpoint /ask là đường stateless được FE dùng khi không tạo hoặc gửi được session.
 */
public class ChatController {

	// Service xử lý câu hỏi một tài liệu, không lưu lịch sử.
	private final ChatService chatService;
	// Service xử lý câu hỏi trên nhiều tài liệu hoặc toàn bộ kho tài liệu.
	private final MultiChatService multiChatService;
	// Service quản lý session, lịch sử message và nguồn trích dẫn đã lưu.
	private final ChatSessionService chatSessionService;

	public ChatController(
			ChatService chatService,
			MultiChatService multiChatService,
			ChatSessionService chatSessionService
	) {
		// Spring inject implementation tương ứng qua constructor để controller không tự khởi tạo dependency.
		this.chatService = chatService;
		this.multiChatService = multiChatService;
		this.chatSessionService = chatSessionService;
	}

	@PostMapping("/ask")
	public ApiResponse<ChatAskResponse> ask(@Valid @RequestBody ChatAskRequest request) {
		// Chuyển câu hỏi single-document stateless xuống service; @Valid chặn request thiếu documentId/question.
		var response = chatService.ask(request);
		return ApiResponse.success("Ask document successfully", response);
	}

	@PostMapping("/ask-multi")
	public ApiResponse<MultiChatAskResponse> askMulti(@Valid @RequestBody MultiChatAskRequest request) {
		// @Valid kiểm tra mode/question và service quyết định phạm vi document theo mode.
		var response = multiChatService.askMulti(request);
		// Chuẩn hóa payload thành format ApiResponse chung của toàn backend.
		return ApiResponse.success("Multi-document chat processed", response);
	}

	@PostMapping("/sessions")
	public ApiResponse<ChatSessionResponse> createSession(
			@Valid @RequestBody CreateChatSessionRequest request
	) {
		// Tạo session SelectedDocuments và gắn document được chọn trước khi user gửi message đầu tiên.
		return ApiResponse.success(
				"Create chat session successfully",
				chatSessionService.createSession(request)
		);
	}

	@GetMapping("/sessions")
	public ApiResponse<List<ChatSessionResponse>> getMySessions() {
		// Chỉ trả session chưa bị soft-delete của user đang đăng nhập để FE tìm lại chat tương ứng.
		return ApiResponse.success(
				"Get chat sessions successfully",
				chatSessionService.getMySessions()
		);
	}

	@PatchMapping("/sessions/{sessionId}")
	public ApiResponse<ChatSessionResponse> updateSession(
			@PathVariable Long sessionId,
			@Valid @RequestBody UpdateChatSessionRequest request
	) {
		return ApiResponse.success(
				"Update chat session successfully",
				chatSessionService.updateSession(sessionId, request)
		);
	}

	@DeleteMapping("/sessions/{sessionId}")
	public ApiResponse<Void> deleteSession(@PathVariable Long sessionId) {
		// Service kiểm tra ownership rồi soft-delete session; controller không xóa trực tiếp trong DB.
		chatSessionService.deleteSession(sessionId);
		return ApiResponse.success("Delete chat session successfully", null);
	}

	@GetMapping("/sessions/{sessionId}/messages")
	public ApiResponse<ChatMessageListResponse> getMessages(
			@PathVariable Long sessionId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size
	) {
		// Tải lịch sử message theo trang, đồng thời service ghép các source citation của từng message.
		return ApiResponse.success(
				"Get chat messages successfully",
				chatSessionService.getMessages(sessionId, page, size)
		);
	}

	@PostMapping("/sessions/{sessionId}/messages")
	public ApiResponse<ChatMessageResponse> sendMessage(
			@PathVariable Long sessionId,
			@Valid @RequestBody SendChatMessageRequest request
	) {
		// Gửi câu hỏi vào session; service thực hiện access check, vector search, gọi LLM và lưu kết quả.
		return ApiResponse.success(
				"Send chat message successfully",
				chatSessionService.sendMessage(sessionId, request)
		);
	}
}
