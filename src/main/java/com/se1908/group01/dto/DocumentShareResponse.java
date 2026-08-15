package com.se1908.group01.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class DocumentShareResponse {

	private Long documentShareId;
	private Long documentId;
	private Long ownerId;
	private Long sharedWithUserId;
	private String sharedWithEmail;
	private String sharedWithName;
	private Instant createdAt;


}
