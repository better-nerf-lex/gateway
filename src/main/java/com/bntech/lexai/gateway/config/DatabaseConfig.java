package com.bntech.lexai.gateway.config;

import com.bntech.lexai.gateway.config.converter.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.bntech.lexai.gateway.data.repository")
public class DatabaseConfig {

    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        CompositeDatabasePopulator populator = new CompositeDatabasePopulator();
        populator.addPopulators(new ResourceDatabasePopulator(new ClassPathResource("schema.sql")));
        initializer.setDatabasePopulator(populator);

        return initializer;
    }

    @Bean
    public R2dbcCustomConversions customConversions(ObjectMapper objectMapper) {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new EntityDetailsToJsonConverter(objectMapper));
        converters.add(new JsonToEntityDetailsConverter(objectMapper));
        converters.add(new JsonToQuestionDtoListConverter(objectMapper));
        converters.add(new QuestionDtoListToJsonConverter(objectMapper));
        converters.add(new JsonToStringListConverter(objectMapper));
        converters.add(new StringListToJsonConverter(objectMapper));
        converters.add(new JsonToTemplateFileListConverter(objectMapper));
        converters.add(new TemplateFileListToJsonConverter(objectMapper));
        converters.add(new StringToActionTypeConverter());
        converters.add(new StringToCasePartyRoleConverter());
        return new R2dbcCustomConversions(CustomConversions.StoreConversions
                .of(PostgresDialect.INSTANCE.getSimpleTypeHolder()), converters);
    }

}
