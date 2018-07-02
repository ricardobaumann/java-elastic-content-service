package de.funkedigital.fuzo.contentservice.config;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.redfin.sitemapgenerator.WebSitemapGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.WebApplicationContext;

import java.net.MalformedURLException;

@Configuration
public class AwsConfig {

    @Bean
    AmazonSQS amazonSQS(@Value("${region}") String region) {
        return AmazonSQSClientBuilder.standard().withRegion(region).build();
    }

}
