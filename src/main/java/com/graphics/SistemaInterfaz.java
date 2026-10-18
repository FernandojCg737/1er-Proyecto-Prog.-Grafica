package com.graphics;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

/**
 * Controla la interfaz gráfica (HUD) de conducción y el menú interactivo de pausa.
 * Características:
 * - Doble cuadro de mandos analógico de auto real:
 *     1. Tacómetro (RPM / Régimen de acelerada del motor) con zona roja y aguja giratoria dinámica.
 *     2. Velocímetro (Velocidad por hora km/h) con aguja analógica y display digital de 7 segmentos.
 * - Consola central con testigos luminosos de Faros, Modo Día/Noche, Marcha (P/D/R) y Bocina.
 * - Cinta inferior con indicador de sector urbano actual.
 * - Menú de pausa profesional (teclas P o TAB) con renderizado tipográfico 3x5 y teclas 3D estilizadas.
 */
public class SistemaInterfaz {

    // ==================== 1. FUENTE VECTORIAL 3x5 ====================
    private static final int[] FUENTE_3X5 = new int[128];
    static {
        // Cada glifo ocupa 15 bits: 5 filas de 3 bits (fila 0: bits 14..12 ... fila 4: bits 2..0)
        FUENTE_3X5[' '] = 0;
        FUENTE_3X5['!'] = glifo(2, 2, 2, 0, 2);
        FUENTE_3X5['('] = glifo(2, 4, 4, 4, 2);
        FUENTE_3X5[')'] = glifo(2, 1, 1, 1, 2);
        FUENTE_3X5['+'] = glifo(0, 2, 7, 2, 0);
        FUENTE_3X5['-'] = glifo(0, 0, 7, 0, 0);
        FUENTE_3X5['.'] = glifo(0, 0, 0, 0, 2);
        FUENTE_3X5['/'] = glifo(1, 1, 2, 4, 4);
        FUENTE_3X5[':'] = glifo(0, 2, 0, 2, 0);
        FUENTE_3X5['['] = glifo(6, 4, 4, 4, 6);
        FUENTE_3X5[']'] = glifo(3, 1, 1, 1, 3);

        // Dígitos 0-9
        FUENTE_3X5['0'] = glifo(7, 5, 5, 5, 7);
        FUENTE_3X5['1'] = glifo(2, 6, 2, 2, 7);
        FUENTE_3X5['2'] = glifo(7, 1, 7, 4, 7);
        FUENTE_3X5['3'] = glifo(7, 1, 7, 1, 7);
        FUENTE_3X5['4'] = glifo(5, 5, 7, 1, 1);
        FUENTE_3X5['5'] = glifo(7, 4, 7, 1, 7);
        FUENTE_3X5['6'] = glifo(7, 4, 7, 5, 7);
        FUENTE_3X5['7'] = glifo(7, 1, 1, 1, 1);
        FUENTE_3X5['8'] = glifo(7, 5, 7, 5, 7);
        FUENTE_3X5['9'] = glifo(7, 5, 7, 1, 7);

        // Letras mayúsculas A-Z
        FUENTE_3X5['A'] = glifo(2, 5, 7, 5, 5);
        FUENTE_3X5['B'] = glifo(6, 5, 6, 5, 6);
        FUENTE_3X5['C'] = glifo(7, 4, 4, 4, 7);
        FUENTE_3X5['D'] = glifo(6, 5, 5, 5, 6);
        FUENTE_3X5['E'] = glifo(7, 4, 6, 4, 7);
        FUENTE_3X5['F'] = glifo(7, 4, 6, 4, 4);
        FUENTE_3X5['G'] = glifo(7, 4, 5, 5, 7);
        FUENTE_3X5['H'] = glifo(5, 5, 7, 5, 5);
        FUENTE_3X5['I'] = glifo(7, 2, 2, 2, 7);
        FUENTE_3X5['J'] = glifo(1, 1, 1, 5, 2);
        FUENTE_3X5['K'] = glifo(5, 6, 4, 6, 5);
        FUENTE_3X5['L'] = glifo(4, 4, 4, 4, 7);
        FUENTE_3X5['M'] = glifo(5, 7, 7, 5, 5);
        FUENTE_3X5['N'] = glifo(5, 7, 5, 5, 5);
        FUENTE_3X5['O'] = glifo(7, 5, 5, 5, 7);
        FUENTE_3X5['P'] = glifo(7, 5, 7, 4, 4);
        FUENTE_3X5['Q'] = glifo(7, 5, 5, 7, 1);
        FUENTE_3X5['R'] = glifo(7, 5, 6, 6, 5);
        FUENTE_3X5['S'] = glifo(7, 4, 7, 1, 7);
        FUENTE_3X5['T'] = glifo(7, 2, 2, 2, 2);
        FUENTE_3X5['U'] = glifo(5, 5, 5, 5, 7);
        FUENTE_3X5['V'] = glifo(5, 5, 5, 5, 2);
        FUENTE_3X5['W'] = glifo(5, 5, 7, 7, 5);
        FUENTE_3X5['X'] = glifo(5, 5, 2, 5, 5);
        FUENTE_3X5['Y'] = glifo(5, 5, 2, 2, 2);
        FUENTE_3X5['Z'] = glifo(7, 1, 2, 4, 7);
    }

