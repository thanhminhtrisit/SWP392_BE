package com.se1908.group01.enums;

/** Các lifecycle state trả về UI trong khi ingestion tài liệu chạy bất đồng bộ. */
public enum DocumentStatus {
	UPLOADED,
	PARSING,
	INDEXING,
	READY,
	FAILED
}

