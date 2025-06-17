package org.example.services;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class Rdf4jConfig {

    @Value("${rdf4j.repository.url}")
    private String repositoryUrl;

    @Bean
    public Repository repository() {
        return new HTTPRepository(repositoryUrl);
    }

    @Bean
    @Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public RepositoryConnection repositoryConnection(Repository repository) {
        return repository.getConnection();
    }
}