    private static int glifo(int f0, int f1, int f2, int f3, int f4) {
        return (f0 << 12) | (f1 << 9) | (f2 << 6) | (f3 << 3) | f4;
    }

    /** Estado persistente del tacómetro (RPM actual simulado con inercia de motor). */
    private static float rpmActual = 1000f;

    // ==================== 2. SECTOR URBANO ====================

    /** Obtiene el nombre del sector urbano según las coordenadas mundiales del auto. */
    public static String obtenerSector(float x, float z) {
        if (x < -15 && z < -15) return "Sector Noroeste - Residencial";
        if (x > 15 && z < -15) return "Sector Noreste - Financiero";
        if (x < -15 && z > 15) return "Sector Suroeste - Comercial";
        if (x > 15 && z > 15) return "Sector Sureste - Industrial";
        if (Math.abs(x) <= 22 && Math.abs(z) <= 22) {
            if ((Math.abs(x + 20) < 6 && Math.abs(z + 20) < 6) ||
                (Math.abs(x - 20) < 6 && Math.abs(z + 20) < 6) ||
                (Math.abs(x + 20) < 6 && Math.abs(z - 20) < 6)) {
                return "Distrito Verde - Parque Central";
            }
            return "Distrito Centro - Cívico";
        }
        return "Avenida Perimetral";
    }

    // ==================== 3. RENDERIZADO TIPOGRÁFICO Y LED ====================

    /** Dibuja texto en pantalla en mayúsculas utilizando la fuente procedural 3x5. */
    public static void dibujarTexto(clase1 renderer, String texto, float x, float y,
                                    float anchoChar, float altoChar, float espaciado,
                                    float r, float g, float b) {
        if (texto == null || texto.isEmpty()) return;
        float curX = x;
        float px = anchoChar / 3.0f;
        float py = altoChar / 5.0f;

        for (int i = 0; i < texto.length(); i++) {
            char ch = Character.toUpperCase(texto.charAt(i));
            if (ch < FUENTE_3X5.length) {
                int mask = FUENTE_3X5[ch];
                if (mask != 0) {
                    for (int row = 0; row < 5; row++) {
                        int rowBits = (mask >> ((4 - row) * 3)) & 7;
                        float cyRow = y + (2 - row) * py;

                        if (rowBits == 7) {
                            renderer.caja(curX + px, cyRow, 0.06f, anchoChar, py * 0.88f, 0.01f, r, g, b);
                        } else if (rowBits == 6) {
                            renderer.caja(curX + px * 0.5f, cyRow, 0.06f, px * 2.0f, py * 0.88f, 0.01f, r, g, b);
                        } else if (rowBits == 3) {
                            renderer.caja(curX + px * 1.5f, cyRow, 0.06f, px * 2.0f, py * 0.88f, 0.01f, r, g, b);
                        } else {
                            if ((rowBits & 4) != 0) renderer.caja(curX, cyRow, 0.06f, px * 0.85f, py * 0.88f, 0.01f, r, g, b);
                            if ((rowBits & 2) != 0) renderer.caja(curX + px, cyRow, 0.06f, px * 0.85f, py * 0.88f, 0.01f, r, g, b);
                            if ((rowBits & 1) != 0) renderer.caja(curX + px * 2, cyRow, 0.06f, px * 0.85f, py * 0.88f, 0.01f, r, g, b);
                        }
                    }
                }
            }
            curX += anchoChar + espaciado;
        }
    }

