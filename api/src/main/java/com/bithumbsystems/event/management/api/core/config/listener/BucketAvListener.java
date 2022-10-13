package com.bithumbsystems.event.management.api.core.config.listener;

import com.bithumbsystems.event.management.api.core.model.request.BucketUploadRequest;
import com.bithumbsystems.persistence.mongodb.chat.model.enums.FileStatus;
import com.bithumbsystems.persistence.mongodb.chat.service.ChatDomainService;
import com.bithumbsystems.persistence.mongodb.document.service.DocumentDomainService;
import com.bithumbsystems.persistence.mongodb.lrcmanagment.project.reviewestimate.service.ReviewEstimateDomainService;
import com.google.gson.Gson;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.Tag;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class BucketAvListener {

    private final S3AsyncClient s3AsyncClient;
    private final ReviewEstimateDomainService reviewEstimateDomainService;
    private final ChatDomainService chatDomainService;
    private final DocumentDomainService documentDomainService;

    /**
     * S3 Bucket에 Upload 후 호출되는 SQS Receiver Method
     * S3 상태를 체크해야 한다.
     *
     * @param header
     * @param message
     */
    @SqsListener(value = {"${cloud.aws.sqs.bucketav-queue-name}"}, deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    private void bucketAvStatusReceiver(@Headers Map<String, String> header, @Payload String message) {
        log.debug("header: {} message: {}", header, message);
        BucketUploadRequest bucketUploadRequest = new Gson().fromJson(message, BucketUploadRequest.class);

        scheduleStart(bucketUploadRequest);
    }

    private void scheduleStart(BucketUploadRequest bucketUploadRequest) {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(processUploadFileStatusCheck(scheduledExecutorService, bucketUploadRequest), 1, 8, TimeUnit.SECONDS);
    }

    private Runnable processUploadFileStatusCheck(ScheduledExecutorService scheduledExecutorService, BucketUploadRequest bucketUploadRequest) {
        log.debug(">> Thread start => processUploadFileStatusCheck");
        // S3 search
        return () -> {
            log.debug("s3 search => {}", Thread.currentThread().getName());
            // S3 tag : bucketav 의 값을 읽어야 한다.
            GetObjectTaggingRequest getTaggingRequest = GetObjectTaggingRequest.builder()
                    .bucket(bucketUploadRequest.getBucketName())
                    .key(bucketUploadRequest.getFileKey())
                    .build();  // new GetObjectTaggingRequest(bucketUploadRequest.getBucketName(), bucketUploadRequest.getFileKey());

            CompletableFuture<GetObjectTaggingResponse> tags = s3AsyncClient.getObjectTagging(getTaggingRequest);
            List<Tag> tagSet = tags.join().tagSet();

            Iterator<Tag> tagIterator = tagSet.listIterator();

            while(tagIterator.hasNext()) {
                Tag tag = (Tag) tagIterator.next();
                log.debug("tag => {}", tag);
                log.debug("tag => {}", tag);
                if (tag != null) {
                    if (tag.key().equals("bucketav")) {
                        String value = tag.value();

                        if (value.toUpperCase().equals("CLEAN") || value.toUpperCase().equals("INFECTED") || value.toUpperCase().equals("NO")) {
                            log.debug("CLEAN data receive....");
                            // DB 상태 업데이트를 수행한다.
                            processDbUpdate(bucketUploadRequest, value);
                            // 쓰레드 종료한다.
                            scheduledExecutorService.shutdown();
                        } else {
                            // Not Clean
                            // DB 상태 업데이트를 하고 다시 조회해야 한다.
                            log.debug("db update continue....");
                            processDbUpdate(bucketUploadRequest, value);
                        }
                    }
                }
            }
        };
    }

    /**
     * 테이블에 파일 상태 정보를 업데이트 한다.
     *
     * @param bucketUploadRequest
     * @param value
     */
    private void processDbUpdate(BucketUploadRequest bucketUploadRequest, String value)
    {
        if (bucketUploadRequest.getTableName().toLowerCase().equals("lrc_project_review_estimate")) {
            reviewEstimateDomainService.findFileKey(bucketUploadRequest.getFileKey())
                    .flatMap(result -> {
                        FileStatus fileStatus = null;
                        if (value.toUpperCase().equals("CLEAN")) fileStatus = FileStatus.CLEAN;
                        else if (value.toUpperCase().equals("INFECTED")) fileStatus = FileStatus.INFECTED;
                        else if (value.toUpperCase().equals("NO")) fileStatus = FileStatus.NO;
                        result.setFileStatus(fileStatus);
                        result.setUpdateDate(LocalDateTime.now());
                        result.setUpdateAdminAccountId(bucketUploadRequest.getAccountId());
                        return reviewEstimateDomainService.save(result);
                    })
                    .publishOn(Schedulers.boundedElastic()).subscribe();
        } else if (bucketUploadRequest.getTableName().toLowerCase().equals("lrc_chat_file")) {
            chatDomainService.findByChatFileId(bucketUploadRequest.getFileKey())
                    .flatMap(result -> {
                        FileStatus fileStatus = null;
                        if (value.toUpperCase().equals("CLEAN")) fileStatus = FileStatus.CLEAN;
                        else if (value.toUpperCase().equals("INFECTED")) fileStatus = FileStatus.INFECTED;
                        else if (value.toUpperCase().equals("NO")) fileStatus = FileStatus.NO;
                        result.setFileStatus(fileStatus);
                        return chatDomainService.save(result);
                    })
                    .publishOn(Schedulers.boundedElastic()).subscribe();
        } else if (bucketUploadRequest.getTableName().toLowerCase().equals("lrc_project_submitted_document_file")) {
            documentDomainService.findByDocFileId(bucketUploadRequest.getFileKey())
                    .flatMap(result -> {
                        FileStatus fileStatus = null;
                        if (value.toUpperCase().equals("CLEAN")) fileStatus = FileStatus.CLEAN;
                        else if (value.toUpperCase().equals("INFECTED")) fileStatus = FileStatus.INFECTED;
                        else if (value.toUpperCase().equals("NO")) fileStatus = FileStatus.NO;
                        result.setFileStatus(fileStatus);
                        return documentDomainService.save(result);
                    })
                    .publishOn(Schedulers.boundedElastic()).subscribe();
        }
    }
}



