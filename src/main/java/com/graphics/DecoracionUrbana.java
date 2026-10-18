package com.graphics;

/**
 * Encapsula la decoración urbana procedural para el mapa 11x11:
 * - Parques con árboles y bancos de descanso.
 * - Edificios con ventanas iluminadas por pisos durante la noche.
 * - Delegación de semáforos alternados y pasos de cebra en intersecciones a SistemaSemaforos.
 */
public class DecoracionUrbana {

    /** Dibuja todos los elementos decorativos de la ciudad. */
    public static void dibujar(clase1 renderer, float tiempo, boolean noche) {
        // 1. Manzanas: parques (árboles y bancos) y edificios (ventanas nocturnas)
        for (int fila = 0; fila < MapaCiudad.FILAS; fila++) {
            for (int col = 0; col < MapaCiudad.COLUMNAS; col++) {
                float x = MapaCiudad.centro(col);
                float z = MapaCiudad.centro(fila);
                int tipo = MapaCiudad.MAPA[fila][col];

                if (tipo == 2) {
                    dibujarParque(renderer, x, z);
                } else if (tipo == 1) {
                    float altura = MapaCiudad.getAlturaEdificio(fila, col);
                    dibujarVentanas(renderer, x, z, altura, noche);
                }
            }
        }

        // 2. Pasos de cebra de acera a acera y semáforos alternados en las intersecciones
        SistemaSemaforos.dibujar(renderer, tiempo);

        // 3. Nubes volumétricas esponjosas en el cielo diurno (estilo estilizado idéntico a la captura)
        if (!noche) {
            dibujarNubes(renderer);
        }

        // 4. Señalización vial y tótems de sectores en intersecciones principales
        dibujarSenalizacion(renderer);
    }

    /** Dibuja postes y placas de señalización vial en las esquinas de los cruces principales. */
    private static void dibujarSenalizacion(clase1 renderer) {
        float[][] cruces = {
            {-30.0f, -30.0f},
            { 30.0f, -30.0f},
            {-30.0f,  30.0f},
            { 30.0f,  30.0f},
            {  0.0f,   0.0f}
        };

        for (float[] cruce : cruces) {
            float px = cruce[0] + 6.8f; // Desplazado respecto al semáforo (que está en 5.2) para evitar que se entrecrucen
            float pz = cruce[1] + 5.2f;

            // Poste metálico esbelto
            renderer.caja(px, 2.0f, pz, 0.14f, 4.0f, 0.14f, 0.32f, 0.36f, 0.40f);

            // Placa superior de avenida (azul reflectante con borde dorado)
            renderer.caja(px, 3.8f, pz, 2.0f, 0.40f, 0.08f, 0.10f, 0.38f, 0.82f);
            renderer.caja(px, 3.8f, pz, 1.9f, 0.32f, 0.09f, 0.98f, 0.92f, 0.35f);

            // Placa transversal de sector (verde vial con borde blanco)
            renderer.caja(px, 3.3f, pz, 0.08f, 0.36f, 1.9f, 0.12f, 0.58f, 0.32f);
            renderer.caja(px, 3.3f, pz, 0.09f, 0.28f, 1.8f, 0.98f, 0.98f, 1.0f);
        }
    }

    /** Dibuja cúmulos de nubes blancas esponjosas flotando en el cielo diurno. */
    private static void dibujarNubes(clase1 renderer) {
        float[][] nubes = {
            {-32.0f, 33.0f, -52.0f},
            { 28.0f, 35.0f, -54.0f},
            { 48.0f, 32.0f, -18.0f},
            {-50.0f, 34.0f,  12.0f},
            { 34.0f, 36.0f,  46.0f},
            {-28.0f, 37.0f,  50.0f},
            {  0.0f, 40.0f, -62.0f}
        };

        renderer.entero("uEmision", 1); // Emisión activa para que brillen como blanco puro en el cielo azul
        for (float[] nube : nubes) {
            float cx = nube[0];
            float cy = nube[1];
            float cz = nube[2];
            // Bloque central esponjoso
            renderer.caja(cx, cy, cz, 9.5f, 3.2f, 5.0f, 0.98f, 0.99f, 1.0f);
            // Lóbulo lateral izquierdo
            renderer.caja(cx - 3.8f, cy - 0.4f, cz + 0.6f, 6.0f, 2.4f, 4.2f, 0.95f, 0.97f, 0.99f);
            // Lóbulo lateral derecho
            renderer.caja(cx + 3.8f, cy - 0.3f, cz - 0.5f, 6.2f, 2.5f, 4.4f, 0.95f, 0.97f, 0.99f);
            // Cúpula superior
            renderer.caja(cx + 0.6f, cy + 1.4f, cz, 5.4f, 2.0f, 3.8f, 1.0f, 1.0f, 1.0f);
        }
        renderer.entero("uEmision", 0);
    }

    /** Dibuja 4 árboles y un banco en la parcela del parque. */
    private static void dibujarParque(clase1 renderer, float x, float z) {
        float[] offsets = {-2.6f, 2.6f};
        for (float ox : offsets) {
            for (float oz : offsets) {
                float arbolX = x + ox;
                float arbolZ = z + oz;
                // Tronco marrón
                renderer.caja(arbolX, 1.2f, arbolZ, 0.40f, 2.0f, 0.40f, 0.38f, 0.22f, 0.12f);
                // Copa verde
                renderer.caja(arbolX, 2.7f, arbolZ, 2.1f, 2.2f, 2.1f, 0.14f, 0.45f, 0.22f);
            }
        }
        // Banco de madera
        renderer.caja(x, 0.65f, z, 3.0f, 0.25f, 0.8f, 0.55f, 0.30f, 0.13f);
        renderer.caja(x, 0.40f, z, 2.0f, 0.60f, 0.35f, 0.22f, 0.24f, 0.24f);
        renderer.caja(x, 1.00f, z + 0.35f, 3.0f, 0.70f, 0.15f, 0.55f, 0.30f, 0.13f);
    }

    /** Distribuye ventanas en las 4 fachadas del edificio, con emisión nocturna. */
    private static void dibujarVentanas(clase1 renderer, float x, float z, float altura, boolean noche) {
        if (noche) {
            renderer.entero("uEmision", 1);
        }
        for (float y = 1.8f; y < altura - 0.5f; y += 2.0f) {
            for (float desp = -2.2f; desp <= 2.2f; desp += 2.2f) {
                // Fachada norte y sur
                renderer.caja(x + desp, y, z - 3.61f, 0.85f, 0.95f, 0.04f, 0.95f, 0.78f, 0.38f);
                renderer.caja(x + desp, y, z + 3.61f, 0.85f, 0.95f, 0.04f, 0.95f, 0.78f, 0.38f);
                // Fachada oeste y este
                renderer.caja(x - 3.61f, y, z + desp, 0.04f, 0.95f, 0.85f, 0.95f, 0.78f, 0.38f);
                renderer.caja(x + 3.61f, y, z + desp, 0.04f, 0.95f, 0.85f, 0.95f, 0.78f, 0.38f);
            }
        }
        renderer.entero("uEmision", 0);
    }
}
