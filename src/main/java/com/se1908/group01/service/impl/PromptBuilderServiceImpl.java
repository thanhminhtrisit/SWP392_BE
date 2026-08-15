package com.se1908.group01.service.impl;

import com.se1908.group01.dto.RetrievedChunk;
import com.se1908.group01.enums.ChatMode;
import com.se1908.group01.enums.KnowledgePolicy;
import com.se1908.group01.service.PromptBuilderService;
import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

@Service
/**
 * Đóng gói question và các chunk được truy xuất thành prompt cho model AI.
 * Prompt single-document ép model chỉ trả lời từ context của document được chọn.
 */
public class PromptBuilderServiceImpl implements PromptBuilderService {

	private static final int MAX_CHUNK_CHARACTERS = 2000;
	private static final String INTERNAL_METADATA_RULE = "Never mention or expose internal retrieval metadata such as document IDs, chunk IDs, chunk indexes, page metadata, or bracketed context labels. Answer naturally without referencing those internal identifiers.";

	@Override
	public String buildDocumentQuestionPrompt(String question, List<RetrievedChunk> chunks) {
		// Tạo prompt stateless cho một document, giữ metadata chunk/page để FE có thể hiển thị citation.
		var prompt = new StringBuilder();
		prompt.append("""
				You are an AI study assistant.
				Answer the user's question using only the provided document context.
				If the answer is not present in the context, say: "The answer cannot be found in the selected document."
				Do not use outside knowledge.
				Do not invent citations, facts, or document content.
				Never mention or expose internal retrieval metadata such as document IDs, chunk IDs, chunk indexes, page metadata, or bracketed context labels.

				Document context:
				""");

		// Ghép từng chunk theo thứ tự similarity; nội dung quá dài được cắt ở truncate().
		for (int i = 0; i < chunks.size(); i++) {
			var retrieved = chunks.get(i);
			var chunk = retrieved.getChunk();
			prompt.append("\n[Chunk ")
					.append(i + 1)
					.append(" | chunkIndex=")
					.append(chunk.getChunkIndex());
			if (chunk.getPageNumber() != null) {
				prompt.append(" | page=").append(chunk.getPageNumber());
			}
			prompt.append("]\n")
					.append(truncate(chunk.getContent()))
					.append("\n");
		}

		prompt.append("\nUser question:\n")
				.append(question);

		return prompt.toString();
	}

	@Override
	public String buildMultiDocumentQuestionPrompt(ChatMode mode, String context, String question) {

		var systemMessage = resolveSystemMessage(mode, KnowledgePolicy.DOCUMENTS_ONLY);

		return "[SYSTEM]\n"
				+ systemMessage
				+ "\n\n" + INTERNAL_METADATA_RULE
				+ "\n\n[USER]\n"
				+ "CONTEXT:\n"
				+ "-----------\n"
				+ context
				+ "\n-----------\n"
				+ "QUESTION:\n"
				+ question;
	}

	@Override
	public String buildSessionQuestionPrompt(
			ChatMode mode,
			KnowledgePolicy knowledgePolicy,
			String context,
			List<Message> conversationMemory,
			String question
	) {
		// Session prompt thêm tối đa phần conversation memory gần đây nhưng vẫn yêu cầu fact phải có trong document context.
		var prompt = new StringBuilder();
		prompt.append("[SYSTEM]\n")
				.append(resolveSystemMessage(mode, knowledgePolicy))
				.append("\n\n").append(INTERNAL_METADATA_RULE)
				.append("\nConversation history is provided only to understand follow-up questions. ")
				.append("All factual claims must still be supported by the document context.\n\n");

		if (conversationMemory != null && !conversationMemory.isEmpty()) {
			// Lịch sử chỉ giúp hiểu câu hỏi follow-up, không được thay thế nguồn dữ liệu của document.
			prompt.append("[CONVERSATION HISTORY - LAST ")
					.append(conversationMemory.size())
					.append(" MESSAGES]\n");
			for (var message : conversationMemory) {
				prompt.append("[")
						.append(message.getMessageType().name())
						.append("] ")
						.append(message.getText())
						.append("\n");
			}
			prompt.append("\n");
		}

		return prompt.append("[DOCUMENT CONTEXT]\n")
				.append("-----------\n")
				.append(context)
				.append("\n-----------\n")
				.append("[CURRENT QUESTION]\n")
				.append(question)
				.toString();
	}

	private String resolveSystemMessage(ChatMode mode, KnowledgePolicy policy) {
		if (mode == ChatMode.SELECTED_DOCUMENTS) {
			// SelectedDocuments, bao gồm single-document, luôn bị ép về policy DOCUMENTS_ONLY.
			return """
					You are an AI study assistant.
					Answer the user's question using ONLY the information contained in the provided context from the documents explicitly selected by the user.

					Rules:
					- You MUST NOT use any knowledge that is not explicitly present in the context.
					- If the selected documents do not contain enough information to answer the question, you MUST say that the information is not available in the selected documents and that you cannot answer.
					- You MUST NOT rely on your own pretrained or external knowledge outside of what is written in the context, even if you believe it to be correct.
					- You MUST NOT invent or guess facts.
					- You MUST NOT claim that information comes from the selected documents if it is not clearly stated in the context.

					When answering:
					- Be concise and precise.
					- Base every statement strictly on the provided context from the selected documents.
					- If something is not in the context, treat it as unknown and say that the selected documents do not contain that information.""";
		}
		// USER_STORAGE — split by policy; nhánh này không phải luồng single-document chính.
		if (policy == KnowledgePolicy.DOCUMENTS_PLUS_GENERAL) {
			return """
					You are an assistant that answers questions using ONLY the information contained in the provided context.
					The context may include:
					- the user's private stored documents, and
					- public documents available in the system.

					Rules:
					- You MUST NOT use any knowledge that is not explicitly present in the context.
					- If the context does not contain enough information to answer the question, you MUST say that the information is not available in the documents and that you cannot answer.
					- You MUST NOT rely on your own pretrained or external knowledge outside of what is written in the context, even if you believe it to be correct.
					- You MUST NOT invent or guess facts.
					- You MUST NOT claim that information comes from the documents if it is not clearly stated in the context.
					- If the context mixes private and public documents, do not reveal which parts are private versus public; simply refer to them as "the provided documents" or "the provided context".

					When answering:
					- Be concise and precise.
					- Base every statement strictly on the provided context.
					- If something is not in the context, treat it as unknown and say that the documents do not contain that information.""";
		}
		// USER_STORAGE + DOCUMENTS_ONLY; đây không phải nhánh single-document chính.
		return """
				You are an AI study assistant.
				Answer the user's question using ONLY the information contained in the provided context from the user's stored documents.

				Rules:
				- You MUST NOT use any knowledge that is not explicitly present in the context.
				- If the context does not contain enough information to answer the question, you MUST say that the information is not available in the user's documents and that you cannot answer.
				- You MUST NOT rely on your own pretrained or external knowledge outside of what is written in the context, even if you believe it to be correct.
				- You MUST NOT invent or guess facts.
				- You MUST NOT claim that information comes from the documents if it is not clearly stated in the context.

				When answering:
				- Be concise and precise.
				- Base every statement strictly on the provided context.
				- If something is not in the context, treat it as unknown and say that the documents do not contain that information.""";
	}

	private String truncate(String content) {
		if (content == null || content.length() <= MAX_CHUNK_CHARACTERS) {
			return content;
		}
		return content.substring(0, MAX_CHUNK_CHARACTERS);
	}
}
