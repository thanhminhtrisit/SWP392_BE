package com.se1908.group01.service.impl;

import com.se1908.group01.config.AzureStorageProperties;
import com.se1908.group01.dto.DocumentPageResponse;
import com.se1908.group01.dto.DocumentShareLinkResponse;
import com.se1908.group01.dto.DocumentShareResponse;
import com.se1908.group01.dto.DocumentUploadResponse;
import com.se1908.group01.dto.FileAccessUrlResponse;
import com.se1908.group01.entity.Document;
import com.se1908.group01.entity.DocumentChunk;
import com.se1908.group01.entity.DocumentShare;
import com.se1908.group01.entity.DocumentShareLink;
import com.se1908.group01.entity.DocumentStatus;
import com.se1908.group01.entity.User;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.ChatSessionDocumentRepository;
import com.se1908.group01.repository.DocumentChunkRepository;
import com.se1908.group01.repository.DocumentFolderRepository;
import com.se1908.group01.repository.DocumentRepository;
import com.se1908.group01.repository.DocumentShareLinkRepository;
import com.se1908.group01.repository.DocumentShareRepository;
import com.se1908.group01.repository.DocumentTagRepository;
import com.se1908.group01.repository.FriendshipRepository;
import com.se1908.group01.repository.UserRepository;
import com.se1908.group01.service.*;
import com.se1908.group01.util.FilenameSanitizer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;


@Service
/**
 * Cài đặt transaction upload tài liệu và điều phối storage với indexing bất đồng bộ.
 * Method đưa file vào Azure Blob private trước, sau đó lưu metadata database và bản copy tạm cho ingestion.
 * [SUA NGAY 2026-08-11 - co ho tro cua AI] Sửa chữ "S3" -> "Azure Blob" trong các comment của lớp này.
 * LÝ DO: sau khi chuyển nhà cung cấp, các mô tả cũ nói sai về nơi file thực sự nằm. Chỉ sửa chữ, không sửa logic.
 */
public class DocumentServiceImpl implements DocumentService {

	private static final int MAX_PAGE_SIZE = 100;

	private final FileValidationService fileValidationService;
	/*
	 * [SUA NGAY 2026-08-11 - co ho tro cua AI] Doi kieu S3StorageService -> FileStorageService
	 * va S3Properties -> AzureStorageProperties (kem ten bien) sau khi chuyen phan luu tru
	 * tu AWS S3 sang Azure Blob Storage.
	 *
	 * LY DO CHI DOI TEN, KHONG DOI LOGIC: lop nay von chi goi qua interface, khong dung
	 * kieu nao cua AWS SDK. Cac phuong thuc uploadPrivate / createPresignedGetUrl /
	 * copyObject / delete giu nguyen chu ky va ngu nghia nen luong nghiep vu khong doi.
	 *
	 * CAC GETTER doc.getS3Key() / doc.setS3Key() VAN GIU NGUYEN TEN: cot database van la
	 * s3_key. Blob name cua Azure cung chi la mot chuoi dinh danh nen nhet vao vua khit;
	 * doi ten cot se keo theo migration + entity + DTO, khong dang.
	 */
	private final FileStorageService fileStorageService;
	private final AzureStorageProperties azureStorageProperties;
	private final DocumentRepository documentRepository;
	private final DocumentFolderRepository documentFolderRepository;
	private final DocumentChunkRepository documentChunkRepository;
	private final DocumentTagRepository documentTagRepository;
	private final DocumentIngestionService documentIngestionService;
	private final DocumentShareLinkRepository documentShareLinkRepository;
	private final DocumentShareRepository documentShareRepository;
	private final FriendshipRepository friendshipRepository;
	private final UserRepository userRepository;
	private final DocumentIngestionJobService documentIngestionJobService;
	private final CurrentUserService currentUserService;
	private final ChatSessionDocumentRepository chatSessionDocumentRepository;
	private final SubscriptionEntitlementService subscriptionEntitlementService;

