package com.se1908.group01.service;

import com.se1908.group01.config.VNPayConfig;
import com.se1908.group01.dto.AdminPaymentListResponse;
import com.se1908.group01.dto.AdminPaymentResponse;
import com.se1908.group01.dto.PaymentCallbackResponse;
import com.se1908.group01.dto.PaymentHistoryResponse;
import com.se1908.group01.dto.PaymentPurchaseResponse;
import com.se1908.group01.dto.PurchaseRequest;
import com.se1908.group01.dto.RevenueResponse;
import com.se1908.group01.dto.SubscriptionResponse;
import com.se1908.group01.entity.Payment;
import com.se1908.group01.entity.Subscription;
import com.se1908.group01.entity.SubscriptionPlan;
import com.se1908.group01.entity.User;
import com.se1908.group01.enums.PaymentMethod;
import com.se1908.group01.enums.PaymentStatus;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.PaymentRepository;
import com.se1908.group01.repository.SubscriptionPlanRepository;
import com.se1908.group01.repository.UserRepository;
import com.se1908.group01.util.VNPayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionLifecycleService subscriptionLifecycleService;
    private final VNPayConfig vnPayConfig;

    /**
     * Tạo một giao dịch thanh toán cho gói đăng ký đã chọn và
     * sinh URL thanh toán VNPay để người dùng thực hiện thanh toán.
     *
     * @param email email của người dùng đang thực hiện thanh toán
     * @param request thông tin gói đăng ký và phương thức thanh toán
     * @return thông tin giao dịch bao gồm mã thanh toán, mã giao dịch,
     *         URL thanh toán và trạng thái hiện tại
     * @throws ResourceNotFoundException nếu không tìm thấy người dùng
     *                                   hoặc gói đăng ký
     * @throws IllegalArgumentException nếu gói đăng ký không khả dụng,
     *                                  là gói miễn phí hoặc phương thức
     *                                  thanh toán không hợp lệ
     * @throws IllegalStateException nếu cấu hình VNPay chưa đầy đủ
     */
    public PaymentPurchaseResponse purchase(
            String email,
            PurchaseRequest request) {

        validateVNPayConfiguration();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"));

        SubscriptionPlan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subscription plan not found"));

        if (!plan.isActive()) {
            throw new IllegalArgumentException(
                    "Subscription plan is no longer available");
        }

        if (SubscriptionLifecycleService.FREE_PLAN_NAME
                .equalsIgnoreCase(plan.getName())) {
            throw new IllegalArgumentException(
                    "FREE subscription plan does not require payment");
        }

        PaymentMethod paymentMethod = parsePaymentMethod(
                request.getPaymentMethod());

        Payment payment = Payment.builder()
                .user(user)
                .plan(plan)
                .amount(BigDecimal.valueOf(plan.getPrice()))
                .status(PaymentStatus.PENDING)
                .paymentMethod(paymentMethod)
                .build();

        payment = paymentRepository.save(payment);

        return PaymentPurchaseResponse.builder()
                .paymentId(payment.getId())
                .transactionNo(payment.getTransactionNo())
                .paymentUrl(createVNPayUrl(payment))
                .status(payment.getStatus())
                .build();
    }

    /**
     * Xử lý callback từ VNPay sau khi người dùng hoàn tất hoặc hủy thanh toán.
     * Phương thức sẽ kiểm tra chữ ký, mã merchant, số tiền thanh toán và
     * cập nhật trạng thái giao dịch tương ứng. Nếu thanh toán thành công,
     * hệ thống sẽ kích hoạt gói đăng ký cho người dùng.
     *
     * @param params tập các tham số callback do VNPay gửi về
     * @return kết quả xử lý callback bao gồm trạng thái giao dịch
     *         và thông tin giao dịch đã được xử lý hay chưa
     * @throws IllegalArgumentException nếu callback không hợp lệ, thiếu tham số,
     *                                  sai chữ ký, sai merchant hoặc sai số tiền
     * @throws ResourceNotFoundException nếu không tìm thấy giao dịch thanh toán
     * @throws IllegalStateException nếu cấu hình callback của VNPay chưa đầy đủ
     */
    @Transactional
    public PaymentCallbackResponse handleVNPayCallback(
            Map<String, String> params) {

        validateVNPayCallbackConfiguration();

        if (!VNPayUtil.isValidSignature(
                params,
                vnPayConfig.getHashSecret())) {
            throw new IllegalArgumentException(
                    "Invalid VNPay signature");
        }

        String transactionNo = requireParam(params, "vnp_TxnRef");
        String responseCode = requireParam(params, "vnp_ResponseCode");
        String transactionStatus = requireParam(
                params,
                "vnp_TransactionStatus");
        String tmnCode = requireParam(params, "vnp_TmnCode");

        if (!vnPayConfig.getTmnCode().equals(tmnCode)) {
            throw new IllegalArgumentException(
                    "Invalid VNPay merchant code");
        }

        Payment payment = paymentRepository
                .findByTransactionNoForUpdate(transactionNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found"));

        validateCallbackAmount(params, payment);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return callbackResponse(payment, true);
        }

        payment.setResponseCode(responseCode);

        if ("00".equals(responseCode)
                && "00".equals(transactionStatus)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);
            subscriptionLifecycleService.activatePaidSubscription(
                    payment.getUser(),
                    payment.getPlan(),
                    payment
            );
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }

        return callbackResponse(payment, false);
    }

    /**
     * Lấy lịch sử thanh toán của người dùng theo email.
     *
     * @param email email của người dùng cần lấy lịch sử thanh toán
     * @return danh sách các giao dịch thanh toán của người dùng,
     *         bao gồm thông tin gói đăng ký, số tiền, phương thức
     *         thanh toán, trạng thái và thời điểm thanh toán
     * @throws ResourceNotFoundException nếu không tìm thấy người dùng
     */
    public List<PaymentHistoryResponse> getMyPaymentHistory(
            String email) {

        User user = findUser(email);

        return paymentRepository.findByUser(user)
                .stream()
                .map(payment -> PaymentHistoryResponse.builder()
                        .paymentId(payment.getId())
                        .planName(payment.getPlan().getName())
                        .amount(payment.getAmount())
                        .paymentMethod(payment.getPaymentMethod())
                        .status(payment.getStatus())
                        .paidAt(payment.getPaidAt())
                        .build())
                .toList();
    }

    /**
     * Lấy danh sách tất cả giao dịch thanh toán theo phân trang.
     * Có thể lọc kết quả theo trạng thái thanh toán nếu được cung cấp.
     *
     * @param status trạng thái thanh toán cần lọc; nếu {@code null}
     *               hoặc rỗng thì trả về tất cả giao dịch
     * @param page số trang cần lấy (bắt đầu từ 0)
     * @param size số lượng bản ghi trên mỗi trang
     * @return danh sách giao dịch thanh toán theo phân trang
     * @throws IllegalArgumentException nếu tham số phân trang không hợp lệ
     *                                  hoặc trạng thái thanh toán không hợp lệ
     */
    @Transactional(readOnly = true)
    public AdminPaymentListResponse getAllPayments(
            String status,
            int page,
            int size) {

        validatePagination(page, size);

        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Payment> paymentPage;
        if (StringUtils.hasText(status)) {
            paymentPage = paymentRepository.findByStatus(
                    parsePaymentStatus(status),
                    pageable
            );
        } else {
            paymentPage = paymentRepository.findAll(pageable);
        }

        List<AdminPaymentResponse> payments = paymentPage
                .getContent()
                .stream()
                .map(this::toAdminPaymentResponse)
                .toList();

        return new AdminPaymentListResponse(
                payments,
                paymentPage.getNumber(),
                paymentPage.getSize(),
                paymentPage.getTotalElements(),
                paymentPage.getTotalPages()
        );
    }
    /**
     * Lấy thông tin tổng quan về doanh thu từ các giao dịch thanh toán thành công.
     *
     * @return thông tin doanh thu bao gồm tổng doanh thu và
     *         tổng số giao dịch thanh toán thành công
     */
    public RevenueResponse getRevenue() {
        return RevenueResponse.builder()
                .totalRevenue(paymentRepository.getTotalRevenue())
                .totalTransactions(
                        paymentRepository.countByStatus(
                                PaymentStatus.SUCCESS))
                .build();
    }

    /**
     * Lấy thông tin gói đăng ký hiện tại của người dùng.
     * Nếu người dùng chưa có gói đăng ký đang hoạt động,
     * hệ thống sẽ lấy hoặc khởi tạo gói đăng ký theo quy tắc hiện có.
     *
     * @param email email của người dùng
     * @return thông tin gói đăng ký hiện tại của người dùng
     * @throws ResourceNotFoundException nếu không tìm thấy người dùng
     */
    public SubscriptionResponse getMySubscription(String email) {
        User user = findUser(email);

        Subscription subscription = subscriptionLifecycleService
                .getOrCreateActiveSubscription(user);

        SubscriptionPlan plan = subscription.getPlan();

        return SubscriptionResponse.builder()
                .subscriptionId(subscription.getId())
                .status(subscription.getStatus())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .planName(plan.getName())
                .price(plan.getPrice())
                .durationDays(plan.getDurationDays())
                .storageLimitGb(plan.getStorageLimitGb())
                .allowedFormats(plan.getAllowedFormats())
                .maxUploadSizeMb(plan.getMaxUploadSizeMb())
                .multipleDocuments(plan.getMultipleDocuments())
                .videoUpload(plan.getVideoUpload())
                .monthlyTokenLimit(plan.getMonthlyTokenLimit())
                .build();
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"));
    }

    private PaymentMethod parsePaymentMethod(String paymentMethod) {
        try {
            return PaymentMethod.valueOf(
                    paymentMethod.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Invalid payment method");
        }
    }

    private PaymentStatus parsePaymentStatus(String status) {
        try {
            return PaymentStatus.valueOf(
                    status.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Payment status must be PENDING, SUCCESS, or FAILED");
        }
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must be greater than or equal to 0");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    private AdminPaymentResponse toAdminPaymentResponse(
            Payment payment) {

        return AdminPaymentResponse.builder()
                .paymentId(payment.getId())
                .transactionNo(payment.getTransactionNo())
                .userId(payment.getUser().getUserId())
                .userEmail(payment.getUser().getEmail())
                .planId(payment.getPlan().getId())
                .planName(payment.getPlan().getName())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .responseCode(payment.getResponseCode())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .build();
    }

    private String createVNPayUrl(Payment payment) {
        Map<String, String> params = new HashMap<>();

        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());

        long amount = payment.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        params.put("vnp_Amount", String.valueOf(amount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", payment.getTransactionNo());
        params.put(
                "vnp_OrderInfo",
                "Purchase " + payment.getPlan().getName());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put(
                "vnp_CreateDate",
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

        String query = VNPayUtil.buildQuery(params);
        String secureHash = VNPayUtil.hmacSHA512(
                vnPayConfig.getHashSecret(),
                query);

        return vnPayConfig.getPayUrl()
                + "?"
                + query
                + "&vnp_SecureHash="
                + secureHash;
    }

    private void validateVNPayConfiguration() {
        if (!StringUtils.hasText(vnPayConfig.getTmnCode())
                || !StringUtils.hasText(vnPayConfig.getHashSecret())
                || !StringUtils.hasText(vnPayConfig.getPayUrl())
                || !StringUtils.hasText(vnPayConfig.getReturnUrl())) {
            throw new IllegalStateException(
                    "VNPay configuration is incomplete");
        }
    }

    private void validateVNPayCallbackConfiguration() {
        if (!StringUtils.hasText(vnPayConfig.getTmnCode())
                || !StringUtils.hasText(vnPayConfig.getHashSecret())) {
            throw new IllegalStateException(
                    "VNPay callback configuration is incomplete");
        }
    }

    private void validateCallbackAmount(
            Map<String, String> params,
            Payment payment) {

        String rawAmount = requireParam(params, "vnp_Amount");

        try {
            long callbackAmount = Long.parseLong(rawAmount);
            long expectedAmount = payment.getAmount()
                    .movePointRight(2)
                    .longValueExact();

            if (callbackAmount != expectedAmount) {
                throw new IllegalArgumentException(
                        "VNPay payment amount does not match");
            }
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Invalid VNPay payment amount");
        }
    }

    private String requireParam(
            Map<String, String> params,
            String name) {

        String value = params.get(name);
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(
                    "Missing VNPay parameter: " + name);
        }

        return value;
    }

    private PaymentCallbackResponse callbackResponse(
            Payment payment,
            boolean alreadyProcessed) {

        return PaymentCallbackResponse.builder()
                .transactionNo(payment.getTransactionNo())
                .status(payment.getStatus())
                .alreadyProcessed(alreadyProcessed)
                .build();
    }
}
