package com.se1908.group01.service;

import com.se1908.group01.dto.AiGenerationOptions;
import java.util.List;
import org.springframework.ai.chat.messages.Message;

/**
 * [THEM MOI 2026-08-22 - co ho tro cua AI]
 *
 * Viet lai cau hoi follow-up thanh cau hoi DOC LAP truoc khi dem di embed.
 *
 * VAN DE DUOC GIAI QUYET: truoc day ChatSessionServiceImpl lay conversationMemory ra o dau
 * ham sendMessage nhung CHI dung no khi dung prompt gui cho LLM. Chuoi dem di embed van la
 * request.question() tho. Hau qua: tu cau hoi thu hai tro di ("nguoi thu hai phu trach gi?")
 * thi truy hoi mu hoan toan - khong biet "nguoi thu hai" la ai.
 *
 * Loi nay bi CHE GIAU mot phan: cau tra loi cu van nam trong prompt qua conversationMemory
 * (toi da 5 tin nhan - ChatConversationMemoryService.MAX_MEMORY_MESSAGES), nen LLM van co
 * the tra loi dung nho tri nho hoi thoai. Nhung khi cau tra loi cu troi ra khoi cua so 5
 * tin nhan thi no bien mat, ma truy hoi thi chua bao gio mang no ve. Hoi thoai dang muot
 * bong nhien "quen" - va loi kieu nay khong tai hien on dinh nen rat kho bat.
 *
 * Tac hai thu hai, de bi bo qua: danh sach SOURCE tra ve cho frontend duoc lay tu chinh lan
 * tim kiem mu do. Khi LLM tra loi dung nho tri nho, cac trich dan hien ra lai KHONG lien quan
 * gi den cau tra loi. Nguoi dung nhin thay trich dan sai ma trong rat dang tin.
 */
public interface QueryRewriteService {

	/**
	 * Tra ve chuoi nen dung de TIM KIEM. Cau hoi goc khong bi thay doi va van duoc luu vao
	 * lich su cung nhu dua vao prompt - day chi la chuoi phuc vu embedding.
	 *
	 * Khong bao gio nem exception: moi truong hop bat thuong deu rot ve cau hoi goc.
	 *
	 * @param question          cau hoi nguoi dung vua gui
	 * @param conversationMemory lich su gan nhat; rong hoac null nghia la luot dau -> khong goi LLM
	 * @param options           model/temperature dang dung cua session
	 */
	String rewrite(String question, List<Message> conversationMemory, AiGenerationOptions options);
}
