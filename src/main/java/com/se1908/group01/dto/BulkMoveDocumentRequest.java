package com.se1908.group01.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkMoveDocumentRequest {
	private List<Long> documentIds;
	private Long folderId;
}
