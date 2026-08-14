package com.se1908.group01.service;

import com.se1908.group01.dto.TagRequest;
import com.se1908.group01.dto.TagResponse;
import java.util.List;

public interface TagService {

	TagResponse createTag(TagRequest request);

	List<TagResponse> getMyTags();

	TagResponse updateTag(Long tagId, TagRequest request);

	void deleteTag(Long tagId);

	TagResponse addTagToDocument(Long documentId, Long tagId);

	void removeTagFromDocument(Long documentId, Long tagId);

	List<TagResponse> getDocumentTags(Long documentId);

	List<TagResponse> getPublicDocumentTags(Long documentId);
}
