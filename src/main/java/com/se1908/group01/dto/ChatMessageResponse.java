package com.se1908.group01.dto;

import com.se1908.group01.enums.ChatMessageRole;
import com.se1908.group01.enums.ChatMessageStatus;
import java.time.Instant;
import java.util.List;

/** Response của session message, gồm nội dung assistant và source citation đã lưu. */
public record ChatMessageResponse(
		Long messageId,
		ChatMessageRole role,
		String content,
		ChatMessageStatus status,
		Instant createdAt,
		List<ChatMessageSourceResponse> sources,

		// [THEM MOI 2026-08-22 - co ho tro cua AI] Chuoi THUC SU duoc dem di tim kiem.
		//
		// VI SAO CAN: khong co truong nay thi khong the biet query rewriting co chay hay
		// khong, va chay dung hay sai. Log server thi FE va Postman deu khong nhin thay.
		//
		// GIA TRI:
		//   - null  : khi doc lai lich su tin nhan (GET), vi luc do khong con khai niem
		//             "cau vua duoc tim kiem" - day la ly do truong nay nullable.
		//   - == cau hoi goc : khong viet lai (luot dau session, co tat, hoac model thay
		//             cau hoi da tu du nghia).
		//   - != cau hoi goc : da viet lai. Doi chieu voi cau goc de danh gia chat luong.
		String rewrittenQuestion
) {
}
