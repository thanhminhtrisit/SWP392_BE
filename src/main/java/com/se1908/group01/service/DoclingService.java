package com.se1908.group01.service;

import com.se1908.group01.dto.ChunkData;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface DoclingService {

	boolean supports(MultipartFile file);

	boolean isFallbackEnabled();

	List<ChunkData> chunk(MultipartFile file);
}
