package com.se1908.group01.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [SUA NGAY 2026-08-11 - co ho tro cua AI] Lop nay thay cho S3Config (cu).
 *
 * GIU NGUYEN MO HINH BEAN CO DIEU KIEN cua S3Config: bean chi duoc tao khi CA hai gia tri
 * connection-string va container-name deu co noi dung. Nho vay app van khoi dong duoc khi
 * chua cau hinh storage (dung dung hanh vi truoc day voi S3), chi rieng cac API upload/tai
 * file la bao loi khi goi.
 *
 * VI SAO DUNG StringUtils.hasText CHU KHONG PHAI KIEM TRA null: trong .env, viet
 * `TEN_BIEN=` tao ra bien TON TAI voi gia tri chuoi rong va no GHI DE gia tri mac dinh
 * `${TEN_BIEN:mac_dinh}` trong application.yaml. Du an da dinh loi nay mot lan voi
 * GOOGLE_CLIENT_ID. hasText coi ca null lan chuoi rong deu la "chua cau hinh".
 *
 * CHI CAN MOT BEAN, khac S3Config phai tao hai (S3Client de doc/ghi + S3Presigner de ky
 * URL). Ben Azure, BlobClient lay tu container tu no da ky duoc SAS - vi builder dung
 * connection string nen client giu san StorageSharedKeyCredential.
 */
@Configuration
@EnableConfigurationProperties(AzureStorageProperties.class)
public class AzureStorageConfig {

	@Bean
	@ConditionalOnExpression(
			"T(org.springframework.util.StringUtils).hasText('${azure.storage.connection-string:}')"
					+ " and T(org.springframework.util.StringUtils).hasText('${azure.storage.container-name:}')"
	)
	BlobContainerClient blobContainerClient(AzureStorageProperties azureStorageProperties) {
		return new BlobContainerClientBuilder()
				.connectionString(azureStorageProperties.getConnectionString())
				.containerName(azureStorageProperties.getContainerName())
				.buildClient();
	}
}
