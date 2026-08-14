package com.se1908.group01.service;

import com.google.cloud.speech.v1.LongRunningRecognizeRequest;
import com.google.cloud.speech.v1.LongRunningRecognizeResponse;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.se1908.group01.config.GoogleSpeechProperties;
import com.se1908.group01.entity.Document;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
/**
 * Tạo transcript cho video đã upload để nội dung video đi vào cùng pipeline chunk/embedding.
 */
public class VideoTranscriptParser {

    private static final Logger log = LoggerFactory.getLogger(VideoTranscriptParser.class);

    private final SpeechClient speechClient;
    private final Storage storage;
    /*
     * [SUA NGAY 2026-08-11 - co ho tro cua AI] Bo ObjectProvider<S3Client> va S3Properties,
     * inject FileStorageService thay vao.
     *
     * LY DO: truoc day lop nay inject THANG S3Client roi tu goi getObjectAsBytes(...) de
     * tai video ve dia tam cho FFmpeg - tuc la di vong qua interface storage va keo SDK
     * cua nha cung cap vao tan tang service. Doi nha cung cap la phai sua ca file nay.
     * Gio viec tai file da nam trong FileStorageService.downloadToFile(...), o day chi
     * con goi mot dong, khong biet gi ve Azure hay S3.
     *
     * KHONG DUNG ObjectProvider NUA: FileStorageService luon co bean (chinh no moi kiem
     * tra cau hinh va nem loi ro rang khi thieu), khac S3Client truoc day la bean co dieu
     * kien nen co the vang mat.
     */
    private final FileStorageService fileStorageService;
    private final GoogleSpeechProperties googleSpeechProperties;

    public VideoTranscriptParser(
            ObjectProvider<SpeechClient> speechClientProvider,
            ObjectProvider<Storage> storageProvider,
            FileStorageService fileStorageService,
            GoogleSpeechProperties googleSpeechProperties
    ) {
        this.speechClient = speechClientProvider.getIfAvailable();
        this.storage = storageProvider.getIfAvailable();
        this.fileStorageService = fileStorageService;
        this.googleSpeechProperties = googleSpeechProperties;
    }

    /**
     * Tải video từ file storage, trích xuất âm thanh và chuyển đổi nội dung giọng nói thành văn bản.
     *
     * @param documentId mã định danh của tài liệu cần xử lý
     * @param s3Key đường dẫn đối tượng video trong file storage (blob name)
     * @param contentType kiểu nội dung của video
     * @return nội dung văn bản được nhận dạng từ video
     * @throws IllegalStateException khi dịch vụ Google Speech-to-Text hoặc file storage chưa được cấu hình
     * @throws RuntimeException khi quá trình tải, chuyển đổi hoặc nhận dạng video thất bại
     */
    public String parse(Long documentId, String s3Key, String contentType) {
        // Video parsing đọc object gốc bằng document id/object key và trả về text cho embedding.
        if (speechClient == null || storage == null) {
            throw new IllegalStateException(
                    "Google Cloud Speech-to-Text is not configured. "
                            + "Set GOOGLE_SPEECH_GCS_BUCKET and GOOGLE_APPLICATION_CREDENTIALS environment variables.");
        }
        // [SUA NGAY 2026-08-11 - co ho tro cua AI] Bo doan kiem tra `if (s3Client == null)`
        // o day. LY DO: viec kiem tra cau hinh storage da chuyen han vao
        // AzureBlobStorageServiceImpl - no tu nem IllegalStateException voi thong bao dung
        // (doi AZURE_STORAGE_CONNECTION_STRING / AZURE_STORAGE_CONTAINER, khong con nhac
        // AWS_REGION / AWS_S3_BUCKET_NAME nua). Giu them mot ban kiem tra o day se thanh
        // hai cho phai sua moi lan doi cau hinh.

        Path tempMp4 = null;
        Path tempFlac = null;
        String flacGcsKey = null;

        try {
            // 1. Download MP4 from object storage → local temp file
            // [SUA NGAY 2026-08-11 - co ho tro cua AI] Thay getObjectAsBytes(...) + Files.write(...)
            // bang mot loi goi downloadToFile(...).
            // LY DO NGOAI VIEC BO SDK: cach cu doc TOAN BO video vao mang byte trong bo nho
            // roi moi ghi ra dia - video duoc phep len toi 50MB (APP_MAX_VIDEO_FILE_SIZE)
            // nen moi lan parse la mot lan giu 50MB tren heap. downloadToFile ghi thang
            // xuong dia, khong qua buoc trung gian do.
            tempMp4 = Files.createTempFile("video-" + documentId + "-", ".mp4");
            log.info("Downloading video from file storage for document {}", documentId);
            fileStorageService.downloadToFile(s3Key, tempMp4);

            // 2. Extract audio to FLAC (16 kHz mono) via system FFmpeg
            tempFlac = Files.createTempFile("audio-" + documentId + "-", ".flac");
            extractAudio(tempMp4, tempFlac);

            // 3. Upload FLAC to GCS
            flacGcsKey = googleSpeechProperties.getGcsKeyPrefix()
                    + documentId + "/audio-" + System.currentTimeMillis() + ".flac";
            log.info("Uploading FLAC to GCS: {}", flacGcsKey);
            storage.create(
                    BlobInfo.newBuilder(BlobId.of(googleSpeechProperties.getGcsBucketName(), flacGcsKey))
                            .setContentType("audio/flac")
                            .build(),
                    Files.readAllBytes(tempFlac)
            );

            // 4. Transcribe via Google STT v1
            return transcribe(documentId, flacGcsKey);

        } catch (IOException e) {
            throw new RuntimeException(
                    "I/O error during video transcription for document " + documentId, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Video transcription interrupted for document " + documentId, e);
        } finally {
            // 5. Always clean up local temp files and the GCS audio object
            deleteTempFile(tempMp4);
            deleteTempFile(tempFlac);
            deleteFromGcs(flacGcsKey);
        }
    }

    /**
     * Trích xuất luồng âm thanh từ video MP4 sang định dạng FLAC 16 kHz, một kênh bằng FFmpeg.
     *
     * @param inputMp4 đường dẫn đến tệp video MP4 đầu vào
     * @param outputFlac đường dẫn đến tệp âm thanh FLAC đầu ra
     * @throws IOException khi không thể khởi chạy FFmpeg hoặc đọc kết quả xử lý
     * @throws InterruptedException khi luồng đang chờ tiến trình FFmpeg bị gián đoạn
     */
    private void extractAudio(Path inputMp4, Path outputFlac) throws IOException, InterruptedException {
        log.debug("FFmpeg: extracting audio {} → {}", inputMp4, outputFlac);
        var process = new ProcessBuilder(
                "ffmpeg", "-y",
                "-i", inputMp4.toAbsolutePath().toString(),
                "-vn",             // strip video stream
                "-acodec", "flac", // FLAC codec
                "-ar", "16000",    // 16 kHz sample rate
                "-ac", "1",        // mono
                outputFlac.toAbsolutePath().toString()
        )
                .redirectErrorStream(true) // merge stderr into stdout so we can drain it
                .start();

        // Drain output to prevent pipe-buffer deadlock on large files
        var ffmpegOutput = new String(process.getInputStream().readAllBytes());
        var exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException(
                    "FFmpeg exited with code " + exitCode + " for document input " + inputMp4
                            + ":\n" + ffmpegOutput);
        }
        log.debug("FFmpeg extraction complete, output: {}", outputFlac);
    }

