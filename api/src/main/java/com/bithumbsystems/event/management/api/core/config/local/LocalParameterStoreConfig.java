package com.bithumbsystems.event.management.api.core.config.local;

import com.bithumbsystems.event.management.api.core.config.constant.ParameterStoreConstant;
import com.bithumbsystems.event.management.api.core.config.properties.AwsProperties;
import com.bithumbsystems.event.management.api.core.config.properties.MongoProperties;
import javax.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import java.net.URI;

@Log4j2
@Data
@Profile("local|default")
@Configuration
public class LocalParameterStoreConfig {

    private SsmClient ssmClient;
    private MongoProperties mongoProperties;
    private final AwsProperties awsProperties;
    private final CredentialsProvider credentialsProvider;

    @Value("${spring.profiles.active:}")
    private String profileName;

    @PostConstruct
    public void init() {

        log.debug("config store [prefix] => {}", awsProperties.getPrefix());
        log.debug("config store [doc name] => {}", awsProperties.getParamStoreDocName());
        log.debug("config store [kms name] => {}", awsProperties.getParamStoreKmsName());

        this.ssmClient = SsmClient.builder()
                .endpointOverride(URI.create(awsProperties.getSsmEndPoint()))
                //.endpointOverride(URI.create("https://ssm.ap-northeast-2.amazonaws.com"))
                .credentialsProvider(credentialsProvider.getProvider()) // 로컬에서 개발로 붙을때 사용
                .region(Region.of(awsProperties.getRegion()))
                .build();

        this.mongoProperties = new MongoProperties(
                getParameterValue(awsProperties.getParamStoreDocName(), ParameterStoreConstant.DB_URL),
                getParameterValue(awsProperties.getParamStoreDocName(), ParameterStoreConstant.DB_USER),
                getParameterValue(awsProperties.getParamStoreDocName(), ParameterStoreConstant.DB_PASSWORD),
                getParameterValue(awsProperties.getParamStoreDocName(), ParameterStoreConstant.DB_PORT),
                getParameterValue(awsProperties.getParamStoreDocName(), ParameterStoreConstant.DB_NAME)
        );

        // KMS Parameter Key
        this.awsProperties.setKmsKey(getParameterValue(awsProperties.getParamStoreKmsName(), ParameterStoreConstant.KMS_ALIAS_NAME));
        this.awsProperties.setSaltKey(getParameterValue(awsProperties.getParamStoreSaltName(), ParameterStoreConstant.KMS_ALIAS_NAME));
        this.awsProperties.setIvKey(getParameterValue(awsProperties.getParamStoreIvName(), ParameterStoreConstant.KMS_ALIAS_NAME));
        log.debug(">> DB Crypto:{}, {}, {}", this.awsProperties.getKmsKey(), this.awsProperties.getSaltKey(), this.awsProperties.getIvKey());
        this.awsProperties.setEmailSender(getParameterValue(awsProperties.getParamStoreMessageName(), ParameterStoreConstant.MAIL_SENDER));
        this.awsProperties.setSmtpUserName(getParameterValue(awsProperties.getParamStoreMessageName(), ParameterStoreConstant.SMTP_USERNAME));
        this.awsProperties.setSmtpUserPassword(getParameterValue(awsProperties.getParamStoreMessageName(), ParameterStoreConstant.SMTP_PASSWORD));
        this.awsProperties.setSqsBucketavUrl(getParameterValue(awsProperties.getParamStoreMessageName(), ParameterStoreConstant.SQS_BUCKETAV_URL)); // ADD
        this.awsProperties.setCryptoKey(getParameterValue(awsProperties.getParamCryptoName(), ParameterStoreConstant.CRYPTO_KEY));
        this.awsProperties.setJwtSecretKey(getParameterValue(awsProperties.getParamStoreAuthName(), ParameterStoreConstant.JWT_SECRET_KEY));
    }

    protected String getParameterValue(String storeName, String type) {
        String parameterName = String.format("%s/%s_%s/%s", awsProperties.getPrefix(), storeName, profileName, type);

        GetParameterRequest request = GetParameterRequest.builder()
                .name(parameterName)
                .withDecryption(true)
                .build();

        GetParameterResponse response = this.ssmClient.getParameter(request);

        return response.parameter().value();
    }
}