    /** Dibuja un número digital estilo 7 segmentos LED (usado para la velocidad km/h). */
    public static void dibujarNumero7Seg(clase1 renderer, int numero, float x, float y,
                                         float w, float h, float grosor,
                                         float r, float g, float b) {
        String str = String.valueOf(Math.min(999, Math.max(0, numero)));
        float sep = w * 0.35f;
        float anchoTotal = str.length() * w + (str.length() - 1) * sep;
        float inicioX = x - anchoTotal * 0.5f + w * 0.5f;

        // Tabla 7 segmentos para dígitos 0..9: [a, b, c, d, e, f, g]
        boolean[][] segs = {
            {true, true, true, true, true, true, false},    // 0
            {false, true, true, false, false, false, false}, // 1
            {true, true, false, true, true, false, true},   // 2
            {true, true, true, true, false, false, true},   // 3
            {false, true, true, false, false, true, true},   // 4
            {true, false, true, true, false, true, true},   // 5
            {true, false, true, true, true, true, true},    // 6
            {true, true, true, false, false, false, false}, // 7
            {true, true, true, true, true, true, true},     // 8
            {true, true, true, true, false, true, true}     // 9
        };

        float hw = w * 0.5f;
        float hh = h * 0.5f;

        for (int i = 0; i < str.length(); i++) {
            int d = str.charAt(i) - '0';
            if (d < 0 || d > 9) continue;
            float dx = inicioX + i * (w + sep);
            boolean[] s = segs[d];

            float dimR = r * 0.12f, dimG = g * 0.12f, dimB = b * 0.12f;

            // a: superior
            renderer.caja(dx, y + hh, 0.05f, w, grosor, 0.01f, s[0] ? r : dimR, s[0] ? g : dimG, s[0] ? b : dimB);
            // g: central
            renderer.caja(dx, y, 0.05f, w, grosor, 0.01f, s[6] ? r : dimR, s[6] ? g : dimG, s[6] ? b : dimB);
            // d: inferior
            renderer.caja(dx, y - hh, 0.05f, w, grosor, 0.01f, s[3] ? r : dimR, s[3] ? g : dimG, s[3] ? b : dimB);
            // f: vertical superior izq
            renderer.caja(dx - hw, y + hh * 0.5f, 0.05f, grosor, hh, 0.01f, s[5] ? r : dimR, s[5] ? g : dimG, s[5] ? b : dimB);
            // b: vertical superior der
            renderer.caja(dx + hw, y + hh * 0.5f, 0.05f, grosor, hh, 0.01f, s[1] ? r : dimR, s[1] ? g : dimG, s[1] ? b : dimB);
            // e: vertical inferior izq
            renderer.caja(dx - hw, y - hh * 0.5f, 0.05f, grosor, hh, 0.01f, s[4] ? r : dimR, s[4] ? g : dimG, s[4] ? b : dimB);
            // c: vertical inferior der
            renderer.caja(dx + hw, y - hh * 0.5f, 0.05f, grosor, hh, 0.01f, s[2] ? r : dimR, s[2] ? g : dimG, s[2] ? b : dimB);
        }
    }

    /** Dibuja un disco circular regular mediante cajas rotadas en abanico (16-gono suave). */
    private static void dibujarDisco(clase1 renderer, float cx, float cy, float radio, float z,
                                     float r, float g, float b) {
        float diam = radio * 2.0f;
        float grosor = (float) (diam * Math.tan(Math.PI / 16.0));
        for (int i = 0; i < 8; i++) {
            float ang = (float) (i * Math.PI / 8.0);
            renderer.cajaGirada(cx, cy, z, grosor, diam, 0.01f, r, g, b, ang);
        }
    }

    // ==================== 4. HUD DEPORTIVO CON DOS RELOJES (RPM Y VELOCIDAD) ====================

