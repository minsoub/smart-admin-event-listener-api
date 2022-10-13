package com.bithumbsystems.persistence.mongodb.document.model.entity;

import com.bithumbsystems.persistence.mongodb.chat.model.enums.FileStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document("lrc_project_submitted_document_file")
public class DocFile {
    @MongoId(value = FieldType.STRING, targetType = FieldType.STRING)
    private String id;
    private String projectId;
    private String type;
    private String fileKey;
    private String fileName;
    private String email;
    private FileStatus fileStatus;
    private LocalDateTime createDate;
    private String createAccountId;
}
