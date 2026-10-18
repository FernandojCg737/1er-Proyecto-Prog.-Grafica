package com.graphics;

/**
 * Administra el sistema de iluminación de la ciudad ampliada:
 * - Ciclo día/noche conmutable por tecla N.
 * - 16 farolas distribuidas en las nuevas calles y sectores (supera el mínimo de 9).
 * - Focos delanteros del vehículo conmutables por tecla F.
 * - Control de emisión de bombillas vs iluminación del entorno.
 */
public class SistemaIluminacion {

    public boolean noche = true;
    public boolean faros = true;

    // Coordenadas XYZ de las 16 farolas distribuidas en las aceras de la ciudad 11x11
    public static final float[][] FAROLAS = {
        {-44, 4.5f, -44}, {-24, 4.5f, -44}, {16, 4.5f, -44}, {36, 4.5f, -44},
        {-44, 4.5f, -24}, {-24, 4.5f, -24}, {16, 4.5f, -24}, {36, 4.5f, -24},
        {-44, 4.5f,  16}, {-24, 4.5f,  16}, {16, 4.5f,  16}, {36, 4.5f,  16},
        {-44, 4.5f,  36}, {-24, 4.5f,  36}, {16, 4.5f,  36}, {36, 4.5f,  36}
    };

    public void alternarNoche() {
        noche = !noche;
    }

    public void alternarFaros() {
        faros = !faros;
    }

    /** Envía las posiciones y parámetros de iluminación a los uniforms de GLSL. */
    public void prepararUniforms(clase1 renderer, Vehiculo auto) {
        renderer.entero("uNoche", noche ? 1 : 0);
        renderer.entero("uFaros", faros ? 1 : 0);
        renderer.entero("uEmision", 0);

        for (int i = 0; i < FAROLAS.length; i++) {
            renderer.vector("uLuces[" + i + "]", FAROLAS[i][0], FAROLAS[i][1], FAROLAS[i][2]);
        }

        float frenteX = -(float) Math.sin(auto.angulo);
        float frenteZ = -(float) Math.cos(auto.angulo);
        renderer.vector("uAuto", auto.x, 0.7f, auto.z);
        renderer.vector("uFrente", frenteX, 0, frenteZ);
    }

    /** Dibuja los postes y las bombillas (emisivas de noche). */
    public void dibujarFarolas(clase1 renderer) {
        for (float[] farola : FAROLAS) {
            float x = farola[0];
            float y = farola[1];
            float z = farola[2];

            // Poste de la farola
            renderer.caja(x, 2.2f, z, 0.18f, 4.4f, 0.18f, 0.20f, 0.24f, 0.28f);

            // Bombilla: emisiva si es de noche
            if (noche) {
                renderer.entero("uEmision", 1);
            }
            renderer.caja(x, y, z, 0.7f, 0.35f, 0.7f, 1.0f, 0.83f, 0.42f);
            renderer.entero("uEmision", 0);
        }
    }

    public String getEstadoTexto() {
        return " | N: " + (noche ? "noche" : "dia") + " | F: faros " + (faros ? "ON" : "OFF");
    }
}
