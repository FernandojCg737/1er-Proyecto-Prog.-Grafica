package com.graphics;

/**
 * Controla el sistema integral de semáforos y pasos de cebra:
 * - Ubicados EXCLUSIVAMENTE en las intersecciones interiores entre manzanas (-30, -10, 10, 30).
 * - En los bordes del mapa (-50 y +50) NO se dibujan semáforos ni pasos de cebra.
 * - Ciclo alternado de 2 fases: mientras Norte-Sur está en VERDE/AMARILLO, Este-Oeste está en ROJO y viceversa.
 * - Pasos de cebra de acera a acera antes del cruce con franjas longitudinales.
 * - Semáforos montados firmemente sobre las aceras de cada esquina, apuntando a los conductores.
 */
public class SistemaSemaforos {

    public enum EstadoLuz {
        VERDE, AMARILLO, ROJO
    }

    // Coordenadas de las intersecciones INTERIORES entre manzanas (-30, -10, 10, 30).
    // Las avenidas perimetrales (-50 y +50) son el borde exterior del mapa y NO tienen semáforos ni pasos de cebra.
    public static final float[] INTERSECCIONES_INTERIORES = {-30, -10, 10, 30};

    // Duración del ciclo completo: 14 segundos
    // 0s a 5s:  Norte-Sur VERDE,    Este-Oeste ROJO
    // 5s a 7s:  Norte-Sur AMARILLO, Este-Oeste ROJO
    // 7s a 12s: Norte-Sur ROJO,     Este-Oeste VERDE
    // 12s a 14s:Norte-Sur ROJO,     Este-Oeste AMARILLO
    public static final float CICLO_TOTAL = 14.0f;

    /** Obtiene el estado del semáforo para el tráfico que circula en el eje Norte-Sur (Z). */
    public static EstadoLuz getEstadoNorteSur(float tiempo) {
        float fase = (tiempo >= 0 ? tiempo : 0) % CICLO_TOTAL;
        if (fase < 5.0f) {
            return EstadoLuz.VERDE;
        } else if (fase < 7.0f) {
            return EstadoLuz.AMARILLO;
        } else {
            return EstadoLuz.ROJO;
        }
    }

    /** Obtiene el estado del semáforo para el tráfico que circula en el eje Este-Oeste (X). */
    public static EstadoLuz getEstadoEsteOeste(float tiempo) {
        float fase = (tiempo >= 0 ? tiempo : 0) % CICLO_TOTAL;
        if (fase < 7.0f) {
            return EstadoLuz.ROJO;
        } else if (fase < 12.0f) {
            return EstadoLuz.VERDE;
        } else {
            return EstadoLuz.AMARILLO;
        }
    }

    /**
     * Evalúa si un vehículo aproximándose a una intersección interior debe frenar o detenerse por semáforo.
     * Retorna un factor de velocidad multiplicador (0.0 = detención total, 0.4 = precaución amarillo, 1.0 = verde).
     */
    public static float evaluarSemaforo(float x, float z, float frenteX, float frenteZ, float tiempo) {
        boolean esNorteSur = Math.abs(frenteZ) > Math.abs(frenteX);
        EstadoLuz estado = esNorteSur ? getEstadoNorteSur(tiempo) : getEstadoEsteOeste(tiempo);

        if (estado == EstadoLuz.VERDE) {
            return 1.0f; // Vía libre
        }

        // Buscar la próxima intersección interior al frente
        for (float avenida : INTERSECCIONES_INTERIORES) {
            float distAlCruce;
            if (esNorteSur) {
                distAlCruce = (avenida - z) * (frenteZ > 0 ? 1 : -1);
            } else {
                distAlCruce = (avenida - x) * (frenteX > 0 ? 1 : -1);
            }

            // Si el vehículo está en la zona de frenado antes del paso de cebra (entre 4.2m y 12.0m antes del cruce)
            if (distAlCruce > 4.2f && distAlCruce < 12.5f) {
                if (estado == EstadoLuz.ROJO) {
                    if (distAlCruce < 6.8f) {
                        return 0.0f; // Alto total
                    } else {
                        return (distAlCruce - 6.8f) / (12.5f - 6.8f);
                    }
                } else if (estado == EstadoLuz.AMARILLO) {
                    return 0.35f; // Reducción de velocidad en amarillo
                }
            }
        }

        return 1.0f;
    }

    /** Dibuja los pasos de cebra y semáforos únicamente en las intersecciones interiores. */
    public static void dibujar(clase1 renderer, float tiempo) {
        EstadoLuz luzNS = getEstadoNorteSur(tiempo);
        EstadoLuz luzEO = getEstadoEsteOeste(tiempo);

        // Recorrer ÚNICAMENTE las intersecciones interiores entre manzanas
        for (float ix : INTERSECCIONES_INTERIORES) {
            for (float iz : INTERSECCIONES_INTERIORES) {
                dibujarPasosCebraInterseccion(renderer, ix, iz);
                dibujarSemaforosInterseccion(renderer, ix, iz, luzNS, luzEO);
            }
        }
    }

