package com.se1908.group01.service.impl;

import com.se1908.group01.dto.AiGenerationOptions;
import com.se1908.group01.service.AiChatClientService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
/**
 * Adapter nối service nghiệp vụ với Spring AI OpenAI ChatClient.
 * Lớp này nhận prompt đã bị giới hạn theo document context và trả nội dung answer từ model.
 *
 * [SUA NGAY 2026-08-11 - co ho tro cua AI] Doi provider Google GenAI -> OpenAI.
 * Chi doi kieu options (GoogleGenAiChatOptions -> OpenAiChatOptions) va cac thong bao loi.
 * Toan bo logic nghiep vu giu nguyen, vi ChatClient la interface chung cua Spring AI.
 */
public class AiChatClientServiceImpl implements AiChatClientService {

	// ObjectProvider cho phép application vẫn khởi động khi chưa cấu hình ChatClient/provider.
	private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

	public AiChatClientServiceImpl(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
		// Spring inject provider; ChatClient thật chỉ được lấy khi có request AI.
		this.chatClientBuilderProvider = chatClientBuilderProvider;
	}

	@Override
	public String ask(String prompt, AiGenerationOptions options) {
		// Lấy ChatClient do Spring cấu hình; thiếu provider thì trả SERVICE_UNAVAILABLE thay vì giả lập answer.
		var builder = chatClientBuilderProvider.getIfAvailable();
		if (builder == null) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Spring AI chat model is not configured. Set SPRING_AI_MODEL_CHAT=openai and OPENAI_API_KEY.");
		}

		try {
			// Truyền model đã được resolve và prompt RAG vào OpenAI.
			var chatOptions = OpenAiChatOptions.builder()
					// Chuyển model nghiệp vụ sang tên model mà OpenAI hiểu.
					.model(options.model().getProviderModel())
					// [SUA NGAY 2026-08-11 - co ho tro cua AI] DA BO .temperature(options.temperature())
					//
					// LY DO: dong model GPT-5.6 khoa cung temperature. Goi thu truc tiep API
					// tra ve loi:
					//   "Unsupported value: 'temperature' does not support 0.2 with this model.
					//    Only the default (1) value is supported."  (code: unsupported_value)
					// Day la dac diem chung cua cac model thien ve suy luan - chung tu quan ly
					// do ngau nhien ben trong, khong cho chinh tu ngoai.
					//
					// HE QUA: truong "temperature" trong request API van duoc nhan, van bi validate
					// trong khoang 0.0-1.0, van luu vao cot chat_session.temperature - NHUNG KHONG
					// CO TAC DUNG gi len cau tra loi. Da ghi chu ro dieu nay trong API_CONTRACT.md.
					//
					// MO LAI KHI NAO: neu sau nay doi sang dong model co ho tro temperature
					// (vi du ho gpt-4.1), chi can them lai mot dong:
					//     .temperature(options.temperature())
					//
					// KHONG chong bia dat bang temperature nua. Viec do hien dua hoan toan vao
					// rag.user-storage.allow-general-knowledge=false va PromptBuilderService.
					//
					// ⚠️ KHONG THEM .build() O DAY. ChatClientRequestSpec.options() cua Spring AI 2.0
					// nhan vao mot ChatOptions.Builder<?>, KHONG phai ChatOptions da build xong.
					// Them .build() se gay loi compile:
					//   "inference variable B has incompatible bounds
					//    upper bounds: ChatOptions.Builder<?>  lower bounds: OpenAiChatOptions"
					// (Da mac loi nay mot lan ngay 2026-08-11, ghi lai de khoi lap lai.)
					;
			return builder.build()
					// Bắt đầu tạo một prompt request mới, độc lập với request trước đó.
					.prompt()
					// Gắn model đã resolve cho riêng lần gọi này.
					.options(chatOptions)
					// Toàn bộ RAG prompt được truyền như một user message tới model.
					.user(prompt)
					// Thực hiện synchronous network call tới AI provider.
					.call()
					// Chỉ lấy phần text answer, bỏ metadata provider khỏi response nghiệp vụ.
					.content();
		} catch (RuntimeException ex) {
			// Không lộ chi tiết lỗi/key/provider cho client; giữ exception gốc làm cause để log/debug.
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI service is unavailable", ex);
		}
	}
}
