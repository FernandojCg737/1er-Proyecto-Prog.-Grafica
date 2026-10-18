package com.graphics;

/**
 * Controla el sistema de juego de entregas en la ciudad ampliada 11x11:
 * - Paradas en diferentes sectores conectados por la red vial.
 * - Baliza dorada flotante animada y marca en el asfalto.
 * - Detección de cercanía (< 3 m) y frenado (< 1 m/s) para completar cada parada.
 * - Cronómetro y estado de victoria.
 */
public class SistemaEntregas {

    public static final float[][] DESTINOS = {
        { 30, -30}, // Parada 1: Sector Noreste
        {-30, -30}, // Parada 2: Sector Noroeste
        {-30,  30}, // Parada 3: Sector Suroeste
        { 30,  30}  // Parada 4: Sector Sureste (meta final)
    };

    public int entregasCompletadas = 0;
    public float tiempo = 0.0f;

    public void reiniciar() {
        entregasCompletadas = 0;
        tiempo = 0.0f;
    }

    public boolean esVictoria() {
        return entregasCompletadas >= DESTINOS.length;
    }

    public void actualizar(float deltaTime, float autoX, float autoZ, float velocidad) {
        if (esVictoria()) {
            return; // Se detiene el cronómetro al completar todas las entregas
        }

        tiempo += deltaTime;

        float destX = DESTINOS[entregasCompletadas][0];
        float destZ = DESTINOS[entregasCompletadas][1];
        float dx = autoX - destX;
        float dz = autoZ - destZ;
        float distCuadrada = dx * dx + dz * dz;

        boolean cerca = distCuadrada < 3.0f * 3.0f;
        boolean frenando = Math.abs(velocidad) < 1.0f;

        if (cerca && frenando) {
            entregasCompletadas++;
        }
    }

    /** Dibuja la baliza flotante y la plataforma sobre el asfalto. */
    public void dibujar(clase1 renderer, boolean vistaMapa) {
        if (esVictoria()) {
            return;
        }

        float x = DESTINOS[entregasCompletadas][0];
        float z = DESTINOS[entregasCompletadas][1];

        renderer.entero("uEmision", 1);
        // Marca dorada en el suelo
        renderer.caja(x, 0.06f, z, 5.0f, 0.08f, 5.0f, 1.0f, 0.72f, 0.12f);

        // Baliza tridimensional flotante (solo en la vista principal)
        if (!vistaMapa) {
            float altura = 3.5f + (float) Math.sin(tiempo * 2.5f) * 0.35f;
            renderer.cajaGirada(x, altura, z, 0.85f, 0.85f, 0.85f, 1.0f, 0.82f, 0.15f, tiempo);
        }
        renderer.entero("uEmision", 0);
    }

    public String getEstadoTexto() {
        if (esVictoria()) {
            return " | GANASTE en " + (int) tiempo + " s! (R: reiniciar)";
        }
        return " | Entregas " + entregasCompletadas + "/" + DESTINOS.length +
               " | Frena en marca dorada | " + (int) tiempo + " s";
    }
}
