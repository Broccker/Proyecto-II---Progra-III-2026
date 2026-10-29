package com.sistema.backend.logic.ia;

import com.sistema.common.modelo.CategoriaRecurso;
import java.util.ArrayList;
import java.util.List;

/**
 * Resuelve los nombres de categoría que propone la IA contra las categorías
 * reales del sistema, sin distinguir mayúsculas ni minúsculas.
 */
public final class MapeoCategorias {

    private MapeoCategorias() {
    }

    public record ResultadoMapeo(List<CategoriaRecurso> encontradas,
                                 List<String> noEncontradas) {

        public ResultadoMapeo {
            encontradas = List.copyOf(encontradas);
            noEncontradas = List.copyOf(noEncontradas);
        }
    }

    public static ResultadoMapeo mapear(List<String> nombresDetectados,
                                        List<CategoriaRecurso> categoriasSistema) {
        List<CategoriaRecurso> encontradas = new ArrayList<>();
        List<String> noEncontradas = new ArrayList<>();
        if (nombresDetectados == null) {
            return new ResultadoMapeo(encontradas, noEncontradas);
        }
        for (String nombre : nombresDetectados) {
            if (nombre == null || nombre.isBlank()) {
                continue;
            }
            String buscado = nombre.trim();
            CategoriaRecurso coincidencia = categoriasSistema.stream()
                    .filter(categoria -> categoria.getDescripcion().equalsIgnoreCase(buscado))
                    .findFirst()
                    .orElse(null);
            if (coincidencia == null) {
                if (noEncontradas.stream().noneMatch(previo -> previo.equalsIgnoreCase(buscado))) {
                    noEncontradas.add(buscado);
                }
            } else if (!encontradas.contains(coincidencia)) {
                encontradas.add(coincidencia);
            }
        }
        return new ResultadoMapeo(encontradas, noEncontradas);
    }
}
