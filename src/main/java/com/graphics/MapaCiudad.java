package com.graphics;

/**
 * Encapsula la estructura del mapa ampliado (11 x 11 celdas),
 * el cálculo de coordenadas mundiales y el sistema de colisiones.
 * Cumple con el Requisito 1 del enunciado oficial:
 * - 11x11 celdas de 10 unidades cada una (área total 110 x 110).
 * - Más de 12 manzanas con edificios (19 en total).
 * - Más de 4 parques (6 en total).
 * - Red vial completamente interconectada y transitable.
 */
public class MapaCiudad {

    public static final int FILAS = 11;
    public static final int COLUMNAS = 11;
    public static final float CELDA = 10.0f;
    public static final float LIMITE = 55.0f; // -55 a +55 unidades

    // 0 = Calle, 1 = Edificio, 2 = Parque
    public static final int[][] MAPA = {
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Fila 0: Avenida Norte (-50)
        {0, 1, 0, 1, 0, 2, 0, 1, 0, 1, 0}, // Fila 1: Manzanas (-40) -> 4 edificios, 1 parque
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Fila 2: Avenida (-30)
        {0, 1, 0, 2, 0, 1, 0, 1, 0, 1, 0}, // Fila 3: Manzanas (-20) -> 4 edificios, 1 parque
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Fila 4: Avenida (-10)
        {0, 1, 0, 1, 0, 1, 0, 2, 0, 1, 0}, // Fila 5: Manzanas central (0) -> 4 edificios, 1 parque
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Fila 6: Avenida (+10)
        {0, 1, 0, 2, 0, 1, 0, 2, 0, 1, 0}, // Fila 7: Manzanas (+20) -> 3 edificios, 2 parques
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Fila 8: Avenida (+30)
        {0, 1, 0, 1, 0, 2, 0, 1, 0, 1, 0}, // Fila 9: Manzanas (+40) -> 4 edificios, 1 parque
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}  // Fila 10: Avenida Sur (+50)
    };

    /** Convierte el índice de fila o columna al centro de su celda en el mundo. */
    public static float centro(int indice) {
        return -LIMITE + CELDA * (indice + 0.5f);
    }

    /** Obtiene la altura del edificio en la posición de celda dada. */
    public static float getAlturaEdificio(int fila, int columna) {
        return 6.0f + (fila * 4 + columna * 7) % 11; // Alturas variadas entre 6 y 16 unidades
    }

    /** Comprueba si una posición circular colisiona con manzanas o bordes. */
    public static boolean puedeCircular(float x, float z, float radio) {
        float limitePermitido = LIMITE - radio;
        if (Math.abs(x) > limitePermitido || Math.abs(z) > limitePermitido) {
            return false;
        }

        for (int fila = 0; fila < FILAS; fila++) {
            for (int col = 0; col < COLUMNAS; col++) {
                if (MAPA[fila][col] == 0) {
                    continue; // Calle transitable
                }

                float centroX = centro(col);
                float centroZ = centro(fila);
                // La manzana mide 10x10, centrada en (centroX, centroZ)
                float cercaX = Math.max(centroX - 5.0f, Math.min(x, centroX + 5.0f));
                float cercaZ = Math.max(centroZ - 5.0f, Math.min(z, centroZ + 5.0f));
                float dx = x - cercaX;
                float dz = z - cercaZ;

                if (dx * dx + dz * dz < radio * radio) {
                    return false; // Colisión detectada con la manzana
                }
            }
        }
        return true;
    }

    /** Dibuja la base de asfalto y todas las parcelas (aceras, edificios y parques). */
    public static void dibujar(clase1 renderer) {
        // Asfalto base que cubre toda la ciudad ampliada (110 x 110)
        renderer.caja(0, -0.25f, 0, LIMITE * 2, 0.5f, LIMITE * 2, 0.16f, 0.19f, 0.23f);

        for (int fila = 0; fila < FILAS; fila++) {
            for (int col = 0; col < COLUMNAS; col++) {
                float x = centro(col);
                float z = centro(fila);
                int tipo = MAPA[fila][col];

                if (tipo == 0) {
                    dibujarMarcasCalle(renderer, fila, col, x, z);
                } else {
                    // Acera elevada en toda la manzana
                    renderer.caja(x, 0.15f, z, 10, 0.3f, 10, 0.60f, 0.64f, 0.66f);

                    if (tipo == 1) {
                        // Edificio
                        float altura = getAlturaEdificio(fila, col);
                        float rojo = 0.25f + (col * 0.05f) % 0.45f;
                        float azul = 0.40f + (fila * 0.04f) % 0.40f;
                        float verde = 0.35f + ((fila + col) * 0.03f) % 0.25f;

                        // Cuerpo del edificio
                        renderer.caja(x, altura / 2 + 0.3f, z, 7.2f, altura, 7.2f, rojo, verde, azul);
                        // Cornisa o cubierta superior
                        renderer.caja(x, altura + 0.45f, z, 7.6f, 0.3f, 7.6f, 0.20f, 0.26f, 0.32f);
                    } else if (tipo == 2) {
                        // Parque: césped verde sobre la acera
                        renderer.caja(x, 0.32f, z, 9.2f, 0.1f, 9.2f, 0.18f, 0.46f, 0.25f);
                    }
                }
            }
        }
    }

    /** Dibuja marcas viales discontinuas entre cruces. */
    private static void dibujarMarcasCalle(clase1 renderer, int fila, int col, float x, float z) {
        // Tramos horizontales entre cruces (fila par, columna impar)
        if (fila % 2 == 0 && col % 2 == 1) {
            for (int d = -3; d <= 3; d += 3) {
                renderer.caja(x + d, 0.025f, z, 1.6f, 0.03f, 0.14f, 1.0f, 0.84f, 0.35f);
            }
        }
        // Tramos verticales entre cruces (columna par, fila impar)
        if (col % 2 == 0 && fila % 2 == 1) {
            for (int d = -3; d <= 3; d += 3) {
                renderer.caja(x, 0.025f, z + d, 0.14f, 0.03f, 1.6f, 1.0f, 0.84f, 0.35f);
            }
        }
    }
}
