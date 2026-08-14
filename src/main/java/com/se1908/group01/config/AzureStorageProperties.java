package com.se1908.group01.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * [SUA NGAY 2026-08-11 - co ho tro cua AI] Lop nay thay cho S3Properties (cu) va AwsProperties (cu).
 *
 * LY DO GOP HAI LOP THANH MOT: ben AWS phai tach lam hai vi `region` la cau hinh chung cua
 * ca SDK, con bucket la rieng cua S3. Ben Azure thi connection string da chua san
 * endpoint/account/key nen khong con khai niem "region roi rac" nua - chi con mot nhom
 * cau hinh duy nhat, khong ly do gi de giu hai lop.
 *
 * TEN THUOC TINH DOI THEO: bucket-name -> container-name,
 * presigned-url-expiration-minutes -> sas-expiration-minutes (Azure goi la SAS chu khong
 * goi la presigned URL). Rieng key-prefix giu nguyen ten vi y nghia khong doi.
 *
 * KHONG CON `endpoint`: truong do truoc day de tro ve LocalStack/MinIO khi test S3 offline,
 * du an chua bao gio dung den (AWS_S3_ENDPOINT luon rong). Muon tro ve Azurite thi sua
 * thang connection string, khong can them truong.
 */
@ConfigurationProperties(prefix = "azure.storage")
public class AzureStorageProperties {

	/**
	 * Connection string cua storage account, thuong lay tu bien moi truong
	 * AZURE_STORAGE_CONNECTION_STRING. Tuong duong mat khau toan quyen - khong log ra.
	 */
	private String connectionString;

	/**
	 * Ten container chua blob (tuong duong bucket ben S3). Env AZURE_STORAGE_CONTAINER.
	 */
	private String containerName;

	/**
	 * Tien to tuy chon cho ten blob, vi du "documents/".
	 */
	private String keyPrefix = "";

	/**
	 * Thoi han hieu luc cua SAS token, tinh bang phut.
	 */
	private long sasExpirationMinutes = 10;

	public String getConnectionString() {
		return connectionString;
	}

	public void setConnectionString(String connectionString) {
		this.connectionString = connectionString;
	}

	public String getContainerName() {
		return containerName;
	}

	public void setContainerName(String containerName) {
		this.containerName = containerName;
	}

	public String getKeyPrefix() {
		return keyPrefix;
	}

	public void setKeyPrefix(String keyPrefix) {
		this.keyPrefix = keyPrefix == null ? "" : keyPrefix.trim();
	}

	public long getSasExpirationMinutes() {
		return sasExpirationMinutes;
	}

	public void setSasExpirationMinutes(long sasExpirationMinutes) {
		this.sasExpirationMinutes = sasExpirationMinutes;
	}
}