	public DocumentServiceImpl(
			FileValidationService fileValidationService,
			FileStorageService fileStorageService,
			AzureStorageProperties azureStorageProperties,
			DocumentRepository documentRepository,
			DocumentFolderRepository documentFolderRepository,
			DocumentChunkRepository documentChunkRepository,
			DocumentTagRepository documentTagRepository,
			DocumentIngestionService documentIngestionService,
			DocumentShareLinkRepository documentShareLinkRepository,
			DocumentShareRepository documentShareRepository,
			FriendshipRepository friendshipRepository,
			UserRepository userRepository,
			DocumentIngestionJobService documentIngestionJobService,
			CurrentUserService currentUserService,
			ChatSessionDocumentRepository chatSessionDocumentRepository,
			SubscriptionEntitlementService subscriptionEntitlementService
	) {
		this.fileValidationService = fileValidationService;
		this.fileStorageService = fileStorageService;
		this.azureStorageProperties = azureStorageProperties;
		this.documentRepository = documentRepository;
		this.documentFolderRepository = documentFolderRepository;
		this.documentChunkRepository = documentChunkRepository;
		this.documentTagRepository = documentTagRepository;
		this.documentIngestionService = documentIngestionService;
		this.documentShareLinkRepository = documentShareLinkRepository;
		this.documentShareRepository = documentShareRepository;
		this.friendshipRepository = friendshipRepository;
		this.userRepository = userRepository;
		this.documentIngestionJobService = documentIngestionJobService;
		this.currentUserService = currentUserService;
		this.chatSessionDocumentRepository = chatSessionDocumentRepository;
		this.subscriptionEntitlementService = subscriptionEntitlementService;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	/**
	 * Upload một tài liệu cho user đã xác thực.
	 * Business flow: lấy plan active -> validate file và entitlement -> upload file storage -> lưu metadata -> schedule ingestion.
	 */
	public DocumentUploadResponse upload(MultipartFile file, Boolean isPublic, Long folderId) throws IOException {
		// Xác định owner đã xác thực trước khi áp dụng rule upload theo plan.
		var userId = currentUserService.getCurrentUserId();
		var activePlan = subscriptionEntitlementService.getActivePlan(userId);
		// Enforce type và size từng file trước khi tạo side effect với external storage.
		fileValidationService.validateForUpload(file, activePlan.getMaxUploadSizeMb());
		// Enforce quyền video và storage quota tổng của subscription active.
		subscriptionEntitlementService.enforceUploadEntitlements(
				userId,
				file,
				activePlan,
				fileValidationService.isVideo(file)
		);
		if (folderId != null) {
			documentFolderRepository.findByFolderIdAndUserId(folderId, userId)
					.orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
		}

		var originalName = FilenameSanitizer.sanitize(file.getOriginalFilename());
		var key = buildObjectKey(userId, originalName);

		// Lưu file gốc ở chế độ private trong Azure Blob; database chỉ lưu metadata và object key.
		fileStorageService.uploadPrivate(file, key);

		Document doc;
		Path ingestionFile = null;
		try {
			// Tạo record database ở state UPLOADED trước khi parsing bất đồng bộ bắt đầu.
			doc = new Document();
			doc.setUserId(userId);
			doc.setFolderId(folderId);
			doc.setOriginalFileName(originalName);
			doc.setS3Key(key);
			doc.setContentType(file.getContentType());
			doc.setFileSize(file.getSize());
			doc.setIsPublic(Boolean.TRUE.equals(isPublic));
			doc.setStatus(DocumentStatus.UPLOADED);

			doc = documentRepository.save(doc);
			// MultipartFile chỉ thuộc request, nên copy xuống disk cho async ingestion worker.
			ingestionFile = documentIngestionJobService.copyToTempFile(file);
			// Chỉ bắt đầu ingestion sau khi transaction metadata commit thành công.
			registerIngestionAfterCommit(doc.getDocumentId(), ingestionFile, originalName, file.getContentType());
		} catch (RuntimeException | IOException ex) {
			// Xóa blob và temp file nếu setup metadata thất bại sau khi upload.
			try {
				fileStorageService.delete(key);
			} catch (RuntimeException ignored) {
			}
			deleteTempFileQuietly(ingestionFile);
			throw ex;
		}

		return toResponse(doc);
	}

	@Transactional
	@Override
	public DocumentUploadResponse moveToTrash(Long documentId) {
		var userId = currentUserService.getCurrentUserId();
		var doc = findOwnedDocument(userId, documentId);
		if (!Boolean.TRUE.equals(doc.getIsDeleted())) {
			doc.setIsDeleted(Boolean.TRUE);
			doc.setDeletedAt(Instant.now());
			doc = documentRepository.save(doc);
		}
		return toResponse(doc);
	}

	@Transactional(readOnly = true)
	@Override
	public List<DocumentUploadResponse> getMyDocuments() {
		var userId = currentUserService.getCurrentUserId();
		return documentRepository.findByUserIdAndIsDeletedFalseOrderByUploadedAtDesc(userId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	@Override
	public List<DocumentUploadResponse> getStarredDocuments() {
		var userId = currentUserService.getCurrentUserId();
		return documentRepository.findByUserIdAndIsStarredTrueAndIsDeletedFalseOrderByUploadedAtDesc(userId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	@Override
	public DocumentUploadResponse getDocumentDetail(Long documentId) {
		var userId = currentUserService.getCurrentUserId();
		return toResponse(findOwnedActiveDocument(userId, documentId));
	}

	@Transactional
	@Override
	public DocumentUploadResponse renameDocument(Long documentId, String originalFileName) {
		var userId = currentUserService.getCurrentUserId();
		var doc = findOwnedActiveDocument(userId, documentId);
		doc.setOriginalFileName(normalizeOriginalFileName(originalFileName));
		return toResponse(documentRepository.save(doc));
	}

	@Transactional
	@Override
	public DocumentUploadResponse moveDocumentToFolder(Long documentId, Long folderId) {
		var userId = currentUserService.getCurrentUserId();
		var doc = findOwnedActiveDocument(userId, documentId);
		if (folderId != null) {
			// Document chỉ được đưa vào folder thuộc cùng user đã xác thực.
			documentFolderRepository.findByFolderIdAndUserId(folderId, userId)
					.orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
		}
		doc.setFolderId(folderId);
		return toResponse(documentRepository.save(doc));
	}

	@Transactional(readOnly = true)
	@Override
	public FileAccessUrlResponse getPreviewUrl(Long documentId) {
		var userId = currentUserService.getCurrentUserId();
		return toFileAccessUrlResponse(findOwnedActiveDocument(userId, documentId), false);
	}

	@Transactional(readOnly = true)
	@Override
	public FileAccessUrlResponse getDownloadUrl(Long documentId) {
		var userId = currentUserService.getCurrentUserId();
		return toFileAccessUrlResponse(findOwnedActiveDocument(userId, documentId), true);
	}

	@Transactional(readOnly = true)
	@Override
	public List<DocumentUploadResponse> getPublicDocuments() {
		return documentRepository.findByIsPublicTrueAndIsDeletedFalseOrderByUploadedAtDesc()
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	@Override
	public DocumentUploadResponse getPublicDocumentDetail(Long documentId) {
		if (documentId == null) {
			throw new IllegalArgumentException("documentId is required");
		}
		return documentRepository.findByDocumentIdAndIsPublicTrueAndIsDeletedFalse(documentId)
				.map(this::toResponse)
				.orElseThrow(() -> new ResourceNotFoundException("Public document not found"));
	}

	@Transactional(readOnly = true)
	@Override
	public FileAccessUrlResponse getPublicPreviewUrl(Long documentId) {
		return toFileAccessUrlResponse(findPublicActiveDocument(documentId), false);
	}

	@Transactional(readOnly = true)
	@Override
	public FileAccessUrlResponse getPublicDownloadUrl(Long documentId) {
		return toFileAccessUrlResponse(findPublicActiveDocument(documentId), true);
	}

	/**
	 * Lưu một tài liệu công khai từ Community về kho lưu trữ cá nhân (My Files) của người dùng hiện tại.
	 * <p>
	 * Quy tắc nghiệp vụ kiểm tra bên trong:
	 * <ul>
	 *   <li>Xác thực người dùng hiện tại từ phiên đăng nhập JWT.</li>
	 *   <li>Tìm kiếm tài liệu công khai theo {@code documentId} (yêu cầu tài liệu tồn tại, {@code isPublic = true} và chưa bị xóa).</li>
	 *   <li>Ngăn chặn tác giả tự lưu tài liệu của chính mình (ném ra {@link IllegalArgumentException} theo Lựa chọn A).</li>
	 *   <li>Nếu {@code folderId} được cung cấp, xác thực thư mục thuộc về người dùng hiện tại.</li>
	 *   <li>Kiểm tra hạn ngạch dung lượng lưu trữ của người dùng theo gói Subscription active (ném ra {@link IllegalArgumentException} nếu vượt quá).</li>
	 *   <li>Thực hiện sao chép file object trực tiếp trên Azure Blob Storage với object key mới độc lập.</li>
	 *   <li>Tạo mới bản ghi {@link Document} ở trạng thái riêng tư ({@code isPublic = false}).</li>
	 *   <li>Nhân bản các bản ghi {@link DocumentChunk} từ tài liệu gốc sang tài liệu mới để hỗ trợ Chat AI tức thì.</li>
	 * </ul>
	 *
	 * @param documentId ID của tài liệu công khai trên Community
	 * @param folderId   ID của thư mục đích trong My Files (tùy chọn, có thể null)
	 * @return một đối tượng DTO {@link DocumentUploadResponse} chứa thông tin tài liệu mới được tạo trong My Files
	 * @throws ResourceNotFoundException nếu không tìm thấy tài liệu công khai hoặc thư mục đích
	 * @throws IllegalArgumentException  nếu người dùng tự lưu tài liệu của chính mình hoặc vượt quá dung lượng lưu trữ cho phép
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public DocumentUploadResponse savePublicDocumentToMyFiles(Long documentId, Long folderId) {
		var userId = currentUserService.getCurrentUserId();
		var sourceDoc = findPublicActiveDocument(documentId);

		if (sourceDoc.getUserId().equals(userId)) {
			throw new IllegalArgumentException("You already own this document in My Files");
		}

		var originalName = FilenameSanitizer.sanitize(sourceDoc.getOriginalFileName());
		boolean alreadySaved = documentRepository.existsByUserIdAndOriginalFileNameAndFileSizeAndIsDeletedFalse(
				userId,
				originalName,
				sourceDoc.getFileSize()
		);
		if (alreadySaved) {
			throw new IllegalArgumentException("You have already saved this document to My Files");
		}

		if (folderId != null) {
			documentFolderRepository.findByFolderIdAndUserId(folderId, userId)
					.orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
		}

		var activePlan = subscriptionEntitlementService.getActivePlan(userId);
		var currentUsageBytes = documentRepository.sumActiveStorageBytesByUserId(userId);
		long newTotalBytes = currentUsageBytes + (sourceDoc.getFileSize() != null ? sourceDoc.getFileSize() : 0L);
		long maxStorageBytes = activePlan.getStorageLimitGb() * 1024L * 1024L * 1024L;
		if (newTotalBytes > maxStorageBytes) {
			throw new IllegalArgumentException("Storage limit exceeded for your active plan");
		}

		var destinationKey = buildObjectKey(userId, originalName);

		fileStorageService.copyObject(sourceDoc.getS3Key(), destinationKey);

		var newDoc = new Document();
		newDoc.setUserId(userId);
		newDoc.setFolderId(folderId);
		newDoc.setOriginalFileName(originalName);
		newDoc.setS3Key(destinationKey);
		newDoc.setContentType(sourceDoc.getContentType());
		newDoc.setFileSize(sourceDoc.getFileSize());
		newDoc.setIsPublic(Boolean.FALSE);
		newDoc.setIsStarred(Boolean.FALSE);
		newDoc.setIsDeleted(Boolean.FALSE);
		newDoc.setStatus(sourceDoc.getStatus() != null ? sourceDoc.getStatus() : DocumentStatus.READY);

		var savedDoc = documentRepository.save(newDoc);

		var chunks = documentChunkRepository.findByDocumentDocumentIdOrderByChunkIndexAsc(sourceDoc.getDocumentId());
		if (chunks != null && !chunks.isEmpty()) {
			var newChunks = chunks.stream().map(chunk -> {
				var newChunk = new DocumentChunk();
				newChunk.setDocument(savedDoc);
				newChunk.setChunkIndex(chunk.getChunkIndex());
				newChunk.setContent(chunk.getContent());
				newChunk.setPageNumber(chunk.getPageNumber());
				newChunk.setEmbeddingVector(chunk.getEmbeddingVector());
				return newChunk;
			}).toList();
			documentChunkRepository.saveAll(newChunks);
		}

		return toResponse(savedDoc);
	}

	/**
	 * Tạo một liên kết chia sẻ (share link) mới cho tài liệu được chỉ định.
	 * <p>
	 * Phương thức này thực hiện xác thực quyền sở hữu của người dùng đối với tài liệu gốc.
	 * Nếu tài liệu đã có một liên kết chia sẻ đang hoạt động và chưa hết hạn, hệ thống sẽ trả về luôn liên kết đó.
	 * Ngược lại, nếu liên kết cũ đã hết hạn, hệ thống sẽ vô hiệu hóa liên kết cũ và tạo ra một liên kết chia sẻ mới
	 * với mã token ngẫu nhiên và duy nhất.
	 *
	 * @param documentId ID của tài liệu cần tạo liên kết chia sẻ
	 * @return một đối tượng {@link DocumentShareLinkResponse} chứa thông tin chi tiết của liên kết chia sẻ vừa tạo
	 * @throws ResourceNotFoundException nếu không tìm thấy tài liệu đang hoạt động hoặc người dùng không sở hữu tài liệu đó
	 */
	@Transactional
	@Override
	public DocumentShareLinkResponse createShareLink(Long documentId) {
		var userId = currentUserService.getCurrentUserId();
		var doc = findOwnedActiveDocument(userId, documentId);

		var existingLink = documentShareLinkRepository
				.findFirstByDocument_DocumentIdAndOwnerIdAndEnabledTrueOrderByCreatedAtDesc(documentId, userId);
		if (existingLink.isPresent() && !isExpired(existingLink.get())) {
			return toShareLinkResponse(existingLink.get());
		}
		existingLink.ifPresent(link -> {
			link.setEnabled(Boolean.FALSE);
			documentShareLinkRepository.save(link);
		});

		var shareLink = new DocumentShareLink();
		shareLink.setDocument(doc);
		shareLink.setOwnerId(userId);
		shareLink.setToken(generateShareToken());
		shareLink.setEnabled(Boolean.TRUE);

		return toShareLinkResponse(documentShareLinkRepository.save(shareLink));
	}

	/**
	 * Vô hiệu hóa (tắt) liên kết chia sẻ đang hoạt động của tài liệu.
	 * <p>
	 * Phương thức này kiểm tra xem người dùng hiện tại có sở hữu tài liệu hay không, tìm liên kết chia sẻ đang hoạt động
	 * và cập nhật trạng thái của liên kết đó thành vô hiệu hóa (enabled = false).
	 *
	 * @param documentId ID của tài liệu cần vô hiệu hóa liên kết chia sẻ
	 * @return đối tượng {@link DocumentShareLinkResponse} chứa thông tin liên kết chia sẻ sau khi đã bị vô hiệu hóa
	 * @throws ResourceNotFoundException nếu không tìm thấy liên kết chia sẻ đang hoạt động của tài liệu
	 */
	@Transactional
	@Override
	public DocumentShareLinkResponse disableShareLink(Long documentId) {
		var userId = currentUserService.getCurrentUserId();
		findOwnedActiveDocument(userId, documentId);

		var shareLink = documentShareLinkRepository
				.findFirstByDocument_DocumentIdAndOwnerIdAndEnabledTrueOrderByCreatedAtDesc(documentId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Share link not found"));
		shareLink.setEnabled(Boolean.FALSE);

		return toShareLinkResponse(documentShareLinkRepository.save(shareLink));
	}

	/**
	 * Lấy thông tin chi tiết của tài liệu thông qua mã token của liên kết chia sẻ.
	 * <p>
	 * Phương thức này thực hiện tìm kiếm tài liệu từ token, kiểm tra xem liên kết chia sẻ có hợp lệ
	 * (chưa hết hạn, đang kích hoạt) và tài liệu gốc chưa bị xóa vào thùng rác.
	 *
	 * @param token mã token của liên kết chia sẻ
	 * @return một đối tượng {@link DocumentUploadResponse} chứa thông tin chi tiết của tài liệu được chia sẻ
	 * @throws ResourceNotFoundException nếu liên kết chia sẻ không tồn tại, đã hết hạn hoặc tài liệu gốc đã bị xóa
	 */
	@Transactional(readOnly = true)
	@Override
	public DocumentUploadResponse getDocumentByShareLink(String token) {
		return toResponse(findDocumentByShareLink(token));
	}

	/**
	 * Lấy đường dẫn xem trước (preview URL) tạm thời của tài liệu thông qua mã token của liên kết chia sẻ.
	 * <p>
	 * Phương thức này tìm kiếm tài liệu tương ứng với token chia sẻ hợp lệ, sau đó yêu cầu dịch vụ lưu trữ file
	 * sinh ra một đường dẫn tạm thời (SAS URL) cho phép truy cập xem trước file mà không cần đăng nhập.
	 *
	 * @param token mã token của liên kết chia sẻ
	 * @return đối tượng {@link FileAccessUrlResponse} chứa đường dẫn xem trước tạm thời
	 * @throws ResourceNotFoundException nếu không tìm thấy liên kết chia sẻ hợp lệ
	 */
	@Transactional(readOnly = true)
	@Override
	public FileAccessUrlResponse getShareLinkPreviewUrl(String token) {
		return toFileAccessUrlResponse(findDocumentByShareLink(token), false);
	}

	/**
	 * Lấy đường dẫn tải xuống (download URL) tạm thời của tài liệu thông qua mã token của liên kết chia sẻ.
	 * <p>
	 * Phương thức này tìm kiếm tài liệu tương ứng với token chia sẻ hợp lệ, sau đó yêu cầu dịch vụ lưu trữ file
	 * sinh ra một đường dẫn tạm thời (SAS URL) được cấu hình chế độ tải file trực tiếp (attachment) về máy.
	 *
	 * @param token mã token của liên kết chia sẻ
	 * @return đối tượng {@link FileAccessUrlResponse} chứa đường dẫn tải xuống tạm thời
	 * @throws ResourceNotFoundException nếu không tìm thấy liên kết chia sẻ hợp lệ
	 */
	@Transactional(readOnly = true)
	@Override
	public FileAccessUrlResponse getShareLinkDownloadUrl(String token) {
		return toFileAccessUrlResponse(findDocumentByShareLink(token), true);
	}

	/**
	 * Lưu một tài liệu được chia sẻ qua liên kết vào danh mục tài liệu được chia sẻ với tôi (Shared with me).
	 * <p>
	 * Phương thức này thực hiện các bước kiểm tra an toàn và nghiệp vụ:
	 * <ul>
	 *   <li>Tìm kiếm tài liệu gốc và kiểm tra tính hợp lệ của token chia sẻ (chưa hết hạn, chưa bị vô hiệu hóa).</li>
	 *   <li>Ngăn chặn chủ sở hữu tài liệu tự thực hiện hành động chia sẻ/lưu với chính mình.</li>
	 *   <li>Kiểm tra xem tài liệu đã từng được lưu/chia sẻ trước đó với người dùng hiện tại chưa để tránh tạo bản ghi trùng lặp.</li>
	 * </ul>
	 * Nếu hợp lệ, hệ thống tạo bản ghi liên kết chia sẻ mới trong bảng {@code document_shares}.
	 *
	 * @param token mã token của liên kết chia sẻ tài liệu
	 * @return đối tượng {@link DocumentShareResponse} chứa thông tin chia sẻ tài liệu thành công
	 * @throws ResourceNotFoundException nếu không tìm thấy liên kết chia sẻ hợp lệ hoặc tài liệu đã bị xóa
	 * @throws IllegalArgumentException  nếu người dùng cố tình tự lưu tài liệu của chính mình
	 */
	@Transactional
	@Override
	public DocumentShareResponse saveShareLinkToSharedWithMe(String token) {
		var userId = currentUserService.getCurrentUserId();
		var doc = findDocumentByShareLink(token);

		if (doc.getUserId().equals(userId)) {
			throw new IllegalArgumentException("You already own this document");
		}

		var existingShare = documentShareRepository
				.findByDocument_DocumentIdAndSharedWithUser_UserId(doc.getDocumentId(), userId);
		if (existingShare.isPresent()) {
			return toDocumentShareResponse(existingShare.get());
		}

		var sharedWithUser = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		var documentShare = new DocumentShare();
		documentShare.setDocument(doc);
		documentShare.setOwnerId(doc.getUserId());
		documentShare.setSharedWithUser(sharedWithUser);

		return toDocumentShareResponse(documentShareRepository.save(documentShare));
	}

	/**
	 * Chia sẻ quyền truy cập tài liệu trực tiếp cho một người dùng khác thông qua email của họ.
	 * <p>
	 * Các bước kiểm tra nghiệp vụ và an toàn:
	 * <ul>
	 *   <li>Xác thực người dùng hiện tại là chủ sở hữu của tài liệu đang hoạt động.</li>
	 *   <li>Tìm kiếm tài khoản người nhận thông qua email và đảm bảo họ tồn tại trong hệ thống.</li>
	 *   <li>Ngăn chặn chủ sở hữu tự chia sẻ tài liệu với chính bản thân mình.</li>
	 *   <li>Đảm bảo người sở hữu và người được chia sẻ đã là bạn bè của nhau (quan hệ tồn tại trong bảng {@code friendships}).</li>
	 *   <li>Đảm bảo tài liệu chưa từng được chia sẻ với người dùng này trước đó để tránh tạo bản ghi trùng lặp.</li>
	 * </ul>
	 * Nếu hợp lệ, hệ thống tạo bản ghi liên kết chia sẻ trong bảng {@code document_shares}.
	 *
	 * @param documentId ID của tài liệu muốn chia sẻ
	 * @param email      Email của người dùng được chia sẻ tài liệu
	 * @return một đối tượng {@link DocumentShareResponse} chứa thông tin chi tiết của việc chia sẻ tài liệu
	 * @throws ResourceNotFoundException nếu không tìm thấy tài liệu hoặc tài khoản người nhận
	 * @throws IllegalArgumentException  nếu tự chia sẻ với chính mình, hai người chưa kết bạn, hoặc tài liệu đã được chia sẻ trước đó
	 */
	@Transactional
	@Override
	public DocumentShareResponse shareDocumentWithUser(Long documentId, String email) {
		var ownerId = currentUserService.getCurrentUserId();
		var doc = findOwnedActiveDocument(ownerId, documentId);
		var sharedWithUser = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		var sharedWithUserId = sharedWithUser.getUserId();

		if (ownerId.equals(sharedWithUserId)) {
			throw new IllegalArgumentException("You cannot share a document with yourself");
		}

		var user1Id = Math.min(ownerId, sharedWithUserId);
		var user2Id = Math.max(ownerId, sharedWithUserId);
		if (!friendshipRepository.existsByUser_UserIdAndFriend_UserId(user1Id, user2Id)) {
			throw new IllegalArgumentException("You can only share documents with friends");
		}

		if (documentShareRepository.existsByDocument_DocumentIdAndSharedWithUser_UserId(documentId, sharedWithUserId)) {
			throw new IllegalArgumentException("Document is already shared with this user");
		}

		var documentShare = new DocumentShare();
		documentShare.setDocument(doc);
		documentShare.setOwnerId(ownerId);
		documentShare.setSharedWithUser(sharedWithUser);

		return toDocumentShareResponse(documentShareRepository.save(documentShare));
	}

	@Transactional(readOnly = true)
	@Override
	public List<DocumentShareResponse> getDocumentShares(Long documentId) {
		var ownerId = currentUserService.getCurrentUserId();
		findOwnedActiveDocument(ownerId, documentId);
		return documentShareRepository
				.findByDocument_DocumentIdAndOwnerIdOrderByCreatedAtDesc(documentId, ownerId)
				.stream()
				.map(this::toDocumentShareResponse)
				.toList();
	}

	/**
	 * Thu hồi quyền truy cập tài liệu đã chia sẻ trực tiếp với một người dùng cụ thể.
	 * <p>
	 * Phương thức này thực hiện xác thực quyền sở hữu của người dùng hiện tại đối với tài liệu gốc,
	 * sau đó tìm kiếm và xóa bản ghi chia sẻ tương ứng trong bảng {@code document_shares}.
	 *
	 * @param documentId ID của tài liệu cần thu hồi quyền chia sẻ
	 * @param userId     ID của người dùng bị thu hồi quyền truy cập tài liệu
	 * @throws ResourceNotFoundException nếu không tìm thấy bản ghi chia sẻ tài liệu tương ứng
	 */
	@Transactional
	@Override
	public void removeUserShare(Long documentId, Long userId) {
		var ownerId = currentUserService.getCurrentUserId();
		findOwnedActiveDocument(ownerId, documentId);

		var documentShare = documentShareRepository
				.findByDocument_DocumentIdAndOwnerIdAndSharedWithUser_UserId(documentId, ownerId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Document share not found"));
		documentShareRepository.delete(documentShare);
	}

	/**
	 * Lấy danh sách toàn bộ các tài liệu đang hoạt động được người khác chia sẻ với người dùng hiện tại.
	 * <p>
	 * Phương thức này truy vấn bảng {@code document_shares} để tìm kiếm các bản ghi được chia sẻ với
	 * người dùng hiện tại, kiểm tra xem tài liệu gốc chưa bị xóa và chuyển đổi kết quả thành danh sách DTO.
	 *
	 * @return một {@link List} chứa các đối tượng DTO {@link DocumentUploadResponse} đại diện cho các tài liệu được chia sẻ
	 */
	@Transactional(readOnly = true)
	@Override
	public List<DocumentUploadResponse> getSharedWithMeDocuments() {
		var userId = currentUserService.getCurrentUserId();
		return documentShareRepository.findBySharedWithUser_UserIdOrderByCreatedAtDesc(userId)
				.stream()
				.map(DocumentShare::getDocument)
				.filter(doc -> !Boolean.TRUE.equals(doc.getIsDeleted()))
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	@Override
	public DocumentUploadResponse getSharedWithMeDocumentDetail(Long documentId) {
		var userId = currentUserService.getCurrentUserId();
		return toResponse(findSharedWithMeActiveDocument(documentId, userId));
	}

	@Transactional(readOnly = true)
	@Override
	public FileAccessUrlResponse getSharedWithMePreviewUrl(Long documentId) {
		var userId = currentUserService.getCurrentUserId();
		return toFileAccessUrlResponse(findSharedWithMeActiveDocument(documentId, userId), false);
	}

	@Transactional(readOnly = true)
	@Override
	public FileAccessUrlResponse getSharedWithMeDownloadUrl(Long documentId) {
		var userId = currentUserService.getCurrentUserId();
		return toFileAccessUrlResponse(findSharedWithMeActiveDocument(documentId, userId), true);
	}

	@Transactional
	@Override
	public void removeSharedWithMeDocument(Long documentId) {
		var userId = currentUserService.getCurrentUserId();
		var documentShare = documentShareRepository
				.findByDocument_DocumentIdAndSharedWithUser_UserId(documentId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Shared document not found"));
		documentShareRepository.delete(documentShare);
	}

	@Transactional
	@Override
	public void bulkRemoveSharedWithMeDocuments(List<Long> documentIds) {
		if (documentIds == null || documentIds.isEmpty()) return;
		var userId = currentUserService.getCurrentUserId();
		for (var docId : documentIds) {
			documentShareRepository
					.findByDocument_DocumentIdAndSharedWithUser_UserId(docId, userId)
					.ifPresent(documentShareRepository::delete);
		}
	}

	@Transactional
	@Override
	public void bulkMoveDocuments(List<Long> documentIds, Long folderId) {
		if (documentIds == null || documentIds.isEmpty()) return;
		var userId = currentUserService.getCurrentUserId();
		if (folderId != null) {
			documentFolderRepository.findByFolderIdAndUserIdAndIsDeletedFalse(folderId, userId)
					.orElseThrow(() -> new ResourceNotFoundException("Target folder not found"));
		}
		for (var docId : documentIds) {
			documentRepository.findByDocumentIdAndUserIdAndIsDeletedFalse(docId, userId)
					.ifPresent(doc -> {
						doc.setFolderId(folderId);
						documentRepository.save(doc);
					});
		}
	}

	@Transactional
	@Override
	public void bulkMoveToTrash(List<Long> documentIds) {
		if (documentIds == null || documentIds.isEmpty()) return;
		var userId = currentUserService.getCurrentUserId();
		var now = Instant.now();
		for (var docId : documentIds) {
			documentRepository.findByDocumentIdAndUserIdAndIsDeletedFalse(docId, userId)
					.ifPresent(doc -> {
						doc.setIsDeleted(true);
						doc.setDeletedAt(now);
						documentRepository.save(doc);
					});
		}
	}

	/**
	 * Lưu một tài liệu được chia sẻ trực tiếp với tôi về kho lưu trữ cá nhân (My Files).
	 * <p>
	 * Quy tắc nghiệp vụ kiểm tra bên trong:
	 * <ul>
	 *   <li>Xác thực phiên đăng nhập của người dùng hiện tại (người nhận).</li>
	 *   <li>Xác thực tài liệu được chia sẻ tồn tại và có hiệu lực thông qua {@link #findSharedWithMeActiveDocument}.</li>
	 *   <li>Kiểm tra xem người dùng hiện tại đã lưu bản sao của tài liệu này trong My Files chưa (ném {@link IllegalArgumentException} nếu đã tồn tại).</li>
	 *   <li>Nếu {@code folderId} được chỉ định, kiểm tra thư mục thuộc sở hữu của người dùng hiện tại.</li>
	 *   <li>Kiểm tra hạn ngạch lưu trữ theo gói Subscription active.</li>
	 *   <li>Sao chép file object trên Azure Blob Storage sang object key mới của người dùng hiện tại.</li>
	 *   <li>Tạo bản ghi {@link Document} riêng tư mới trong My Files.</li>
	 *   <li>Nhân bản các bản ghi {@link DocumentChunk} để hỗ trợ Chat AI tức thì.</li>
	 * </ul>
	 *
	 * @param documentId ID của tài liệu được chia sẻ
	 * @param folderId   ID của thư mục đích trong My Files (tùy chọn, có thể null)
	 * @return một đối tượng DTO {@link DocumentUploadResponse} chứa thông tin tài liệu mới được tạo trong My Files
	 * @throws ResourceNotFoundException nếu tài liệu chia sẻ hoặc thư mục không tồn tại
	 * @throws IllegalArgumentException  nếu tài liệu đã được lưu trước đó hoặc vượt hạn ngạch dung lượng
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public DocumentUploadResponse saveSharedWithMeDocumentToMyFiles(Long documentId, Long folderId) {
		var userId = currentUserService.getCurrentUserId();
		var sourceDoc = findSharedWithMeActiveDocument(documentId, userId);

		var originalName = FilenameSanitizer.sanitize(sourceDoc.getOriginalFileName());
		boolean alreadySaved = documentRepository.existsByUserIdAndOriginalFileNameAndFileSizeAndIsDeletedFalse(
				userId,
				originalName,
				sourceDoc.getFileSize()
		);
		if (alreadySaved) {
			throw new IllegalArgumentException("You have already saved this document to My Files");
		}

		if (folderId != null) {
			documentFolderRepository.findByFolderIdAndUserId(folderId, userId)
					.orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
		}

		var activePlan = subscriptionEntitlementService.getActivePlan(userId);
		var currentUsageBytes = documentRepository.sumActiveStorageBytesByUserId(userId);
		long newTotalBytes = currentUsageBytes + (sourceDoc.getFileSize() != null ? sourceDoc.getFileSize() : 0L);
		long maxStorageBytes = activePlan.getStorageLimitGb() * 1024L * 1024L * 1024L;
		if (newTotalBytes > maxStorageBytes) {
			throw new IllegalArgumentException("Storage limit exceeded for your active plan");
		}

		var destinationKey = buildObjectKey(userId, originalName);

		fileStorageService.copyObject(sourceDoc.getS3Key(), destinationKey);

		var newDoc = new Document();
		newDoc.setUserId(userId);
		newDoc.setFolderId(folderId);
		newDoc.setOriginalFileName(originalName);
		newDoc.setS3Key(destinationKey);
		newDoc.setContentType(sourceDoc.getContentType());
		newDoc.setFileSize(sourceDoc.getFileSize());
		newDoc.setIsPublic(Boolean.FALSE);
		newDoc.setIsStarred(Boolean.FALSE);
		newDoc.setIsDeleted(Boolean.FALSE);
		newDoc.setStatus(sourceDoc.getStatus() != null ? sourceDoc.getStatus() : DocumentStatus.READY);

		var savedDoc = documentRepository.save(newDoc);

		var chunks = documentChunkRepository.findByDocumentDocumentIdOrderByChunkIndexAsc(sourceDoc.getDocumentId());
		if (chunks != null && !chunks.isEmpty()) {
			var newChunks = chunks.stream().map(chunk -> {
				var newChunk = new DocumentChunk();
				newChunk.setDocument(savedDoc);
				newChunk.setChunkIndex(chunk.getChunkIndex());
				newChunk.setContent(chunk.getContent());
				newChunk.setPageNumber(chunk.getPageNumber());
				newChunk.setEmbeddingVector(chunk.getEmbeddingVector());
				return newChunk;
			}).toList();
			documentChunkRepository.saveAll(newChunks);
		}

		return toResponse(savedDoc);
	}

	@Transactional
	@Override
	public DocumentUploadResponse updateVisibility(Long documentId, Boolean isPublic) {
		if (isPublic == null) {
			throw new IllegalArgumentException("isPublic is required");
		}
		var userId = currentUserService.getCurrentUserId();
		var doc = findOwnedActiveDocument(userId, documentId);
		doc.setIsPublic(isPublic);
		return toResponse(documentRepository.save(doc));
	}

	@Transactional
	@Override
	public DocumentUploadResponse updateStarred(Long documentId, Boolean isStarred) {
		if (isStarred == null) {
			throw new IllegalArgumentException("isStarred is required");
		}
		var userId = currentUserService.getCurrentUserId();
		var doc = findOwnedActiveDocument(userId, documentId);
		doc.setIsStarred(isStarred);
		return toResponse(documentRepository.save(doc));
	}

	@Transactional(readOnly = true)
	@Override
	public List<DocumentUploadResponse> getTrash() {
		var userId = currentUserService.getCurrentUserId();
		return documentRepository.findByUserIdAndIsDeletedTrueOrderByDeletedAtDesc(userId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	@Override
	public DocumentUploadResponse restoreFromTrash(Long documentId) {
		var userId = currentUserService.getCurrentUserId();
		var doc = findOwnedDocument(userId, documentId);
		if (Boolean.TRUE.equals(doc.getIsDeleted())) {
			if (doc.getFolderId() != null) {
				var folderOpt = documentFolderRepository.findByFolderIdAndUserId(doc.getFolderId(), userId);
				if (folderOpt.isEmpty() || Boolean.TRUE.equals(folderOpt.get().getIsDeleted())) {
					doc.setFolderId(null);
				}
			}
			doc.setIsDeleted(Boolean.FALSE);
			doc.setDeletedAt(null);
			doc = documentRepository.save(doc);
		}
		return toResponse(doc);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void deletePermanently(Long documentId) {
		var userId = currentUserService.getCurrentUserId();
		var doc = findOwnedDocument(userId, documentId);
		if (!Boolean.TRUE.equals(doc.getIsDeleted())) {
			throw new IllegalArgumentException("Document must be in trash before permanent delete");
		}

		if (doc.getS3Key() != null && !doc.getS3Key().isBlank()) {
			fileStorageService.delete(doc.getS3Key());
		}
		documentTagRepository.deleteByDocumentDocumentId(doc.getDocumentId());
		documentChunkRepository.deleteByDocumentDocumentId(doc.getDocumentId());
		documentShareRepository.deleteByDocument_DocumentId(doc.getDocumentId());
		documentShareLinkRepository.deleteByDocument_DocumentId(doc.getDocumentId());
		chatSessionDocumentRepository.deleteByDocumentDocumentId(doc.getDocumentId());
		documentRepository.delete(doc);
	}

	@Transactional(readOnly = true)
	@Override
	public DocumentPageResponse filterMyDocuments(
			List<Long> tagIds,
			String contentType,
			Instant createdFrom,
			Instant createdTo,
			String sort,
			int page,
			int size
	) {
		validatePageParams(page, size);
		var userId = currentUserService.getCurrentUserId();

		Specification<Document> spec = (root, query, cb) -> cb.and(
				cb.equal(root.get("userId"), userId),
				cb.isFalse(root.get("isDeleted"))
		);

		if (tagIds != null && !tagIds.isEmpty()) {
			var matchingDocumentIds = documentTagRepository.findDocumentIdsByTagIdIn(tagIds);
			if (matchingDocumentIds.isEmpty()) {
				return new DocumentPageResponse(List.of(), page, size, 0, 0);
			}
			spec = spec.and((root, query, cb) -> root.get("documentId").in(matchingDocumentIds));
		}

		if (StringUtils.hasText(contentType)) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("contentType"), contentType));
		}

		if (createdFrom != null) {
			spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("uploadedAt"), createdFrom));
		}

		if (createdTo != null) {
			spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("uploadedAt"), createdTo));
		}

		var pageable = PageRequest.of(page, size, Sort.by(resolveSortDirection(sort), "uploadedAt"));
		var resultPage = documentRepository.findAll(spec, pageable);
		var documents = resultPage.getContent().stream().map(this::toResponse).toList();

		return new DocumentPageResponse(
				documents,
				resultPage.getNumber(),
				resultPage.getSize(),
				resultPage.getTotalElements(),
				resultPage.getTotalPages()
		);
	}

	private Sort.Direction resolveSortDirection(String sort) {
		if (!StringUtils.hasText(sort) || "NEWEST".equalsIgnoreCase(sort)) {
			return Sort.Direction.DESC;
		}
		if ("OLDEST".equalsIgnoreCase(sort)) {
			return Sort.Direction.ASC;
		}
		throw new IllegalArgumentException("sort must be one of: NEWEST, OLDEST");
	}

	private void validatePageParams(int page, int size) {
		if (page < 0) {
			throw new IllegalArgumentException("Page must be greater than or equal to 0");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("Size must be between 1 and " + MAX_PAGE_SIZE);
		}
	}

	private String buildObjectKey(Long userId, String sanitizedFilename) {
		var prefix = azureStorageProperties.getKeyPrefix();
		if (prefix == null) {
			prefix = "";
		}
		prefix = prefix.trim();
		if (!prefix.isEmpty() && !prefix.endsWith("/")) {
			prefix = prefix + "/";
		}

		var uuid = UUID.randomUUID();
		return prefix + "documents/" + userId + "/" + uuid + "-" + sanitizedFilename;
	}

	private void registerIngestionAfterCommit(
			Long documentId,
			Path ingestionFile,
			String originalFilename,
			String contentType
	) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			// Không có transaction active để chờ, nên dispatch async job ngay lập tức.
			documentIngestionJobService.ingestAsync(documentId, ingestionFile, originalFilename, contentType);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				// Document id đã bền vững, worker có thể load lại và index temp file an toàn.
				documentIngestionJobService.ingestAsync(documentId, ingestionFile, originalFilename, contentType);
			}

			@Override
			public void afterCompletion(int status) {
				if (status != STATUS_COMMITTED) {
					// Không để lại bản copy tạm khi transaction database bao quanh bị rollback.
					deleteTempFileQuietly(ingestionFile);
				}
			}
		});
	}

	private void deleteTempFileQuietly(Path filePath) {
		if (filePath == null) {
			return;
		}
		try {
			Files.deleteIfExists(filePath);
		} catch (IOException ignored) {
		}
	}

	private Document findOwnedDocument(Long userId, Long documentId) {
		validateUserId(userId);
		if (documentId == null) {
			throw new IllegalArgumentException("documentId is required");
		}
		return documentRepository.findByDocumentIdAndUserId(documentId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Document not found"));
	}

	private Document findOwnedActiveDocument(Long userId, Long documentId) {
		validateUserId(userId);
		if (documentId == null) {
			throw new IllegalArgumentException("documentId is required");
		}
		return documentRepository.findByDocumentIdAndUserIdAndIsDeletedFalse(documentId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Document not found"));
	}

	private Document findPublicActiveDocument(Long documentId) {
		if (documentId == null) {
			throw new IllegalArgumentException("documentId is required");
		}
		return documentRepository.findByDocumentIdAndIsPublicTrueAndIsDeletedFalse(documentId)
				.orElseThrow(() -> new ResourceNotFoundException("Public document not found"));
	}

	private Document findDocumentByShareLink(String token) {
		if (token == null || token.isBlank()) {
			throw new IllegalArgumentException("token is required");
		}

		var shareLink = documentShareLinkRepository.findByTokenAndEnabledTrue(token)
				.orElseThrow(() -> new ResourceNotFoundException("Share link not found"));
		if (isExpired(shareLink)) {
			throw new ResourceNotFoundException("Share link not found");
		}

		var doc = shareLink.getDocument();
		if (Boolean.TRUE.equals(doc.getIsDeleted())) {
			throw new ResourceNotFoundException("Shared document not found");
		}
		return doc;
	}

	private Document findSharedWithMeActiveDocument(Long documentId, Long userId) {
		if (documentId == null) {
			throw new IllegalArgumentException("documentId is required");
		}
		var documentShare = documentShareRepository
				.findByDocument_DocumentIdAndSharedWithUser_UserId(documentId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Shared document not found"));

		var doc = documentShare.getDocument();
		if (Boolean.TRUE.equals(doc.getIsDeleted())) {
			throw new ResourceNotFoundException("Shared document not found");
		}
		return doc;
	}

	private boolean isExpired(DocumentShareLink shareLink) {
		return shareLink.getExpiresAt() != null && shareLink.getExpiresAt().isBefore(Instant.now());
	}

	private String generateShareToken() {
		String token;
		do {
			token = UUID.randomUUID().toString().replace("-", "");
		} while (documentShareLinkRepository.findByTokenAndEnabledTrue(token).isPresent());
		return token;
	}

	private void validateUserId(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException("userId is required");
		}
	}

	private String normalizeOriginalFileName(String originalFileName) {
		var sanitized = FilenameSanitizer.sanitize(originalFileName);
		if (sanitized.length() > 512) {
			sanitized = sanitized.substring(sanitized.length() - 512);
		}
		return sanitized;
	}

	private FileAccessUrlResponse toFileAccessUrlResponse(Document doc, boolean download) {
		// [SUA NGAY 2026-08-11 - co ho tro cua AI] getPresignedUrlExpirationMinutes ->
		// getSasExpirationMinutes: Azure goi URL co thoi han la SAS chu khong goi la
		// presigned URL. Y nghia va don vi (phut) khong doi nen phan tinh expiresAt
		// tra ve frontend van chinh xac nhu cu.
		var expiresAt = Instant.now().plus(Duration.ofMinutes(azureStorageProperties.getSasExpirationMinutes()));
		var url = fileStorageService.createPresignedGetUrl(
				doc.getS3Key(),
				doc.getOriginalFileName(),
				doc.getContentType(),
				download
		);
		return new FileAccessUrlResponse(url, expiresAt, doc.getOriginalFileName(), doc.getContentType());
	}

	private DocumentShareLinkResponse toShareLinkResponse(DocumentShareLink shareLink) {
		var response = new DocumentShareLinkResponse();
		response.setShareLinkId(shareLink.getShareLinkId());
		response.setDocumentId(shareLink.getDocument().getDocumentId());
		response.setToken(shareLink.getToken());
		response.setAccessPath("/api/documents/share-link/" + shareLink.getToken());
		response.setEnabled(shareLink.getEnabled());
		response.setExpiresAt(shareLink.getExpiresAt());
		response.setCreatedAt(shareLink.getCreatedAt());
		return response;
	}

	private DocumentShareResponse toDocumentShareResponse(DocumentShare documentShare) {
		User sharedWithUser = documentShare.getSharedWithUser();
		var response = new DocumentShareResponse();
		response.setDocumentShareId(documentShare.getDocumentShareId());
		response.setDocumentId(documentShare.getDocument().getDocumentId());
		response.setOwnerId(documentShare.getOwnerId());
		response.setSharedWithUserId(sharedWithUser.getUserId());
		response.setSharedWithEmail(sharedWithUser.getEmail());
		response.setSharedWithName(sharedWithUser.getFullName());
		response.setCreatedAt(documentShare.getCreatedAt());
		return response;
	}

	private DocumentUploadResponse toResponse(Document doc) {
		var res = new DocumentUploadResponse();
		res.setDocumentId(doc.getDocumentId());
		res.setUserId(doc.getUserId());
		userRepository.findById(doc.getUserId()).map(User::getEmail).ifPresent(res::setOwnerEmail);
		res.setFolderId(doc.getFolderId());
		res.setOriginalFileName(doc.getOriginalFileName());
		res.setS3Key(doc.getS3Key());
		res.setContentType(doc.getContentType());
		res.setFileSize(doc.getFileSize());
		res.setIsPublic(doc.getIsPublic());
		res.setIsDeleted(doc.getIsDeleted());
		res.setIsStarred(doc.getIsStarred());
		res.setStatus(doc.getStatus());
		res.setUploadedAt(doc.getUploadedAt());
		res.setDeletedAt(doc.getDeletedAt());
		return res;
	}
}
