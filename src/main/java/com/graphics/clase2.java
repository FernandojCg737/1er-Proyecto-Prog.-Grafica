package com.graphics; // Agrupa las cuatro lecciones dentro del mismo paquete.

import static org.lwjgl.glfw.GLFW.*; // Permite consultar teclas y cambiar el título de la ventana.

/**
 * CLASE 2: CREACIÓN Y MOVIMIENTO DEL AUTO.
 * Hereda la ciudad de clase1 y agrega únicamente lo necesario para conducir.
 * Orden de lectura: variables, teclado, movimiento, colisiones, cámara y
 * dibujo.
 */
public class clase2 extends clase1 {

    // ==================== 1. VARIABLES DEL AUTO ====================
    protected float autoX = -30; // Posición horizontal inicial: centro de una calle.
    protected float autoZ = 30; // Posición inicial sobre el eje que recorre el fondo de la ciudad.
    protected float angulo = 0; // Orientación en radianes; cero apunta hacia -Z.
    protected float velocidad = 0; // Unidades por segundo; un valor negativo significa reversa.
    protected boolean camaraAerea = false; // Compatibilidad con etapas previas.
    protected int modoCamara = 0; // 0: Cercana, 1: Lejana, 2: Capó/Primera persona, 3: Aérea GTA
    protected static final String[] NOMBRES_CAMARA = { "Cercana", "Lejana", "Capo", "Aerea GTA" };
    protected static final float RADIO_AUTO = 1.65f; // Radio que contiene al vehículo para las colisiones.
    protected boolean bocinaSonando = false; // Indica si la bocina se mantiene activada actualmente.
    protected float timerBocina = 0; // Temporizador para el indicador visual y feedback de bocina.
    protected float timerImpacto = 0; // Temporizador para el feedback visual de impacto/colisión.
    protected boolean bloqueadoPorTijera = false; // Indica si el vehículo está paralizado por efecto tijera en reversa.
    protected final Chata chata = new Chata(); // Chata (remolque de carga) enganchada al vehículo del jugador.

    // ==================== 2. TECLADO Y REINICIO ====================

    /** Atiende acciones que deben ocurrir una sola vez por pulsación. */
    @Override // Sustituye el método de clase1 y conserva sus acciones mediante super.
    protected void tecla(int key) {
        super.tecla(key); // Ejecuta el control ESC definido en clase1.

        if (key == GLFW_KEY_C) { // Comprueba si se presionó la tecla de cámara.
            modoCamara = (modoCamara + 1) % 4; // Rota cíclicamente entre los 4 modos de cámara estilo GTA.
            camaraAerea = (modoCamara == 3);
        }

        if (key == GLFW_KEY_R) { // Comprueba si el usuario quiere comenzar de nuevo.
            reiniciar(); // Restaura las variables de conducción.
        }
    }

    /**
     * Comunica al sistema de audio y al entorno si la bocina se mantiene presionada
     * o se soltó.
     */
    protected void actualizarEstadoBocina(boolean activa) {
        SistemaAudio.setBocinaPresionada(activa);
    }

    /**
     * Hace sonar la bocina del auto de forma puntual (usado en tests y acciones
     * puntuales).
     */
    protected void tocarBocina() {
        SistemaAudio.tocarBocina();
        timerBocina = 0.5f;
    }

    /** Coloca nuevamente el auto en su punto de partida. */
    protected void reiniciar() {
        autoX = -30; // Recupera la coordenada X de inicio.
        autoZ = 30; // Recupera la coordenada Z de inicio.
        angulo = 0; // Orienta el frente hacia -Z.
        velocidad = 0; // Detiene cualquier movimiento previo.
        timerBocina = 0;
        timerImpacto = 0;
        bloqueadoPorTijera = false;
        chata.reiniciar(autoX, autoZ, angulo);
        if (bocinaSonando) {
            bocinaSonando = false;
            actualizarEstadoBocina(false);
        }
    }

    // ==================== 3. MOVIMIENTO POR CUADRO ====================