    private String transcribe(Long documentId, String flacGcsKey) {
        var gcsUri = "gs://" + googleSpeechProperties.getGcsBucketName() + "/" + flacGcsKey;
        log.info("Calling STT LongRunningRecognize for document {}, uri={}", documentId, gcsUri);

        // No setEncoding — FLAC from GCS is auto-detected by STT v1
        var config = RecognitionConfig.newBuilder()
                .setLanguageCode("en-US")
                .build();

        var audio = RecognitionAudio.newBuilder()
                .setUri(gcsUri)
                .build();

        var request = LongRunningRecognizeRequest.newBuilder()
                .setConfig(config)
                .setAudio(audio)
                .build();

        LongRunningRecognizeResponse response;
        try {
            response = speechClient.longRunningRecognizeAsync(request)
                    .get(googleSpeechProperties.getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Transcription interrupted for document " + documentId, e);
        } catch (TimeoutException e) {
            throw new RuntimeException("Transcription timed out after "
                    + googleSpeechProperties.getTimeoutSeconds() + "s for document "
                    + documentId, e);
        } catch (ExecutionException e) {
            var cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException(
                    "Transcription failed for document " + documentId
                            + ": " + cause.getMessage(), cause);
        }

        var sb = new StringBuilder();
        for (var result : response.getResultsList()) {
            if (!result.getAlternativesList().isEmpty()) {
                sb.append(result.getAlternatives(0).getTranscript()).append(" ");
            }
        }

        var transcript = sb.toString().trim();
        if (!StringUtils.hasText(transcript)) {
            throw new RuntimeException(
                    "No speech detected in video for document " + documentId);
        }
        log.info("Transcription completed for document {}: {} characters",
                documentId, transcript.length());
        return transcript;
    }

    private void deleteTempFile(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete temp file {}: {}", path, e.getMessage());
        }
    }

    private void deleteFromGcs(String gcsKey) {
        if (!StringUtils.hasText(gcsKey)) return;
        try {
            storage.delete(BlobId.of(googleSpeechProperties.getGcsBucketName(), gcsKey));
            log.debug("Deleted GCS audio object: {}", gcsKey);
        } catch (Exception e) {
            log.warn("Failed to delete GCS audio object {}: {}", gcsKey, e.getMessage());
        }
    }
}