    /**
     * Dibuja los pasos de cebra en los 4 accesos de la intersección.
     * Cruzan la calle de acera a acera (de -4.2 a +4.2 m) con franjas longitudinales.
     */
    private static void dibujarPasosCebraInterseccion(clase1 renderer, float ix, float iz) {
        float r = 0.92f, g = 0.94f, b = 0.95f; // Blanco reflectante

        // 1. Acceso Norte (cruce en Z = iz - 5.6m, entre esquinas de aceras en X)
        for (float ox = -3.8f; ox <= 3.8f; ox += 0.95f) {
            renderer.caja(ix + ox, 0.035f, iz - 5.6f, 0.48f, 0.035f, 2.2f, r, g, b);
        }

        // 2. Acceso Sur (cruce en Z = iz + 5.6m)
        for (float ox = -3.8f; ox <= 3.8f; ox += 0.95f) {
            renderer.caja(ix + ox, 0.035f, iz + 5.6f, 0.48f, 0.035f, 2.2f, r, g, b);
        }

        // 3. Acceso Oeste (cruce en X = ix - 5.6m, entre esquinas de aceras en Z)
        for (float oz = -3.8f; oz <= 3.8f; oz += 0.95f) {
            renderer.caja(ix - 5.6f, 0.035f, iz + oz, 2.2f, 0.035f, 0.48f, r, g, b);
        }

        // 4. Acceso Este (cruce en X = ix + 5.6m)
        for (float oz = -3.8f; oz <= 3.8f; oz += 0.95f) {
            renderer.caja(ix + 5.6f, 0.035f, iz + oz, 2.2f, 0.035f, 0.48f, r, g, b);
        }
    }

    private static void dibujarSemaforosInterseccion(clase1 renderer, float ix, float iz,
                                                      EstadoLuz luzNS, EstadoLuz luzEO) {
        // 1. Esquina Noroeste
        dibujarCabinaSemaforo(renderer, ix - 5.2f, iz - 5.2f, luzNS, 0, -1);

        // 2. Esquina Sureste
        dibujarCabinaSemaforo(renderer, ix + 5.2f, iz + 5.2f, luzNS, 0, 1);

        // 3. Esquina Suroeste
        dibujarCabinaSemaforo(renderer, ix - 5.2f, iz + 5.2f, luzEO, -1, 0);

        // 4. Esquina Noreste
        dibujarCabinaSemaforo(renderer, ix + 5.2f, iz - 5.2f, luzEO, 1, 0);
    }

    /** Construye un semáforo 3D montado sobre la acera y orientado hacia el tráfico. */
    private static void dibujarCabinaSemaforo(clase1 renderer, float x, float z, EstadoLuz estado,
                                              int dirX, int dirZ) {
        // Poste sobre la acera (acera a y=0.3, poste de 3.0m centrado en y=1.8, descansa en y=0.3)
        renderer.caja(x, 1.8f, z, 0.18f, 3.0f, 0.18f, 0.18f, 0.20f, 0.22f);

        // Carcasa principal
        float dimX = (dirX != 0) ? 0.42f : 0.55f;
        float dimZ = (dirZ != 0) ? 0.42f : 0.55f;
        renderer.caja(x, 3.3f, z, dimX, 1.25f, dimZ, 0.08f, 0.10f, 0.12f);

        // Desplazamiento frontal de las bombillas
        float despX = dirX * 0.23f;
        float despZ = dirZ * 0.23f;
        float bTamX = (dirX != 0) ? 0.06f : 0.28f;
        float bTamZ = (dirZ != 0) ? 0.06f : 0.28f;

        // 3 bombillas: Roja (arriba), Amarilla (centro), Verde (abajo)
        for (int i = 0; i < 3; i++) {
            boolean activa = false;
            if (i == 0 && estado == EstadoLuz.ROJO) activa = true;
            if (i == 1 && estado == EstadoLuz.AMARILLO) activa = true;
            if (i == 2 && estado == EstadoLuz.VERDE) activa = true;

            float brillo = activa ? 1.0f : 0.12f;
            renderer.entero("uEmision", activa ? 1 : 0);

            float r = (i < 2) ? brillo : 0.0f;
            float g = (i > 0) ? brillo : 0.0f;
            float b = 0.02f;

            float yLuz = 3.68f - i * 0.36f;
            renderer.caja(x + despX, yLuz, z + despZ, bTamX, 0.24f, bTamZ, r, g, b);
        }
        renderer.entero("uEmision", 0);
    }
}
