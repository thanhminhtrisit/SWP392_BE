package com.se1908.group01.enums;

import java.util.Arrays;

/**
 * Danh sach model chat ma API cho phep client chon.
 *
 * [SUA NGAY 2026-08-11 - co ho tro cua AI] Chuyen tu Google Gemini sang OpenAI.
 *
 * THAY DOI VE KIEU DU LIEU: truoc day {@code providerModel} co kieu
 * {@code GoogleGenAiChatModel.ChatModel} - mot enum rieng cua thu vien Google. Nay doi
 * thanh {@link String} thuan. Ly do: Spring AI nhan ten model duoi dang chuoi, khong bat
 * buoc dung enum cua nha cung cap. Bo phu thuoc do thi lan sau doi provider nua se khong
 * phai sua file nay.
 *
 * Van giu hai truong rieng biet (apiName va providerModel) du hien tai chung trung gia
 * tri, de sau nay muon dat ten than thien cho API ma van gui dung ID that len provider.
 *
 * ⚠️ DAY LA HOP DONG API: apiName la chuoi ma frontend gui len truong "model". Doi danh
 * sach nay la doi hop dong, phai bao cho nguoi lam frontend va cap nhat API_CONTRACT.md.
 */
public enum SupportedAiModel {

	/** Toi uu chi phi - re nhat, du dung cho RAG vi model chu yeu dien dat lai tai lieu. */
	GPT_5_6_LUNA("gpt-5.6-luna", "gpt-5.6-luna"),

	/** Can bang giua nang luc va chi phi. */
	GPT_5_6_TERRA("gpt-5.6-terra", "gpt-5.6-terra"),

	/** Manh nhat, danh cho cong viec phuc tap. Dat nhat. */
	GPT_5_6_SOL("gpt-5.6-sol", "gpt-5.6-sol");

	private final String apiName;
	private final String providerModel;

	SupportedAiModel(String apiName, String providerModel) {
		this.apiName = apiName;
		this.providerModel = providerModel;
	}

	public String getApiName() {
		return apiName;
	}

	public String getProviderModel() {
		return providerModel;
	}

	public static SupportedAiModel fromApiName(String apiName) {
		return Arrays.stream(values())
				.filter(model -> model.apiName.equals(apiName))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException(
						"Unsupported AI model. Allowed models: "
								+ String.join(", ", allowedApiNames())
				));
	}

	public static String[] allowedApiNames() {
		return Arrays.stream(values())
				.map(SupportedAiModel::getApiName)
				.toArray(String[]::new);
	}
}
