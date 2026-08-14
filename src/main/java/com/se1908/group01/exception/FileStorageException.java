package com.se1908.group01.exception;

/**
 * [SUA NGAY 2026-08-11 - co ho tro cua AI] Exception moi, sinh ra cung dot chuyen S3 -> Azure Blob.
 *
 * LY DO PHAI CO: truoc day DocumentController va UserProfileController bat thang
 * software.amazon.awssdk...S3Exception. Nghia la kieu rieng cua AWS ro ri len tan tang
 * controller, doi nha cung cap storage la phai sua controller. Gio tang storage nem
 * FileStorageException, controller chi biet den kieu nay - doi Azure sang nha cung cap
 * khac lan sau se khong dung den controller nua.
 *
 * UNCHECKED (ke thua RuntimeException) la co chu y: giu nguyen chu ky cac phuong thuc
 * nghiep vu dang goi, khong bat ca tang service phai khai throws.
 */
public class FileStorageException extends RuntimeException {

	public FileStorageException(String message) {
		super(message);
	}

	public FileStorageException(String message, Throwable cause) {
		super(message, cause);
	}
}
