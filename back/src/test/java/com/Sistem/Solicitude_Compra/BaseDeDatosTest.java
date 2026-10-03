package com.Sistem.Solicitude_Compra;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class BaseDeDatosTest {
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void conectaAPostgres() {
        String version = jdbc.queryForObject("SELECT version()", String.class);
        assertThat(version).contains("PostgreSQL 17");
    }

    @Test
    void existenLosSchemas() {
        List<String> schemas = jdbc.queryForList(
                "SELECT schema_name FROM information_schema.schemata", String.class);
        System.out.println("Schemas: " + schemas);
        assertThat(schemas).contains("auth", "catalogo", "solicitudes",
                "compras", "ordenes", "reportes", "auditoria");
    }
}