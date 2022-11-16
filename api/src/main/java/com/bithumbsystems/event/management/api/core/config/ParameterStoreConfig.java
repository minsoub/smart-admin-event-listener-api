package com.bithumbsystems.event.management.api.core.config;

import com.bithumbsystems.event.management.api.core.config.constant.ParameterStoreConstant;
import com.bithumbsystems.event.management.api.core.config.properties.AwsProperties;
import com.bithumbsystems.event.management.api.core.config.properties.MongoProperties;
import java.net.URI;
import javax.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

@Slf4j
@Data
@Configuration
@Profile("dev|prod|qa|eks-dev|eks-prod")
public class ParameterStoreConfig {

    private SsmClient ssmClient;
    private MongoProperties mongoProperties;

    private final AwsProperties awsProperties;

    @Value("${cloud.aws.credentials.profile-name}")
    private String profileName;

    @PostConstruct
    public void init() {

        log.debug("config store [prefix] => {}", awsProperties.getPrefix());
        log.debug("config store [name] => {}", awsProperties.getParamStoreDocName());

        this.ssmClient = SsmClient.builder()
            .region(Region.of(awsProperties.getRegion()))
            .endpointOverride(URI.create(awsProperties.getSsmEndPoint())) // "https://ssm.ap-northeast-2.amazonaws.com"))
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
        this.awsProperties.setEmailSender(getParameterValue(awsProperties.getParamStoreMessageName(), ParameterStoreConstant.MAIL_SENDER));
        this.awsProperties.setSmtpUserName(getParameterValue(awsProperties.getParamStoreMessageName(), ParameterStoreConstant.SMTP_USERNAME).trim());
        this.awsProperties.setSmtpUserPassword(getParameterValue(awsProperties.getParamStoreMessageName(), ParameterStoreConstant.SMTP_PASSWORD).trim());
        this.awsProperties.setJwtSecretKey(getParameterValue(awsProperties.getParamStoreAuthName(), ParameterStoreConstant.JWT_SECRET_KEY));
        this.awsProperties.setCryptoKey(getParameterValue(awsProperties.getParamCryptoName(), ParameterStoreConstant.CRYPTO_KEY));
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
