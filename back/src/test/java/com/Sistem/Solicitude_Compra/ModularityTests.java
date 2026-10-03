package com.Sistem.Solicitude_Compra;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTests {

	ApplicationModules modules = ApplicationModules.of(SolicitudeCompraApplication.class);

	@Test
	void verificaLaEstructuraDeModulos() {
		modules.forEach(System.out::println);
		modules.verify();
	}

	@Test
	void generaLaDocumentacionDeModulos() {
		new Documenter(modules).writeDocumentation();
	}
}
