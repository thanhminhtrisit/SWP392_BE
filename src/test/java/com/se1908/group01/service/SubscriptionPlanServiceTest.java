package com.se1908.group01.service;

import com.se1908.group01.dto.CreatePlanRequest;
import com.se1908.group01.dto.UpdatePlanRequest;
import com.se1908.group01.entity.SubscriptionPlan;
import com.se1908.group01.exception.ConflictException;
import com.se1908.group01.repository.SubscriptionPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionPlanServiceTest {

    @Mock
    private SubscriptionPlanRepository repository;

    private SubscriptionPlanService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionPlanService(repository);
    }

    @Test
    void createNormalizesNameAndAllowsReusingDeletedPlanName() {
        CreatePlanRequest request = createRequest("  PLUS  ");
        when(repository.existsByNameIgnoreCaseAndActiveTrue("PLUS"))
                .thenReturn(false);
        when(repository.save(any(SubscriptionPlan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(request);

        assertEquals("PLUS", response.getName());
        verify(repository).save(any(SubscriptionPlan.class));
    }

    @Test
    void createRejectsDuplicateActivePlanNameIgnoringCase() {
        CreatePlanRequest request = createRequest("plus");
        when(repository.existsByNameIgnoreCaseAndActiveTrue("plus"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> service.create(request)
        );
    }

    @Test
    void updateExcludesCurrentPlanFromDuplicateCheck() {
        SubscriptionPlan plan = plan(1L, "BASIC", true);
        UpdatePlanRequest request = updateRequest(" PLUS ");
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(plan));
        when(repository.existsByNameIgnoreCaseAndActiveTrueAndIdNot("PLUS", 1L))
                .thenReturn(false);
        when(repository.save(plan)).thenReturn(plan);

        var response = service.update(1L, request);

        assertEquals("PLUS", response.getName());
    }

    @Test
    void deleteSoftDeletesActivePlan() {
        SubscriptionPlan plan = plan(1L, "PLUS", true);
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(plan));

        service.delete(1L);

        assertEquals(false, plan.isActive());
        verify(repository).save(plan);
    }

    @Test
    void createRequiresFreePlanPriceToBeZero() {
        CreatePlanRequest request = createRequest("FREE");
        request.setPrice(1000D);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(request)
        );
    }

    @Test
    void deleteRejectsFreePlan() {
        SubscriptionPlan plan = plan(1L, "FREE", true);
        when(repository.findById(1L))
                .thenReturn(java.util.Optional.of(plan));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.delete(1L)
        );
    }

    private CreatePlanRequest createRequest(String name) {
        CreatePlanRequest request = new CreatePlanRequest();
        request.setName(name);
        request.setPrice(99000D);
        request.setDurationDays(30);
        request.setDescription("Plus plan");
        request.setStorageLimitGb(10);
        request.setAllowedFormats("pdf,docx");
        request.setMaxUploadSizeMb(50);
        request.setMultipleDocuments(true);
        request.setVideoUpload(true);
        request.setMonthlyTokenLimit(100000L);
        return request;
    }

    private UpdatePlanRequest updateRequest(String name) {
        UpdatePlanRequest request = new UpdatePlanRequest();
        request.setName(name);
        request.setPrice(99000D);
        request.setDurationDays(30);
        request.setDescription("Updated plan");
        request.setStorageLimitGb(10);
        request.setAllowedFormats("pdf,docx");
        request.setMaxUploadSizeMb(50);
        request.setMultipleDocuments(true);
        request.setVideoUpload(true);
        request.setMonthlyTokenLimit(100000L);
        return request;
    }

    private SubscriptionPlan plan(Long id, String name, boolean active) {
        return SubscriptionPlan.builder()
                .id(id)
                .name(name)
                .price(99000D)
                .durationDays(30)
                .description("Plan")
                .storageLimitGb(10)
                .allowedFormats("pdf,docx")
                .maxUploadSizeMb(50)
                .multipleDocuments(true)
                .videoUpload(true)
                .monthlyTokenLimit(100000L)
                .active(active)
                .build();
    }
}
