package com.graphics;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

/**
 * CLASE 4: VERSIÓN FINAL DE LA CIUDAD INTERACTIVA AMPLIADA.
 * Coordina de forma modular todos los subsistemas del proyecto:
 * - Ampliación de la ciudad (11x11 celdas) mediante MapaCiudad.
 * - Conducción y colisiones de clase2.
 * - Iluminación y faros de clase3.
 * - Decoración urbana procedural y semáforos animados en DecoracionUrbana.
 * - Sistema de entregas y baliza dorada en SistemaEntregas.
 * - Tráfico autónomo (+10 pts bonificación) en SistemaTrafico.
 * - Minimapa ortográfico multi-viewport adaptativo.
 */
public class clase4 extends clase3 {

    // ==================== 1. SUBSISTEMAS MODULARES ====================
    private boolean mostrarMapa = true; // Control del minimapa en pantalla
    private boolean pausado = false; // Control del menú de pausa y ayuda de controles
    private boolean completadoNotificado = false; // Control para la pantalla de victoria
    private final SistemaEntregas entregas = new SistemaEntregas();
    private final SistemaTrafico trafico = new SistemaTrafico();
    private final SistemaPeatones peatones = new SistemaPeatones();

    // ==================== 2. CONTROLES Y REINICIO ====================

    @Override
    protected void tecla(int key) {
        super.tecla(key); // ESC (clase1), C, H/B y R (clase2), N y F (clase3)

        if (entregas.esVictoria() && !completadoNotificado) {
            if (key == GLFW_KEY_ENTER) {
                completadoNotificado = true; // "Continuar"
            }
            // "Jugar de Nuevo" (R) y "Salir" (ESC) son manejados por super.tecla(key)
            return;
        }

        if (key == GLFW_KEY_M) {
            mostrarMapa = !mostrarMapa; // Alternar visibilidad del minimapa
        }
        if (key == GLFW_KEY_P || key == GLFW_KEY_TAB) {
            pausado = !pausado; // Alternar pausa y menú interactivo de controles
        }
    }

    @Override
    protected void actualizarEstadoBocina(boolean activa) {
        super.actualizarEstadoBocina(activa);
        if (activa) {
            trafico.alertarPorBocina(autoX, autoZ, angulo);
        }
    }

    @Override
    protected void tocarBocina() {
        super.tocarBocina(); // Reproduce audio procedural y activa feedback visual
        trafico.alertarPorBocina(autoX, autoZ, angulo); // Los autos de tráfico al frente reaccionan abriendo paso
    }

    @Override
    protected void reiniciar() {
        super.reiniciar(); // Restaura el vehículo del jugador a la posición inicial
        entregas.reiniciar(); // Restaura las entregas y el cronómetro
        trafico.reiniciar(); // Restaura las posiciones de los vehículos de tráfico
        pausado = false;
        completadoNotificado = false;
    }

    // ==================== 3. ACTUALIZACIÓN POR CUADRO ====================

    @Override
    protected void actualizar(float deltaTime) {
        if (entregas.esVictoria() && !completadoNotificado) {
            if (ventana != 0) {
                glfwSetWindowTitle(ventana,
                        "clase4 | [FELICIDADES METAS COMPLETADAS] ENTER: Continuar | R: Jugar de Nuevo | ESC: Salir");
            }
            return; // Se detiene la simulación física mientras se muestra el mensaje final
        }

        if (pausado) {
            if (ventana != 0) {
                glfwSetWindowTitle(ventana,
                        "clase4 | [PAUSA - GUÍA DE CONTROLES] Pulsa P o TAB para reanudar | ESC: salir");
            }
            return; // Mientras esté pausado, la física del coche, entregas y tráfico permanecen
                    // congelados
        }

        super.actualizar(deltaTime); // Actualiza la física del coche, chata y controles
        entregas.actualizar(deltaTime, autoX, autoZ, velocidad);
        trafico.actualizar(deltaTime, autoX, autoZ, RADIO_AUTO, chata.x, chata.z, chata.radio, entregas.tiempo); // Tráfico
                                                                                                                 // respeta
                                                                                                                 // jugador
                                                                                                                 // y
                                                                                                                 // chata
        peatones.actualizar(deltaTime, entregas.tiempo, autoX, autoZ, RADIO_AUTO, chata.x, chata.z, chata.radio); // Peatones
                                                                                                                  // respetan
                                                                                                                  // jugador
                                                                                                                  // y
                                                                                                                  // chata
        if (bocinaSonando) {
            trafico.alertarPorBocina(autoX, autoZ, angulo); // Mantiene alerta al tráfico mientras esté pulsado
        }
    }

