package com.Sistem.Solicitude_Compra.aprobaciones;

import com.Sistem.Solicitude_Compra.aprobaciones.repository.AprobacionItemRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** API pública del módulo Aprobaciones para los demás módulos (hoy: Integraciones, para el aviso al aprobador). */
@Service
@Transactional(readOnly = true)
public class AprobacionesApi {

    private final AprobacionItemRepository aprobaciones;

    public AprobacionesApi(AprobacionItemRepository aprobaciones) {
        this.aprobaciones = aprobaciones;
    }

    public Optional<DatosParaAviso> datosParaAviso(Long itemId) {
        return aprobaciones.findById(itemId)
                .map(i -> new DatosParaAviso(i.getSolicitanteId(), i.getSectorId(), i.getDetalle()));
    }

    public record DatosParaAviso(Long solicitanteId, Long sectorId, String detalle) {
    }
}
