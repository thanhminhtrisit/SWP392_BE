package com.se1908.group01.service.impl;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.se1908.group01.config.AzureStorageProperties;
import com.se1908.group01.exception.FileStorageException;
import com.se1908.group01.service.FileStorageService;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * [SUA NGAY 2026-08-11 - co ho tro cua AI] Lop nay thay cho S3StorageServiceImpl (cu).
 *
 * LY DO THAY: du an chuyen tu AWS S3 sang Azure Blob Storage (xem ghi chu trong pom.xml).
 * Anh xa khai niem: bucket -> container, object key -> blob name (giu nguyen chuoi),
 * presigned URL -> SAS token.
 *
 * KHONG CON DAT ACL PRIVATE nhu ban S3: container cua Azure mac dinh da la private,
 * khong co khai niem canned ACL tren tung blob.
 *
 * BlobContainerClient duoc danh dau @Nullable vi bean chi ton tai khi da cau hinh day du
 * (xem AzureStorageConfig). Cac phuong thuc ghi/doc deu fail fast khi thieu, RIENG delete
 * thi im lang tra ve - giu dung su khac biet co chu y cua ban S3 cu, de luong xoa tai lieu
 * trong database khong bi chan chi vi storage chua duoc cau hinh.
 *
 * Luu bytes tai lieu goc vao container private da cau hinh. Upload service giu blob name
 * trong database va tao signed read URL khi can doc file.
 */
@Service
public class AzureBlobStorageServiceImpl implements FileStorageService {

	private final BlobContainerClient blobContainerClient;
	private final AzureStorageProperties azureStorageProperties;

	public AzureBlobStorageServiceImpl(
			@Nullable BlobContainerClient blobContainerClient,
			AzureStorageProperties azureStorageProperties
	) {
		this.blobContainerClient = blobContainerClient;
		this.azureStorageProperties = azureStorageProperties;
	}

	@Override
	public void uploadPrivate(MultipartFile file, String objectKey) throws IOException {
		// Fail fast khi thieu cau hinh storage thay vi tao metadata tai lieu khong hoan chinh.
		var blobClient = requireBlobClient(objectKey);

		// Giu lai content type nhu ban S3 (.contentType tren PutObjectRequest); ben Azure
		// no di qua BlobHttpHeaders. Thieu header nay thi trinh duyet se tai ve dang
		// application/octet-stream thay vi xem truoc duoc PDF/anh.
		var headers = new BlobHttpHeaders();
		if (StringUtils.hasText(file.getContentType())) {
			headers.setContentType(file.getContentType());
		}

		// Stream file tu request truc tiep len Azure, khong doc het vao bo nho.
		try (var in = file.getInputStream()) {
			var options = new BlobParallelUploadOptions(in, file.getSize()).setHeaders(headers);
			blobClient.uploadWithResponse(options, null, Context.NONE);
		} catch (BlobStorageException ex) {
			throw new FileStorageException("Azure Blob upload failed for blob " + objectKey, ex);
		}
	}

	@Override
	public String createPresignedGetUrl(String objectKey, String fileName, String contentType, boolean download) {
		var blobClient = requireBlobClient(objectKey);

		// SAS chi can quyen doc: URL nay duoc gui thang cho trinh duyet cua nguoi dung.
		var permission = new BlobSasPermission().setReadPermission(true);
		var expiry = OffsetDateTime.now().plusMinutes(azureStorageProperties.getSasExpirationMinutes());
		var sasValues = new BlobServiceSasSignatureValues(expiry, permission);

		// Tuong duong responseContentDisposition/responseContentType cua S3 presigned URL:
		// Azure cho ghi de header luc tai bang chinh cac truong nay tren SAS.
		// KHONG DUOC BO: frontend dua vao co `download` de phan biet xem truoc va tai ve.
		sasValues.setContentDisposition(contentDisposition(download, fileName));
		if (StringUtils.hasText(contentType)) {
			sasValues.setContentType(contentType);
		}

		try {
			return blobClient.getBlobUrl() + "?" + blobClient.generateSas(sasValues);
		} catch (BlobStorageException ex) {
			throw new FileStorageException("Azure Blob SAS generation failed for blob " + objectKey, ex);
		}
	}

