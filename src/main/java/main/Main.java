package main;

import main.generator.EntityGenerator;
import main.generator.RepositoryGenerator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(EntityGenerator entityGenerator, RepositoryGenerator repositoryGenerator) {
        return args -> {
            entityGenerator.generateEntities();
            System.out.println("Entity generation completed!");
            repositoryGenerator.generateRepositories();
            System.out.println("Repository generation completed!");
        };
    }
}