    /**
     * Dibuja el cuadro de instrumentos de conducción dentro de la ventana:
     * - Reloj Izquierdo: Tacómetro analógico (RPM / régimen de aceleración con aguja y redline).
     * - Reloj Derecho: Velocímetro analógico (km/h con aguja y display digital 7-segmentos).
     * - Puente de testigos luminosos (Faros, Día/Noche, Marcha P/D/R y Bocina).
     * - Distintivo de sector urbano.
     */
    public static void dibujarHUD(clase1 renderer, float velocidad, boolean faros, boolean noche,
                                  boolean bocina, float autoX, float autoZ, boolean frenoMano) {
        glDisable(GL_DEPTH_TEST);
        renderer.entero("uMapa", 2); // Activa el modo 2D directo en pantalla

        try {
            float kmh = Math.abs(velocidad) * 3.6f;

            // Detección de la aceleración del jugador para calcular RPM reactivo
            boolean acelerando = renderer.pulsada(GLFW_KEY_W) || renderer.pulsada(GLFW_KEY_UP);
            boolean retrocediendo = renderer.pulsada(GLFW_KEY_S) || renderer.pulsada(GLFW_KEY_DOWN);
            boolean frenando = frenoMano || renderer.pulsada(GLFW_KEY_SPACE);

            // Dinámica física de RPM de motor
            float rpmObjetivo = 1000f; // Ralentí normal
            if (frenando) {
                rpmObjetivo = 1000f + (kmh / 60.0f) * 600f;
            } else if (acelerando) {
                // Al pisar a fondo, el motor sube hacia 4200 - 7500 RPM dando sensación de potencia
                rpmObjetivo = 3800f + (kmh / 60.0f) * 3500f;
            } else if (retrocediendo) {
                rpmObjetivo = 2600f + (kmh / 25.0f) * 2200f;
            } else {
                // Marcha constante por inercia
                rpmObjetivo = 1000f + (kmh / 60.0f) * 2600f;
            }
            rpmObjetivo = Math.min(7800f, Math.max(900f, rpmObjetivo));

            // Inercia de motor: sube con agilidad al pisar el acelerador, baja suave al soltar
            float factorSuavizado = acelerando ? 0.22f : 0.10f;
            rpmActual += (rpmObjetivo - rpmActual) * factorSuavizado;

            // Centros y radios de ambos relojes
            float cy = -0.72f;
            float cxTaco = -0.74f; // Tacómetro (Izquierda)
            float cxVel = -0.44f;  // Velocímetro (Derecha)
            float radioBezel = 0.145f;
            float radioEsfera = 0.134f;

            // 1. Visera / Carcasa protectora envolvente del tablero de mandos
            renderer.caja(-0.59f, cy - 0.01f, 0.01f, 0.65f, 0.38f, 0.01f, 0.06f, 0.09f, 0.14f);
            renderer.caja(-0.59f, cy + 0.165f, 0.02f, 0.61f, 0.014f, 0.01f, 0.20f, 0.78f, 0.95f); // Borde superior cian neón

            // 2. DISCOS BASE DE AMBOS RELOJES (Bisel metálico + esfera oscura)
            dibujarDisco(renderer, cxTaco, cy, radioBezel, 0.02f, 0.16f, 0.22f, 0.30f);
            dibujarDisco(renderer, cxTaco, cy, radioEsfera, 0.03f, 0.04f, 0.06f, 0.09f);

            dibujarDisco(renderer, cxVel, cy, radioBezel, 0.02f, 0.16f, 0.22f, 0.30f);
            dibujarDisco(renderer, cxVel, cy, radioEsfera, 0.03f, 0.04f, 0.06f, 0.09f);

            // ==================== 3. TACÓMETRO (RPM / ACELERACIÓN) ====================
            // Arco de escala de 270 grados: -135° (0 RPM) a +135° (8000 RPM)
            for (int r = 0; r <= 16; r++) {
                float fraccion = r / 16.0f;
                float angDeg = -135.0f + fraccion * 270.0f;
                float angRad = (float) Math.toRadians(angDeg);
                float dx = (float) Math.sin(angRad);
                float dy = (float) Math.cos(angRad);

                boolean esMayor = (r % 2 == 0);
                float rTick = esMayor ? 0.120f : 0.125f;
                float tx = cxTaco + dx * rTick;
                float ty = cy + dy * rTick;
                float lTick = esMayor ? 0.018f : 0.010f;
                float wTick = esMayor ? 0.005f : 0.003f;

                // Zona Roja (Redline) a partir de 5500 RPM (fracción >= 0.68)
                boolean redline = fraccion >= 0.68f;
                float rCol = redline ? 1.0f : (esMayor ? 0.90f : 0.50f);
                float gCol = redline ? 0.2f : (esMayor ? 0.95f : 0.60f);
                float bCol = redline ? 0.2f : (esMayor ? 1.00f : 0.70f);

                renderer.cajaGirada(tx, ty, 0.04f, wTick, lTick, 0.01f, rCol, gCol, bCol, -angRad);
            }

            // Banda roja en el arco de peligro (Redline 6k - 8k)
            for (int r = 11; r <= 16; r++) {
                float fraccion = r / 16.0f;
                float angDeg = -135.0f + fraccion * 270.0f;
                float angRad = (float) Math.toRadians(angDeg);
                float tx = cxTaco + (float) Math.sin(angRad) * 0.130f;
                float ty = cy + (float) Math.cos(angRad) * 0.130f;
                renderer.cajaGirada(tx, ty, 0.035f, 0.008f, 0.012f, 0.01f, 0.95f, 0.15f, 0.15f, -angRad);
            }

            // Números principales del tacómetro: 0, 2, 4, 6, 8
            dibujarTexto(renderer, "0", cxTaco - 0.076f, cy - 0.070f, 0.010f, 0.013f, 0.002f, 0.85f, 0.90f, 1.0f);
            dibujarTexto(renderer, "2", cxTaco - 0.082f, cy + 0.020f, 0.010f, 0.013f, 0.002f, 0.85f, 0.90f, 1.0f);
            dibujarTexto(renderer, "4", cxTaco - 0.005f, cy + 0.088f, 0.010f, 0.013f, 0.002f, 0.85f, 0.90f, 1.0f);
            dibujarTexto(renderer, "6", cxTaco + 0.070f, cy + 0.020f, 0.010f, 0.013f, 0.002f, 1.0f, 0.35f, 0.35f);
            dibujarTexto(renderer, "8", cxTaco + 0.066f, cy - 0.070f, 0.010f, 0.013f, 0.002f, 1.0f, 0.25f, 0.25f);

            // Rótulo central de revoluciones
            dibujarTexto(renderer, "RPM", cxTaco - 0.018f, cy + 0.038f, 0.011f, 0.014f, 0.002f, 0.30f, 0.85f, 1.0f);
            dibujarTexto(renderer, "X1000", cxTaco - 0.026f, cy - 0.046f, 0.009f, 0.012f, 0.002f, 0.65f, 0.75f, 0.85f);

            // AGUJA ANALÓGICA DEL TACÓMETRO (Naranja fuego deportivo)
            float fracRpm = Math.min(1.05f, Math.max(0f, rpmActual / 8000.0f));
            float angRpmDeg = -135.0f + fracRpm * 270.0f;
            float angRpmRad = (float) Math.toRadians(angRpmDeg);
            float dirRpmX = (float) Math.sin(angRpmRad);
            float dirRpmY = (float) Math.cos(angRpmRad);
            float lAguja = 0.098f;

            // Cuerpo y punta de la aguja del tacómetro
            renderer.cajaGirada(cxTaco + dirRpmX * lAguja * 0.5f, cy + dirRpmY * lAguja * 0.5f, 0.05f,
                                0.006f, lAguja, 0.01f, 1.0f, 0.40f, 0.10f, -angRpmRad);
            renderer.cajaGirada(cxTaco + dirRpmX * lAguja * 0.85f, cy + dirRpmY * lAguja * 0.85f, 0.055f,
                                0.004f, lAguja * 0.3f, 0.01f, 1.0f, 1.0f, 1.0f, -angRpmRad);
            dibujarDisco(renderer, cxTaco, cy, 0.022f, 0.06f, 0.18f, 0.22f, 0.28f);
            dibujarDisco(renderer, cxTaco, cy, 0.011f, 0.065f, 0.85f, 0.90f, 0.95f);

            // ==================== 4. VELOCÍMETRO (VELOCIDAD POR HORA KM/H) ====================
            // Arco de escala de 270 grados: -135° (0 km/h) a +135° (60 km/h)
            for (int k = 0; k <= 12; k++) {
                float fraccion = k / 12.0f;
                float angDeg = -135.0f + fraccion * 270.0f;
                float angRad = (float) Math.toRadians(angDeg);
                float dx = (float) Math.sin(angRad);
                float dy = (float) Math.cos(angRad);

                boolean esMayor = (k % 2 == 0);
                float rTick = esMayor ? 0.120f : 0.125f;
                float tx = cxVel + dx * rTick;
                float ty = cy + dy * rTick;
                float lTick = esMayor ? 0.018f : 0.010f;
                float wTick = esMayor ? 0.005f : 0.003f;

                boolean altaVel = fraccion >= 0.80f;
                float rCol = altaVel ? 1.0f : (esMayor ? 0.90f : 0.50f);
                float gCol = altaVel ? 0.6f : (esMayor ? 0.95f : 0.60f);
                float bCol = altaVel ? 0.2f : (esMayor ? 1.00f : 0.70f);

                renderer.cajaGirada(tx, ty, 0.04f, wTick, lTick, 0.01f, rCol, gCol, bCol, -angRad);
            }

            // Números principales del velocímetro: 0, 20, 40, 60
            dibujarTexto(renderer, "0", cxVel - 0.076f, cy - 0.070f, 0.010f, 0.013f, 0.002f, 0.85f, 0.90f, 1.0f);
            dibujarTexto(renderer, "20", cxVel - 0.090f, cy + 0.020f, 0.009f, 0.012f, 0.002f, 0.85f, 0.90f, 1.0f);
            dibujarTexto(renderer, "40", cxVel + 0.010f, cy + 0.088f, 0.009f, 0.012f, 0.002f, 0.85f, 0.90f, 1.0f);
            dibujarTexto(renderer, "60", cxVel + 0.066f, cy - 0.070f, 0.009f, 0.012f, 0.002f, 1.0f, 0.65f, 0.25f);

            // Rótulo superior del velocímetro
            dibujarTexto(renderer, "KM/H", cxVel - 0.024f, cy + 0.038f, 0.011f, 0.014f, 0.002f, 0.30f, 0.85f, 1.0f);

            // PANTALLA DIGITAL 7 SEGMENTOS EN EL CENTRO INFERIOR DEL VELOCÍMETRO
            renderer.caja(cxVel, cy - 0.050f, 0.045f, 0.070f, 0.038f, 0.01f, 0.02f, 0.04f, 0.07f); // Marco pantalla
            dibujarNumero7Seg(renderer, Math.round(kmh), cxVel, cy - 0.050f, 0.018f, 0.024f, 0.003f, 0.20f, 0.95f, 1.0f);

            // AGUJA ANALÓGICA DEL VELOCÍMETRO (Blanco / Azul ártico brillante)
            float fracVel = Math.min(1.05f, Math.max(0f, kmh / 60.0f));
            float angVelDeg = -135.0f + fracVel * 270.0f;
            float angVelRad = (float) Math.toRadians(angVelDeg);
            float dirVelX = (float) Math.sin(angVelRad);
            float dirVelY = (float) Math.cos(angVelRad);

            renderer.cajaGirada(cxVel + dirVelX * lAguja * 0.5f, cy + dirVelY * lAguja * 0.5f, 0.05f,
                                0.006f, lAguja, 0.01f, 0.30f, 0.85f, 1.0f, -angVelRad);
            renderer.cajaGirada(cxVel + dirVelX * lAguja * 0.85f, cy + dirVelY * lAguja * 0.85f, 0.055f,
                                0.004f, lAguja * 0.3f, 0.01f, 1.0f, 1.0f, 1.0f, -angVelRad);
            dibujarDisco(renderer, cxVel, cy, 0.022f, 0.06f, 0.18f, 0.22f, 0.28f);
            dibujarDisco(renderer, cxVel, cy, 0.011f, 0.065f, 0.85f, 0.90f, 0.95f);

            // ==================== 5. CONSOLA CENTRAL DE TESTIGOS LUMINOSOS ====================
            float yPuente = cy + 0.115f;
            renderer.caja(-0.59f, yPuente, 0.03f, 0.52f, 0.058f, 0.01f, 0.08f, 0.12f, 0.18f);

            // A) Testigo de Marcha (P: Freno / R: Reversa / D: Conducir)
            float xGear = -0.77f;
            if (frenoMano || Math.abs(velocidad) < 0.2f) {
                renderer.caja(xGear, yPuente, 0.04f, 0.075f, 0.040f, 0.01f, 0.90f, 0.20f, 0.15f);
                dibujarTexto(renderer, "P", xGear - 0.005f, yPuente, 0.012f, 0.016f, 0.002f, 1.0f, 1.0f, 1.0f);
            } else if (velocidad > 0) {
                renderer.caja(xGear, yPuente, 0.04f, 0.075f, 0.040f, 0.01f, 0.15f, 0.85f, 0.35f);
                dibujarTexto(renderer, "D", xGear - 0.005f, yPuente, 0.012f, 0.016f, 0.002f, 1.0f, 1.0f, 1.0f);
            } else {
                renderer.caja(xGear, yPuente, 0.04f, 0.075f, 0.040f, 0.01f, 0.95f, 0.65f, 0.10f);
                dibujarTexto(renderer, "R", xGear - 0.005f, yPuente, 0.012f, 0.016f, 0.002f, 1.0f, 1.0f, 1.0f);
            }

            // B) Testigo de Faros (Verde ON / Gris OFF)
            float xFaro = -0.66f;
            float rFaro = faros ? 0.15f : 0.18f;
            float gFaro = faros ? 0.88f : 0.22f;
            float bFaro = faros ? 0.38f : 0.28f;
            renderer.caja(xFaro, yPuente, 0.04f, 0.11f, 0.040f, 0.01f, rFaro, gFaro, bFaro);
            dibujarTexto(renderer, faros ? "LUCES ON" : "LUCES OFF", xFaro - 0.042f, yPuente,
                         0.008f, 0.012f, 0.001f, faros ? 1.0f : 0.65f, faros ? 1.0f : 0.65f, faros ? 1.0f : 0.65f);

            // C) Testigo de Ambiente (Sol dorado / Luna azulada)
            float xAmb = -0.52f;
            float rAmb = noche ? 0.25f : 0.95f;
            float gAmb = noche ? 0.45f : 0.80f;
            float bAmb = noche ? 0.90f : 0.15f;
            renderer.caja(xAmb, yPuente, 0.04f, 0.10f, 0.040f, 0.01f, rAmb, gAmb, bAmb);
            dibujarTexto(renderer, noche ? "NOCHE" : "DIA", xAmb - 0.024f, yPuente,
                         0.009f, 0.013f, 0.002f, noche ? 1.0f : 0.1f, noche ? 1.0f : 0.1f, noche ? 1.0f : 0.1f);

            // D) Testigo de Bocina (Se ilumina vivo al presionar H o B)
            float xBoc = -0.40f;
            float rBoc = bocina ? 1.0f : 0.18f;
            float gBoc = bocina ? 0.88f : 0.22f;
            float bBoc = bocina ? 0.15f : 0.28f;
            renderer.caja(xBoc, yPuente, 0.04f, 0.095f, 0.040f, 0.01f, rBoc, gBoc, bBoc);
            dibujarTexto(renderer, "CLAXON", xBoc - 0.028f, yPuente,
                         0.008f, 0.012f, 0.001f, bocina ? 0.1f : 0.65f, bocina ? 0.1f : 0.65f, bocina ? 0.1f : 0.65f);

            // ==================== 6. CINTA INFERIOR DE SECTOR URBANO ====================
            float ySec = cy - 0.170f;
            renderer.caja(-0.59f, ySec, 0.03f, 0.56f, 0.038f, 0.01f, 0.07f, 0.10f, 0.16f);

            // Punto de color identificativo del cuadrante
            float rPip = 0.25f, gPip = 0.55f, bPip = 0.85f;
            if (autoX < 0 && autoZ < 0) { rPip = 0.88f; gPip = 0.35f; bPip = 0.20f; } // Noroeste
            else if (autoX > 0 && autoZ < 0) { rPip = 0.20f; gPip = 0.75f; bPip = 0.88f; } // Noreste
            else if (autoX < 0 && autoZ > 0) { rPip = 0.95f; gPip = 0.70f; bPip = 0.15f; } // Suroeste
            else { rPip = 0.30f; gPip = 0.85f; bPip = 0.40f; } // Sureste

            renderer.caja(-0.83f, ySec, 0.04f, 0.020f, 0.024f, 0.01f, rPip, gPip, bPip);
            String nombreSector = obtenerSector(autoX, autoZ);
            dibujarTexto(renderer, nombreSector, -0.80f, ySec, 0.008f, 0.012f, 0.0015f, 0.85f, 0.90f, 0.95f);

        } finally {
            renderer.entero("uMapa", 0);
            glEnable(GL_DEPTH_TEST);
        }
    }