	@Override
	public void copyObject(String sourceKey, String destinationKey) {
		if (!StringUtils.hasText(destinationKey)) {
			throw new IllegalArgumentException("Destination blob name is required");
		}
		var sourceBlob = requireBlobClient(sourceKey);
		var destinationBlob = blobContainerClient.getBlobClient(destinationKey);

		// Azure khong copy theo ten blob nhu S3 ma doi URL nguon. Vi container la private
		// nen phai kem mot SAS doc ngan han cho blob nguon. Ca hai blob cung mot storage
		// account nen thao tac chay phia server, khong tai du lieu ve client.
		var permission = new BlobSasPermission().setReadPermission(true);
		var sasValues = new BlobServiceSasSignatureValues(OffsetDateTime.now().plusMinutes(5), permission);

		try {
			var sourceUrl = sourceBlob.getBlobUrl() + "?" + sourceBlob.generateSas(sasValues);
			destinationBlob.copyFromUrl(sourceUrl);
		} catch (BlobStorageException ex) {
			throw new FileStorageException(
					"Azure Blob copy failed from " + sourceKey + " to " + destinationKey, ex);
		}
	}

	@Override
	public void delete(String objectKey) {
		// Giu nguyen hanh vi cua ban S3: chua cau hinh storage thi im lang tra ve chu khong
		// nem loi, khac voi ba phuong thuc kia. Day la su khac biet co chu y.
		if (blobContainerClient == null) {
			return;
		}
		if (!StringUtils.hasText(objectKey)) {
			return;
		}
		try {
			// deleteIfExists thay cho deleteObject cua S3: S3 xoa object khong ton tai van
			// tra ve thanh cong, con Azure nem 404. Dung ban IfExists de giu nguyen hanh vi.
			blobContainerClient.getBlobClient(objectKey).deleteIfExists();
		} catch (BlobStorageException ex) {
			throw new FileStorageException("Azure Blob delete failed for blob " + objectKey, ex);
		}
	}

	@Override
	public void downloadToFile(String objectKey, Path target) {
		if (target == null) {
			throw new IllegalArgumentException("Target path is required");
		}
		var blobClient = requireBlobClient(objectKey);
		try {
			// overwrite = true vi ben goi (VideoTranscriptParser) da tao san file tam bang
			// Files.createTempFile, tuc file dich luon ton tai truoc khi tai.
			blobClient.downloadToFile(target.toAbsolutePath().toString(), true);
		} catch (BlobStorageException ex) {
			throw new FileStorageException("Azure Blob download failed for blob " + objectKey, ex);
		}
	}

	/**
	 * Kiem tra cau hinh va tra ve BlobClient cho mot blob name.
	 * Gom lai mot cho vi ca bon phuong thuc ghi/doc deu lap lai dung ba buoc kiem tra nay.
	 */
	private BlobClient requireBlobClient(String objectKey) {
		if (blobContainerClient == null) {
			throw new IllegalStateException(
					"Azure Blob Storage is not configured. "
							+ "Set AZURE_STORAGE_CONNECTION_STRING and AZURE_STORAGE_CONTAINER environment variables.");
		}
		if (!StringUtils.hasText(objectKey)) {
			throw new IllegalArgumentException("Blob name is required");
		}
		return blobContainerClient.getBlobClient(objectKey);
	}

	/**
	 * Giu nguyen logic ma hoa RFC 5987 cua ban S3 cu: ten file tieng Viet co dau khong bi
	 * vo khi trinh duyet tai ve. Chi doi cho gan no vao (SAS thay vi GetObjectRequest).
	 */
	private String contentDisposition(boolean download, String fileName) {
		var disposition = download ? "attachment" : "inline";
		if (!StringUtils.hasText(fileName)) {
			return disposition;
		}
		var encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
		return disposition + "; filename*=UTF-8''" + encodedFileName;
	}
}
