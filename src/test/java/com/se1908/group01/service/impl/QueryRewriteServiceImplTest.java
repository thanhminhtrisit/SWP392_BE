package com.se1908.group01.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.se1908.group01.config.RagProperties;
import com.se1908.group01.dto.AiGenerationOptions;
import com.se1908.group01.enums.SupportedAiModel;
import com.se1908.group01.service.LlmClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * [THEM MOI 2026-08-22 - co ho tro cua AI]
 *
 * Trong tam cua bo test nay KHONG phai "viet lai co hay khong" ma la
 * "co duong nao lam chet luong chat khong". Query rewriting la tinh nang tang cuong;
 * moi nhanh bat thuong deu phai rot ve cau hoi goc.
 */
@ExtendWith(MockitoExtension.class)
class QueryRewriteServiceImplTest {

	private static final String QUESTION = "Người thứ hai phụ trách module gì?";
	private static final AiGenerationOptions OPTIONS =
			new AiGenerationOptions(SupportedAiModel.GPT_5_6_LUNA, 1.0);

	@Mock
	private LlmClient llmClient;

	private RagProperties ragProperties;
	private QueryRewriteServiceImpl service;

	private static List<Message> memory() {
		return List.of(
				new UserMessage("Thành viên nhóm gồm những họ tên nào?"),
				new AssistantMessage("Đồng Thành Minh Trí, Lê Quang Hải, Nguyễn Công Thiên Ân, Trần Thái Dương.")
		);
	}

	@BeforeEach
	void setUp() {
		ragProperties = new RagProperties();
		service = new QueryRewriteServiceImpl(llmClient, ragProperties);
	}

	@Test
	void luotDauTienKhongGoiLlm() {
		// Lich su rong = cau hoi dau tien cua session, khong co gi de tham chieu nguoc.
		// Day la ly do chi phi chi tang ~10% chu khong phai gap doi: bo qua han lan goi LLM.
		var result = service.rewrite(QUESTION, List.of(), OPTIONS);

		assertEquals(QUESTION, result);
		verifyNoInteractions(llmClient);
	}

	@Test
	void memoryNullCungKhongGoiLlm() {
		var result = service.rewrite(QUESTION, null, OPTIONS);

		assertEquals(QUESTION, result);
		verifyNoInteractions(llmClient);
	}

	@Test
	void coTatThiGiuNguyenHanhViCu() {
		ragProperties.getQueryRewrite().setEnabled(false);

		var result = service.rewrite(QUESTION, memory(), OPTIONS);

		assertEquals(QUESTION, result);
		verifyNoInteractions(llmClient);
	}

	@Test
	void vietLaiThanhCongThiTraVeCauDaVietLai() {
		when(llmClient.generateAnswer(anyString(), any()))
				.thenReturn("Lê Quang Hải phụ trách module gì trong nhóm thực hiện tài liệu?");

		var result = service.rewrite(QUESTION, memory(), OPTIONS);

		assertEquals("Lê Quang Hải phụ trách module gì trong nhóm thực hiện tài liệu?", result);
	}

	@Test
	void bocDauNgoacKepVaChiGiuDongDauTien() {
		// Model hay tra ve kem dau ngoac kep hoac them vai dong giai thich du prompt da cam.
		when(llmClient.generateAnswer(anyString(), any()))
				.thenReturn("  \"Lê Quang Hải phụ trách module gì?\"  \nGiải thích: đã thay đại từ.");

		var result = service.rewrite(QUESTION, memory(), OPTIONS);

		assertEquals("Lê Quang Hải phụ trách module gì?", result);
	}

	@Test
	void llmNemExceptionThiRotVeCauHoiGoc() {
		// DAY LA TEST QUAN TRONG NHAT: mot tinh nang phu khong duoc phep lam chet luong chat.
		when(llmClient.generateAnswer(anyString(), any()))
				.thenThrow(new RuntimeException("LLM timeout"));

		var result = service.rewrite(QUESTION, memory(), OPTIONS);

		assertEquals(QUESTION, result);
	}

	@Test
	void llmTraVeChuoiRongThiRotVeCauHoiGoc() {
		when(llmClient.generateAnswer(anyString(), any())).thenReturn("   ");

		assertEquals(QUESTION, service.rewrite(QUESTION, memory(), OPTIONS));
	}

	@Test
	void llmTraVeDoanQuaDaiThiRotVeCauHoiGoc() {
		// Chuoi dai bat thuong nghia la model da giai thich thay vi viet lai. Dem no di embed
		// se lam hong truy hoi, nen tha dung cau goc.
		when(llmClient.generateAnswer(anyString(), any())).thenReturn("x".repeat(401));

		assertEquals(QUESTION, service.rewrite(QUESTION, memory(), OPTIONS));
	}

	@Test
	void chiDuaMaxMemoryMessagesTinNhanCuoiVaoPrompt() {
		ragProperties.getQueryRewrite().setMaxMemoryMessages(2);
		var longMemory = List.<Message>of(
				new UserMessage("TIN-NHAN-CU-NHAT"),
				new UserMessage("tin nhắn giữa"),
				new AssistantMessage("TIN-NHAN-MOI-NHAT")
		);
		when(llmClient.generateAnswer(anyString(), any())).thenReturn("câu đã viết lại");

		service.rewrite(QUESTION, longMemory, OPTIONS);

		var prompt = ArgumentCaptor.forClass(String.class);
		verify(llmClient).generateAnswer(prompt.capture(), any());
		assertFalse(prompt.getValue().contains("TIN-NHAN-CU-NHAT"),
				"Tin nhan cu nhat phai bi cat khoi prompt viet lai");
		assertTrue(prompt.getValue().contains("TIN-NHAN-MOI-NHAT"));
		assertTrue(prompt.getValue().contains(QUESTION),
				"Prompt phai chua cau hoi moi");
	}
}