    // ==================== 5. MENÚ DE PAUSA Y GUÍA COMPLETA DE CONTROLES ====================

    /**
     * Dibuja el menú de pausa superpuesto con la guía completa de controles:
     * - Fondo atenuado de pantalla.
     * - Tarjeta central con bordes dorados e información legible con fuente 3x5.
     * - Teclas 3D estilizadas y descripción clara para cada acción de juego.
     */
    public static void dibujarMenuPausa(clase1 renderer) {
        glDisable(GL_DEPTH_TEST);
        renderer.entero("uMapa", 2);

        try {
            // Fondo atenuado oscuro semitransparente sobre la ciudad
            renderer.caja(0, 0, 0.0f, 2.0f, 2.0f, 0.01f, 0.02f, 0.03f, 0.06f);

            // Marco principal del menú
            float w = 1.48f;
            float h = 1.56f;
            renderer.caja(0, 0, 0.02f, w + 0.024f, h + 0.024f, 0.01f, 0.95f, 0.80f, 0.25f); // Borde exterior dorado
            renderer.caja(0, 0, 0.03f, w, h, 0.01f, 0.05f, 0.08f, 0.14f); // Fondo azul noche

            // CABECERA DEL MENÚ
            float yCabecera = 0.66f;
            renderer.caja(0, yCabecera, 0.04f, w - 0.06f, 0.088f, 0.01f, 0.10f, 0.18f, 0.30f);
            renderer.caja(0, yCabecera - 0.044f, 0.045f, w - 0.06f, 0.005f, 0.01f, 0.95f, 0.80f, 0.25f); // Línea dorada divisoria

            dibujarTexto(renderer, "PAUSA - GUIA DE CONTROLES", -0.42f, yCabecera + 0.015f,
                         0.020f, 0.025f, 0.004f, 1.0f, 0.88f, 0.30f);
            dibujarTexto(renderer, "CIUDAD Y CONDUCCION 3D", -0.26f, yCabecera - 0.022f,
                         0.012f, 0.016f, 0.003f, 0.35f, 0.82f, 0.95f);

            // FILAS DE CONTROLES: [Tecla, Descripción, rKey, gKey, bKey]
            String[][] filas = {
                {"W / S",      "ACELERAR / RETROCESO (MARCHA ATRAS)", "0.20", "0.80", "1.00"},
                {"A / D",      "GIRAR VOLANTE (IZQUIERDA / DERECHA)", "0.20", "0.80", "1.00"},
                {"ESPACIO",    "FRENO DE MANO / DETENCION RAPIDA",    "0.98", "0.45", "0.20"},
                {"C",          "CAMBIAR CAMARA (4 MODOS ESTILO GTA)", "0.25", "0.88", "0.40"},
                {"F",          "ENCENDER / APAGAR FAROS DELANTEROS",   "1.00", "0.88", "0.25"},
                {"N",          "ALTERNAR AMBIENTE (DIA / NOCHE)",     "0.35", "0.72", "1.00"},
                {"H / B",      "BOCINA / CLAXON CONTINUO",            "1.00", "0.82", "0.15"},
                {"M",          "VER / OCULTAR RADAR MINIMAPA",        "0.82", "0.45", "0.98"},
                {"R",          "REINICIAR POSICION DEL VEHICULO",     "0.95", "0.30", "0.30"},
                {"P / TAB",    "PAUSAR O REANUDAR LA PARTIDA",        "0.30", "0.95", "0.70"},
                {"ESC",        "SALIR DEL JUEGO AL ESCRITORIO",       "0.85", "0.30", "0.30"}
            };

            float yInicio = 0.49f;
            float espaciado = 0.088f;

            for (int i = 0; i < filas.length; i++) {
                float y = yInicio - i * espaciado;
                String keyText = filas[i][0];
                String descText = filas[i][1];
                float rK = Float.parseFloat(filas[i][2]);
                float gK = Float.parseFloat(filas[i][3]);
                float bK = Float.parseFloat(filas[i][4]);

                // Caja de fondo de la fila (azul tenue)
                renderer.caja(0, y, 0.04f, w - 0.08f, 0.066f, 0.01f, 0.07f, 0.11f, 0.18f);

                // BOTÓN / TECLA ESTILIZADA EN 3D (Izquierda)
                float xTecla = -0.44f;
                float wTecla = 0.28f;
                renderer.caja(xTecla, y, 0.045f, wTecla, 0.052f, 0.01f, rK * 0.4f, gK * 0.4f, bK * 0.4f); // Sombra borde
                renderer.caja(xTecla, y, 0.050f, wTecla - 0.008f, 0.044f, 0.01f, 0.12f, 0.16f, 0.24f); // Cuerpo tecla

                // Texto centrado dentro de la tecla
                float wCharKey = 0.014f;
                float espCharKey = 0.003f;
                float anchoTextoKey = keyText.length() * wCharKey + (keyText.length() - 1) * espCharKey;
                float xTextoKey = xTecla - anchoTextoKey * 0.5f;
                dibujarTexto(renderer, keyText, xTextoKey, y, wCharKey, 0.018f, espCharKey, rK, gK, bK);

                // TARJETA DESCRIPTIVA DE LA ACCIÓN (Derecha)
                float xDesc = 0.17f;
                float wDesc = 0.80f;
                renderer.caja(xDesc, y, 0.045f, wDesc, 0.052f, 0.01f, 0.09f, 0.14f, 0.22f);

                // Texto descriptivo alineado
                dibujarTexto(renderer, descText, xDesc - wDesc * 0.48f, y,
                             0.011f, 0.015f, 0.002f, 0.90f, 0.94f, 0.98f);
            }

            // BOTÓN INFERIOR DE REANUDACIÓN
            float yPie = -0.57f;
            renderer.caja(0, yPie, 0.04f, w - 0.16f, 0.075f, 0.01f, 0.15f, 0.55f, 0.40f); // Verde esmeralda
            renderer.caja(0, yPie, 0.045f, w - 0.176f, 0.063f, 0.01f, 0.08f, 0.32f, 0.22f);

            String pieTexto = "PULSA  [ P ]  O  [ TAB ]  PARA REANUDAR";
            float wCharPie = 0.015f;
            float espCharPie = 0.003f;
            float anchoPie = pieTexto.length() * wCharPie + (pieTexto.length() - 1) * espCharPie;
            dibujarTexto(renderer, pieTexto, -anchoPie * 0.5f, yPie,
                         wCharPie, 0.020f, espCharPie, 1.0f, 1.0f, 1.0f);

        } finally {
            renderer.entero("uMapa", 0);
            glEnable(GL_DEPTH_TEST);
        }
    }
}
