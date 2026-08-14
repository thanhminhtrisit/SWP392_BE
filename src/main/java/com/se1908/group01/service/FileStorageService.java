package com.se1908.group01.service;

import java.io.IOException;
import java.nio.file.Path;
import org.springframework.web.multipart.MultipartFile;

/**
 * [SUA NGAY 2026-08-11 - co ho tro cua AI] Doi ten tu S3StorageService thanh FileStorageService.
 *
 * LY DO DOI TEN: interface nay von da la abstraction khong phu thuoc nha cung cap (khong
 * co kieu AWS nao trong chu ky), chi moi cai TEN la con dinh S3. Khi ban cai da chuyen
 * sang Azure Blob thi giu ten cu se gay hieu nham cho nguoi doc sau.
 *
 * TEN PHUONG THUC GIU NGUYEN (uploadPrivate / createPresignedGetUrl / copyObject / delete)
 * de khong phai sua rai rac tang nghiep vu. "objectKey" ben Azure chinh la blob name -
 * cung la mot chuoi, nhet vao vua khit, nen cot database s3_key / avatar_s3_key cung
 * khong can doi.
 *
 * Abstraction cho private object storage dung de luu file goc da upload.
 */
public interface FileStorageService {

	/** Upload bytes goc ma khong public object trong container. */
	void uploadPrivate(MultipartFile file, String objectKey) throws IOException;

	/**
	 * Sinh URL doc co thoi han cho mot object.
	 *
	 * @param download true = ep trinh duyet tai ve (Content-Disposition: attachment),
	 *                 false = xem truoc ngay trong trinh duyet (inline).
	 */
	String createPresignedGetUrl(String objectKey, String fileName, String contentType, boolean download);

	void copyObject(String sourceKey, String destinationKey);

	void delete(String objectKey);

	/**
	 * [SUA NGAY 2026-08-11 - co ho tro cua AI] Phuong thuc MOI, them cung dot chuyen sang Azure.
	 *
	 * LY DO THEM: VideoTranscriptParser truoc day inject thang S3Client roi tu goi
	 * getObjectAsBytes(...) de tai video ve dia tam cho FFmpeg - tuc la di vong qua
	 * interface nay va keo SDK cua nha cung cap vao tang service. Dua viec tai file xuong
	 * day thi khong con cho nao ngoai tang storage biet den SDK.
	 *
	 * Ghi de len file dich neu no da ton tai (goi ben dang tao san file tam bang
	 * Files.createTempFile nen file luon ton tai truoc khi tai).
	 */
	void downloadToFile(String objectKey, Path target);
}
