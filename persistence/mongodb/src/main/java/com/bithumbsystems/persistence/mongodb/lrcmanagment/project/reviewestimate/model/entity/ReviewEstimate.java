package com.bithumbsystems.persistence.mongodb.lrcmanagment.project.reviewestimate.model.entity;

import com.bithumbsystems.persistence.mongodb.chat.model.enums.FileStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("lrc_project_review_estimate")
public class ReviewEstimate {

    @Id
    private String id;
    private String projectId;       //프로젝트 코드
    private String organization;    //평가 기관
    private String result;          //평가 결과
    private String reference;       //평가 자료
    private FileStatus fileStatus;
    private String fileKey;         //평가 자료 파일
    private String fileName;        // 파일명
    private Boolean delYn;          //삭제여부
    private LocalDateTime createDate; //생성날짜
    private String createAdminAccountId;  //생성자 id

    private LocalDateTime updateDate; //수정날짜
    private String updateAdminAccountId; //수정자 id
}
