package com.luixtech.framework.config.api;

import com.luixtech.framework.config.LuixProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import lombok.Getter;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.Ordered;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

/**
 * A OpenApi customizer to set up {@link OpenAPI}
 */
@Getter
public class OpenApiCustomizer implements OpenApiCustomiser, Ordered {
    private final int                    order = 0;
    private final LuixProperties.ApiDocs apiDocsProperties;
    private final BuildProperties        buildProperties;

    public OpenApiCustomizer(LuixProperties.ApiDocs apiDocsProperties, BuildProperties buildProperties) {
        this.apiDocsProperties = apiDocsProperties;
        this.buildProperties = buildProperties;
    }

    public void customise(OpenAPI openApi) {
        Contact contact = new Contact()
                .name(apiDocsProperties.getContactName())
                .url(apiDocsProperties.getContactUrl())
                .email(apiDocsProperties.getContactEmail());

        openApi.info(new Info()
                .contact(contact)
                .title(apiDocsProperties.getTitle())
                .description(apiDocsProperties.getDescription())
                .version(defaultIfEmpty(buildProperties.getVersion(), apiDocsProperties.getVersion()))
                .termsOfService(apiDocsProperties.getTermsOfServiceUrl())
                .license(new License().name(apiDocsProperties.getLicense()).url(apiDocsProperties.getLicenseUrl()))
        );

        for (LuixProperties.ApiDocs.Server server : apiDocsProperties.getServers()) {
            openApi.addServersItem(new Server().url(server.getUrl()).description(server.getDescription()));
        }
    }
}