    @Override
    protected boolean colisionaConVehiculos(float x, float z) {
        return trafico.colisionaConTrafico(x, z, RADIO_AUTO) || peatones.colisionaCon(x, z, RADIO_AUTO);
    }

    @Override
    protected boolean colisionaConVehiculos(float x, float z, Chata.EstadoCandidato candChata) {
        // 1. Colisión del auto
        if (colisionaConVehiculos(x, z)) {
            return true;
        }
        // 2. Colisión de la chata y sus extremos contra autos de tráfico o peatones
        if (candChata != null) {
            for (float[] punto : Chata.PUNTOS_COLISION) {
                float wx = candChata.transformarX(punto[0], punto[1]);
                float wz = candChata.transformarZ(punto[0], punto[1]);
                float r = punto[2];
                if (trafico.colisionaConTrafico(wx, wz, r) || peatones.colisionaCon(wx, wz, r)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected String estadoExtra() {
        String mensaje = super.estadoExtra(); // Día/Noche y Faros de clase3
        mensaje += " | M: mapa | P: pausa";
        mensaje += entregas.getEstadoTexto();
        mensaje += " | Trafico: " + trafico.getCantidadAutos() + " autos";
        return mensaje;
    }

    // ==================== 4. DIBUJO DE LA ESCENA ====================

    @Override
    protected void escena() {
        super.escena(); // Dibuja la ciudad (11x11), el auto del jugador y las 16 farolas
        trafico.dibujar(this, noche); // Los otros autos tienen luces encendidas de noche y apagadas de día
        peatones.dibujar(this); // Dibuja los peatones caminando o esperando en los cruces

        if (!vistaMapa) {
            // Decoración detallada (árboles, bancos, semáforos, ventanas, nubes y
            // señalización)
            DecoracionUrbana.dibujar(this, entregas.tiempo, noche);
        }

        // Baliza y plataforma de la entrega activa
        entregas.dibujar(this, vistaMapa);

        if (entregas.esVictoria() && !completadoNotificado && !vistaMapa) {
            float fX = -(float) Math.sin(angulo);
            float fZ = -(float) Math.cos(angulo);

            // Cartel flotando 8 metros delante del coche
            float cartelX = autoX + fX * 8.0f;
            float cartelZ = autoZ + fZ * 8.0f;
            float cartelY = 3.8f;

            float escalaTitulo = 0.18f;
            float escalaOpciones = 0.12f;

            String t1 = "FELICIDADES";
            String t2 = "METAS COMPLETADAS";

            // Format time as MM:SS
            int minutos = (int) (entregas.tiempo / 60);
            int segundos = (int) (entregas.tiempo % 60);
            String strMinutos = minutos < 10 ? "0" + minutos : "" + minutos;
            String strSegundos = segundos < 10 ? "0" + segundos : "" + segundos;
            String tTiempo = "TIEMPO: " + strMinutos + ":" + strSegundos;

            String t3 = "- ENTER - CONTINUAR";
            String t4 = "- R - JUGAR DE NUEVO";
            String t5 = "- ESC - SALIR";

            float off1 = Texto3D.calcularAncho(t1, escalaTitulo) / 2.0f;
            Texto3D.dibujar(this, t1, cartelX - (float) Math.cos(angulo) * off1, cartelY + 1.2f,
                    cartelZ + (float) Math.sin(angulo) * off1, escalaTitulo, 1.0f, 0.8f, 0.0f, angulo);

            float off2 = Texto3D.calcularAncho(t2, escalaTitulo) / 2.0f;
            Texto3D.dibujar(this, t2, cartelX - (float) Math.cos(angulo) * off2, cartelY + 0.2f,
                    cartelZ + (float) Math.sin(angulo) * off2, escalaTitulo, 1.0f, 1.0f, 1.0f, angulo);

            float offT = Texto3D.calcularAncho(tTiempo, escalaTitulo) / 2.0f;
            Texto3D.dibujar(this, tTiempo, cartelX - (float) Math.cos(angulo) * offT, cartelY - 0.7f,
                    cartelZ + (float) Math.sin(angulo) * offT, escalaTitulo, 1.0f, 1.0f, 0.0f, angulo);

            float off3 = Texto3D.calcularAncho(t3, escalaOpciones) / 2.0f;
            Texto3D.dibujar(this, t3, cartelX - (float) Math.cos(angulo) * off3, cartelY - 1.5f,
                    cartelZ + (float) Math.sin(angulo) * off3, escalaOpciones, 0.4f, 1.0f, 0.4f, angulo);

            float off4 = Texto3D.calcularAncho(t4, escalaOpciones) / 2.0f;
            Texto3D.dibujar(this, t4, cartelX - (float) Math.cos(angulo) * off4, cartelY - 2.1f,
                    cartelZ + (float) Math.sin(angulo) * off4, escalaOpciones, 0.4f, 0.8f, 1.0f, angulo);

            float off5 = Texto3D.calcularAncho(t5, escalaOpciones) / 2.0f;
            Texto3D.dibujar(this, t5, cartelX - (float) Math.cos(angulo) * off5, cartelY - 2.7f,
                    cartelZ + (float) Math.sin(angulo) * off5, escalaOpciones, 1.0f, 0.4f, 0.4f, angulo);
        }
    }

    // ==================== 5. MINIMAPA Y HUD: SEGUNDO Y TERCER PASE DE DIBUJO
    // ====================

    @Override
    protected void dibujarFrame() {
        super.dibujarFrame(); // Dibuja primero la vista principal en perspectiva 3D

        if (mostrarMapa) {
            // Dimensiones proporcionales y adaptativas al tamaño actual de la ventana
            int dimensionMenor = Math.min(ancho, alto);
            int lado = Math.min(260, dimensionMenor / 3);
            int margen = Math.min(18, dimensionMenor / 20);
            int x = ancho - lado - margen;
            int y = alto - lado - margen;

            glEnable(GL_SCISSOR_TEST);

            // Borde exterior del minimapa
            glScissor(x - 3, y - 3, lado + 6, lado + 6);
            glClearColor(0.80f, 0.87f, 0.94f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Fondo interior del minimapa
            glScissor(x, y, lado, lado);
            glClearColor(0.06f, 0.10f, 0.15f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glViewport(x, y, lado, lado);
            entero("uMapa", 1); // Activa la proyección ortográfica del vertex shader
            vistaMapa = true;

            try {
                escena(); // Dibuja la ciudad ampliada y destinos desde arriba
                dibujarIndicadorAuto(); // Marca el coche del jugador
                trafico.dibujarEnMinimapa(this); // Marca los vehículos de tráfico
            } finally {
                vistaMapa = false;
                entero("uMapa", 0); // Restablece proyección normal en perspectiva
                glDisable(GL_SCISSOR_TEST);
                glViewport(0, 0, ancho, alto); // Restablece el viewport completo
            }
        }

        // Dibujar el panel HUD dentro de la ventana (velocímetro, luces, sector y
        // transmisión)
        SistemaInterfaz.dibujarHUD(this, velocidad, faros, noche, bocinaSonando, autoX, autoZ, pulsada(GLFW_KEY_SPACE));

        // Si el juego está en pausa, dibuja la guía interactiva de controles en
        // pantalla
        if (pausado) {
            SistemaInterfaz.dibujarMenuPausa(this);
        }

        if (glGetError() != GL_NO_ERROR) {
            throw new IllegalStateException("Error OpenGL en interfaz y minimapa");
        }
    }

    /**
     * Dibuja el marcador cian y su punta blanca orientada para el jugador en el
     * minimapa.
     */
    private void dibujarIndicadorAuto() {
        cajaGirada(autoX, 25, autoZ, 2.4f, 0.1f, 3.4f, 0.1f, 1.0f, 1.0f, angulo);
        float frenteX = -(float) Math.sin(angulo);
        float frenteZ = -(float) Math.cos(angulo);
        float puntaX = autoX + frenteX * 2.2f;
        float puntaZ = autoZ + frenteZ * 2.2f;
        caja(puntaX, 26, puntaZ, 1.0f, 0.1f, 1.0f, 1.0f, 1.0f, 1.0f);

        // Marcador de la chata remolcada en el minimapa
        chata.dibujarEnMinimapa(this);
    }

    /** Punto de entrada principal de la aplicación. */
    public static void main(String[] args) {
        clase4 aplicacion = new clase4();
        aplicacion.run();
    }
}