    /**
     * Actualiza la conducción; deltaTime contiene los segundos transcurridos entre
     * cuadros.
     */
    @Override // Cambia la cámara orbital de clase1 por el control del vehículo.
    protected void actualizar(float deltaTime) {
        // Control continuo de la bocina: suena mientras se mantenga presionada y cesa
        // al soltarla
        boolean bocinaPulsada = pulsada(GLFW_KEY_H) || pulsada(GLFW_KEY_B);
        if (bocinaPulsada != bocinaSonando) {
            bocinaSonando = bocinaPulsada;
            actualizarEstadoBocina(bocinaSonando);
        }

        if (bocinaSonando) {
            timerBocina = 0.2f;
        } else if (timerBocina > 0) {
            timerBocina = Math.max(0, timerBocina - deltaTime);
        }

        float acelerador = 0; // Sin teclas pulsadas no se aplica aceleración del motor.

        if (pulsada(GLFW_KEY_W) || pulsada(GLFW_KEY_UP)) { // Acepta W o flecha arriba para avanzar.
            acelerador += 1; // Solicita aceleración hacia delante.
        }

        if (pulsada(GLFW_KEY_S) || pulsada(GLFW_KEY_DOWN)) { // Acepta S o flecha abajo para retroceder.
            acelerador -= 1; // Primero reduce la velocidad positiva y después entra en reversa.
        }

        velocidad += acelerador * 9 * deltaTime; // Integra la aceleración de 9 unidades por segundo cuadrado.
        float resistencia = 0.7f; // Define la pérdida de velocidad normal al rodar.

        if (pulsada(GLFW_KEY_SPACE)) { // Detecta si el usuario mantiene presionado el freno.
            resistencia = 7; // Aumenta la pérdida de velocidad para detenerse rápidamente.
        }

        float factorFrenado = (float) Math.exp(-resistencia * deltaTime); // Calcula la fracción de velocidad
                                                                          // conservada.
        velocidad *= factorFrenado; // Aplica resistencia de forma proporcional al tiempo transcurrido.
        velocidad = Math.max(-6, Math.min(16, velocidad)); // Limita la reversa a -6 y el avance a 16.
        float direccion = 0; // Sin dirección presionada el volante permanece recto.

        if (pulsada(GLFW_KEY_A) || pulsada(GLFW_KEY_LEFT)) { // Comprueba el giro hacia la izquierda.
            direccion += 1; // Selecciona el sentido positivo de rotación.
        }

        if (pulsada(GLFW_KEY_D) || pulsada(GLFW_KEY_RIGHT)) { // Comprueba el giro hacia la derecha.
            direccion -= 1; // Selecciona el sentido negativo de rotación.
        }

        float sigAngulo = angulo + direccion * velocidad * 0.11f * deltaTime; // Gira según la velocidad; en reversa
                                                                              // invierte el giro.
        float frenteX = -(float) Math.sin(sigAngulo); // Obtiene la componente X del frente del vehículo.
        float frenteZ = -(float) Math.cos(sigAngulo); // Obtiene la componente Z; con ángulo cero vale -1.
        float sigX = autoX + frenteX * velocidad * deltaTime; // Propone la nueva posición X.
        float sigZ = autoZ + frenteZ * velocidad * deltaTime; // Propone la nueva posición Z.

        Chata.EstadoCandidato candChata = chata.calcularCandidato(sigX, sigZ, sigAngulo, autoX, autoZ, angulo);
        boolean bloqueoTijera = (velocidad < 0) && chata.estaEnTijera(candChata, sigAngulo);

        if (!bloqueoTijera && puedeCircularVehiculoCompleto(sigX, sigZ, candChata)
                && !colisionaConVehiculos(sigX, sigZ, candChata)) {
            autoX = sigX;
            autoZ = sigZ;
            angulo = sigAngulo;
            chata.aplicarCandidato(candChata);
            bloqueadoPorTijera = false;
            if (timerImpacto > 0) {
                timerImpacto = Math.max(0, timerImpacto - deltaTime);
            }
        } else {
            if (bloqueoTijera) {
                bloqueadoPorTijera = true;
            } else if (Math.abs(velocidad) > 0.3f) {
                timerImpacto = 0.4f;
            }
            velocidad = 0;
        }

        actualizarTitulo(); // Muestra los controles y la velocidad actual.
    }

    /**
     * Comprueba si tanto el vehículo como la chata y todos sus extremos caben en la
     * calzada.
     */
    protected boolean puedeCircularVehiculoCompleto(float aX, float aZ, Chata.EstadoCandidato candChata) {
        if (!puedeCircular(aX, aZ)) {
            return false;
        }
        return chata.puedeCircular(candChata);
    }

    /**
     * Permite a etapas avanzadas (clase4) verificar colisión física con el tráfico.
     */
    protected boolean colisionaConVehiculos(float x, float z) {
        return false; // En clase2 todavía no hay vehículos de tráfico
    }

