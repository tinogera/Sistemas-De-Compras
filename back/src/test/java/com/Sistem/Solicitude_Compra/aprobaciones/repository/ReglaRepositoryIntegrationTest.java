package com.Sistem.Solicitude_Compra.aprobaciones.repository;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.TipoAprobador;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Regla;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.ReglaPaso;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Corre contra un Postgres real (Testcontainers) con las migraciones de Flyway ya aplicadas. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ReglaRepositoryIntegrationTest {

    @Autowired
    private ReglaRepository reglaRepository;

    @Test
    void despuesDeMigrarExisteLaReglaPorDefectoSinCondicionesYConUnPasoSupervisor() {
        List<Regla> porDefecto = reglaRepository.findByActivaTrueOrderByPrioridadAsc().stream()
                .filter(Regla::isPorDefecto)
                .toList();

        assertThat(porDefecto).hasSize(1);

        Regla regla = porDefecto.get(0);
        assertThat(regla.getNombre()).isEqualTo("Por defecto");
        assertThat(regla.isActiva()).isTrue();
        assertThat(regla.getCondiciones()).isEmpty();
        assertThat(regla.getPasos()).hasSize(1);

        ReglaPaso paso = regla.getPasos().get(0);
        assertThat(paso.getOrden()).isEqualTo(1);
        assertThat(paso.getTipoAprobador()).isEqualTo(TipoAprobador.ROL);
        assertThat(paso.getRol()).isEqualTo(Rol.SUPERVISOR);
        assertThat(paso.getSectorId()).isNull();
    }
}