package com.se1908.group01.entity;

import com.se1908.group01.enums.ShareApprovalStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
@Table(
		name = "document_shares",
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uk_document_share_user",
						columnNames = {"document_id", "shared_with_user_id"}
				)
		}
)
public class DocumentShare {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "document_share_id")
	private Long documentShareId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "document_id", nullable = false)
	private Document document;

	@Column(name = "owner_id", nullable = false)
	private Long ownerId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shared_with_user_id", nullable = false)
	private User sharedWithUser;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private ShareApprovalStatus status = ShareApprovalStatus.PENDING_APPROVAL;


	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

}