    /**
     * Sobrecarga para verificar colisión de todo el conjunto con otros vehículos o
     * peatones.
     */
    protected boolean colisionaConVehiculos(float x, float z, Chata.EstadoCandidato candChata) {
        return colisionaConVehiculos(x, z);
    }

    /**
     * Compone el texto de la ventana sin mezclarlo con las fórmulas de conducción.
     */
    private void actualizarTitulo() {
        int kilometrosPorHora = Math.round(Math.abs(velocidad) * 3.6f); // Convierte m/s a km/h y redondea.
        String titulo = getClass().getSimpleName(); // Obtiene el nombre de la etapa que se está ejecutando.
        titulo += " | WASD/flechas: conducir | Espacio: freno"; // Añade los controles del vehículo.
        titulo += " | C: camara [" + NOMBRES_CAMARA[modoCamara] + "] | H/B: bocina | R: reiniciar | "; // Muestra la
                                                                                                       // cámara GTA
                                                                                                       // activa.
        titulo += kilometrosPorHora + " km/h"; // Añade la magnitud de la velocidad.
        if (bocinaSonando || timerBocina > 0) {
            titulo += " [¡BOCINA! 📢]"; // Feedback visual en el título
        }
        if (bloqueadoPorTijera) {
            titulo += " [¡TIJERA! Chata trabada 🛑]"; // Feedback visual al bloquearse marcha atrás
        } else if (timerImpacto > 0) {
            titulo += " [¡IMPACTO! 💥]"; // Feedback visual de colisión
        }
        titulo += estadoExtra(); // Permite a clase3 y clase4 añadir luces y entregas.
        if (ventana != 0) {
            glfwSetWindowTitle(ventana, titulo); // Publica el texto en la barra superior de la ventana.
        }
    }

    /** Deja un espacio para los indicadores de las próximas lecciones. */
    protected String estadoExtra() {
        return ""; // En clase2 todavía no hay información adicional.
    }

    // ==================== 4. COLISIONES CON LA CIUDAD ====================

    /**
     * Comprueba si el círculo del auto cabe en una posición sin tocar manzanas ni
     * bordes.
     */
    protected boolean puedeCircular(float x, float z) {
        return MapaCiudad.puedeCircular(x, z, RADIO_AUTO); // Delega la comprobación al mapa ampliado 11x11.
    }

    // ==================== 5. CÁMARA ====================

    /**
     * Modos de cámara estilo saga GTA: 3ª persona cercana, lejana, capó y aérea.
     */
    @Override // Personaliza la cámara creada en clase1.
    protected void configurarCamara() {
        float frenteX = -(float) Math.sin(angulo);
        float frenteZ = -(float) Math.cos(angulo);
        float camX, camY, camZ;
        float objX, objY, objZ;

        switch (modoCamara) {
            case 0: // 3ª Persona Cercana (Close Chase): pegada y deportiva
                camX = autoX - frenteX * 7.5f;
                camY = 3.6f;
                camZ = autoZ - frenteZ * 7.5f;
                objX = autoX + frenteX * 2.5f;
                objY = 1.0f;
                objZ = autoZ + frenteZ * 2.5f;
                break;
            case 1: // 3ª Persona Lejana (Far Chase): amplia y elevada para ver el tráfico
                camX = autoX - frenteX * 13.5f;
                camY = 7.2f;
                camZ = autoZ - frenteZ * 13.5f;
                objX = autoX + frenteX * 3.5f;
                objY = 1.0f;
                objZ = autoZ + frenteZ * 3.5f;
                break;
            case 2: // Primera Persona / Capó (Bumper / Hood Cam): inmersión total en la carretera
                camX = autoX + frenteX * 1.35f;
                camY = 1.25f;
                camZ = autoZ + frenteZ * 1.35f;
                objX = autoX + frenteX * 20.0f;
                objY = 1.10f;
                objZ = autoZ + frenteZ * 20.0f;
                break;
            case 3: // Clásico Aéreo Cenital / Helicóptero (Top-Down): vista superior tipo GTA 1 & 2
            default:
                camX = autoX - frenteX * 4.0f;
                camY = 46.0f;
                camZ = autoZ - frenteZ * 4.0f;
                objX = autoX;
                objY = 0.5f;
                objZ = autoZ;
                break;
        }

        vector("uOjo", camX, camY, camZ);
        vector("uObjetivo", objX, objY, objZ);
        decimal("uAspecto", (float) ancho / alto); // Mantiene las proporciones al redimensionar la ventana.
    }

    // ==================== 6. DIBUJO DEL AUTO ====================

    /** Dibuja primero la ciudad existente y luego el vehículo. */
    @Override // Amplía el dibujo de clase1 sin copiar su código.
    protected void escena() {
        super.escena(); // Dibuja asfalto, calles, edificios y parques.
        dibujarAuto(); // Añade el modelo del vehículo sobre la ciudad.
    }

    /** Indica si los faros del vehículo están encendidos (ampliado en clase3). */
    protected boolean farosActivos() {
        return false;
    }

    /**
     * Construye el auto con cajas; argumentos: posición XYZ, tamaño XYZ y color
     * RGB.
     */
    protected void dibujarAuto() {
        // Sombra de contacto suave sobre el asfalto que asienta el auto en el suelo
        cajaGirada(autoX, 0.03f, autoZ, 2.2f, 0.02f, 3.2f, 0.08f, 0.10f, 0.13f, angulo);

        pieza(0, 0.65f, 0, 1.65f, 0.55f, 2.6f, 0.95f, 0.24f, 0.12f); // Dibuja la carrocería roja.
        pieza(0, 1.12f, 0.12f, 1.3f, 0.55f, 1.25f, 0.22f, 0.65f, 0.78f); // Dibuja la cabina azulada.
        float[] ladosRuedas = { -0.88f, 0.88f }; // Ubica ruedas a izquierda y derecha del auto.
        float[] ejesRuedas = { -0.82f, 0.82f }; // Ubica las ruedas delanteras y traseras.

        for (float x : ladosRuedas) { // Selecciona uno de los dos lados del vehículo.
            for (float z : ejesRuedas) { // Selecciona el eje delantero o trasero.
                pieza(x, 0.38f, z, 0.24f, 0.58f, 0.6f, 0.055f, 0.065f, 0.08f); // Dibuja una rueda oscura.
            }
        }

        boolean encendidos = farosActivos();
        float[] ladosFaros = { -0.55f, 0.55f }; // Define la separación lateral de las luces.
        for (float x : ladosFaros) { // Repite el dibujo para ambos lados.
            if (encendidos) {
                entero("uEmision", 1);
                pieza(x, 0.68f, -1.32f, 0.38f, 0.2f, 0.07f, 1.0f, 0.96f, 0.68f); // Faro delantero encendido
                pieza(x, 0.68f, 1.32f, 0.35f, 0.17f, 0.07f, 0.95f, 0.08f, 0.08f); // Luz trasera roja encendida
                entero("uEmision", 0);
            } else {
                pieza(x, 0.68f, -1.32f, 0.38f, 0.2f, 0.07f, 0.36f, 0.40f, 0.44f); // Faro delantero apagado
                pieza(x, 0.68f, 1.32f, 0.35f, 0.17f, 0.07f, 0.42f, 0.10f, 0.10f); // Luz trasera apagada
            }
        }

        pieza(0, 0.40f, 1.38f, 0.16f, 0.12f, 0.20f, 0.20f, 0.22f, 0.24f);
        pieza(0, 0.44f, 1.48f, 0.12f, 0.14f, 0.12f, 0.70f, 0.72f, 0.76f);

        chata.dibujar(this, encendidos);
    }

    /** Devuelve la chata acoplada al vehículo. */
    public Chata getChata() {
        return chata;
    }

    /**
     * Indica si el vehículo está paralizado por tijera de la chata en retroceso.
     */
    public boolean isBloqueadoPorTijera() {
        return bloqueadoPorTijera;
    }

    /** Transforma una pieza del espacio local del auto al espacio de la ciudad. */
    protected void pieza(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(angulo); // Calcula el coseno de la orientación del auto.
        float seno = (float) Math.sin(angulo); // Calcula el seno de la misma orientación.
        float mundoX = autoX + coseno * x + seno * z; // Gira la posición local y suma la posición X del auto.
        float mundoZ = autoZ - seno * x + coseno * z; // Gira la posición local y suma la posición Z del auto.
        cajaGirada(mundoX, y, mundoZ, sx, sy, sz, r, g, b, angulo); // Dibuja la pieza con la orientación del vehículo.
    }

    /** Punto de entrada para ejecutar únicamente la segunda etapa. */
    public static void main(String[] args) {
        clase2 aplicacion = new clase2(); // Crea la aplicación con ciudad y vehículo.
        aplicacion.run(); // Inicia la ventana y el ciclo de dibujo heredados.
    }
}
