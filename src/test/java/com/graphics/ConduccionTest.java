package com.graphics;

import junit.framework.TestCase;

/**
 * Comprueba las reglas de conducción, límites, transitabilidad,
 * colisiones entre vehículos, carril derecho y modos de cámara GTA.
 */
public class ConduccionTest extends TestCase {

    /**
     * Comprueba que las avenidas del mapa ampliado conectan el inicio y los
     * destinos de entrega.
     */
    public void testCallesYDestinosTransitables() {
        clase2 juego = new clase2();

        // Avenida oeste intermedia (x = -30)
        for (float z = -45; z <= 45; z += 0.5f) {
            assertTrue("Avenida oeste en z=" + z + " debe ser transitable", juego.puedeCircular(-30, z));
        }

        // Avenida norte intermedia (z = -30)
        for (float x = -45; x <= 45; x += 0.5f) {
            assertTrue("Avenida norte en x=" + x + " debe ser transitable", juego.puedeCircular(x, -30));
        }

        // Avenida exterior este (x = 50)
        for (float z = -45; z <= 45; z += 1.0f) {
            assertTrue("Avenida exterior este en z=" + z + " debe ser transitable", juego.puedeCircular(50, z));
        }

        // Cruces interiores transitables
        assertTrue(juego.puedeCircular(-10, -10));
        assertTrue(juego.puedeCircular(0, -30));
        assertTrue(juego.puedeCircular(10, 10));

        // Manzanas centrales ocupadas (no transitables)
        assertFalse(juego.puedeCircular(0, 0)); // Manzana central
        assertFalse(juego.puedeCircular(-40, -40)); // Manzana noroeste
        assertFalse(juego.puedeCircular(40, 40)); // Manzana sureste
    }

    /**
     * Comprueba la colisión con edificios, parques y los nuevos límites de 55
     * unidades.
     */
    public void testObstaculosYLimites() {
        clase2 juego = new clase2();

        // Centros de manzanas
        assertFalse(juego.puedeCircular(-20, -20)); // Centro de edificio/parque
        assertFalse(juego.puedeCircular(20, -20)); // Centro de parque
        assertFalse(juego.puedeCircular(0, 20)); // Manzana

        // Invasión de acera (manzana en -20, centroX=-20, acera comienza en -25)
        assertFalse(juego.puedeCircular(-25.5f, -20));

        // Límites del mapa ampliado (LIMITE = 55, límite permitido = 55 - 1.65 = 53.35)
        assertFalse(juego.puedeCircular(54.0f, 0)); // Supera el límite derecho
        assertFalse(juego.puedeCircular(-54.0f, 0)); // Supera el límite izquierdo
        assertFalse(juego.puedeCircular(0, -56.0f)); // Fuera del límite norte
        assertFalse(juego.puedeCircular(0, 56.0f)); // Fuera del límite sur

        // Posición válida con separación suficiente de la acera
        assertTrue(juego.puedeCircular(-28.0f, -20));
    }

    /**
     * Comprueba que el reinicio restablece la posición, orientación y detiene el
     * vehículo.
     */
    public void testReinicio() {
        clase4 juego = new clase4();
        juego.autoX = 15;
        juego.autoZ = -10;
        juego.velocidad = 12;
        juego.angulo = 1.5f;

        juego.reiniciar();

        assertEquals(-30f, juego.autoX, 0f);
        assertEquals(30f, juego.autoZ, 0f);
        assertEquals(0f, juego.velocidad, 0f);
        assertEquals(0f, juego.angulo, 0f);
    }

    /**
     * Comprueba la detección de colisión física entre el auto del jugador y los
     * autos de tráfico.
     */
    public void testColisionEntreAutos() {
        clase4 juego = new clase4();
        SistemaTrafico trafico = new SistemaTrafico();

        // Obtener la posición del primer auto de tráfico (Taxi)
        SistemaTrafico.AutoAutonomo taxi = trafico.getAutos()[0];
        float tx = taxi.vehiculo.x;
        float tz = taxi.vehiculo.z;

        // Si el jugador intenta ubicarse sobre el taxi, debe detectarse colisión
        assertTrue("Debe detectar colisión al chocar directamente con el auto de tráfico",
                trafico.colisionaConTrafico(tx, tz, 1.65f));

        // A una distancia prudente (ej. a 15 metros), no debe haber colisión
        assertFalse("No debe detectar colisión si los autos están separados",
                trafico.colisionaConTrafico(tx + 15.0f, tz, 1.65f));
    }

    /**
     * Comprueba que los waypoints de los vehículos autónomos están sobre el carril
     * derecho y son transitables.
     */
    public void testCarrilDerechoTrafico() {
        SistemaTrafico trafico = new SistemaTrafico();

        for (SistemaTrafico.AutoAutonomo auto : trafico.getAutos()) {
            for (float[] wp : auto.waypoints) {
                float wx = wp[0];
                float wz = wp[1];

                // Los waypoints no deben estar en el centro exacto de la calle (-30, -50,
                // etc.), sino desplazados al carril derecho (+-2.3)
                boolean esCarrilDesplazado = (Math.abs(wx) % 10.0f > 1.5f) || (Math.abs(wz) % 10.0f > 1.5f);
                assertTrue("Los waypoints deben estar desplazados al carril derecho", esCarrilDesplazado);

                // Cada punto del carril debe ser físicamente transitable
                assertTrue("El punto del carril (" + wx + ", " + wz + ") debe caber en la calle sin tocar aceras",
                        MapaCiudad.puedeCircular(wx, wz, auto.vehiculo.radio));
            }
        }
    }

    /**
     * Comprueba que la tecla C rota secuencialmente por los 4 modos de cámara
     * estilo GTA.
     */
    public void testModosCamaraGTA() {
        clase2 juego = new clase2();
        assertEquals(0, juego.modoCamara); // Inicia en 0: Cercana

        juego.tecla(org.lwjgl.glfw.GLFW.GLFW_KEY_C);
        assertEquals(1, juego.modoCamara); // 1: Lejana

        juego.tecla(org.lwjgl.glfw.GLFW.GLFW_KEY_C);
        assertEquals(2, juego.modoCamara); // 2: Capó

        juego.tecla(org.lwjgl.glfw.GLFW.GLFW_KEY_C);
        assertEquals(3, juego.modoCamara); // 3: Aérea GTA

        juego.tecla(org.lwjgl.glfw.GLFW.GLFW_KEY_C);
        assertEquals(0, juego.modoCamara); // Vuelve a 0: Cercana
    }

    /**
     * Comprueba la alternancia estricta entre calles Norte-Sur y Este-Oeste en los
     * semáforos.
     */
    public void testSemaforosAlternados() {
        // En t = 2s: Norte-Sur VERDE, Este-Oeste ROJO
        assertEquals(SistemaSemaforos.EstadoLuz.VERDE, SistemaSemaforos.getEstadoNorteSur(2.0f));
        assertEquals(SistemaSemaforos.EstadoLuz.ROJO, SistemaSemaforos.getEstadoEsteOeste(2.0f));

        // En t = 6s: Norte-Sur AMARILLO, Este-Oeste ROJO
        assertEquals(SistemaSemaforos.EstadoLuz.AMARILLO, SistemaSemaforos.getEstadoNorteSur(6.0f));
        assertEquals(SistemaSemaforos.EstadoLuz.ROJO, SistemaSemaforos.getEstadoEsteOeste(6.0f));

        // En t = 9s: Norte-Sur ROJO, Este-Oeste VERDE
        assertEquals(SistemaSemaforos.EstadoLuz.ROJO, SistemaSemaforos.getEstadoNorteSur(9.0f));
        assertEquals(SistemaSemaforos.EstadoLuz.VERDE, SistemaSemaforos.getEstadoEsteOeste(9.0f));

        // En t = 13s: Norte-Sur ROJO, Este-Oeste AMARILLO
        assertEquals(SistemaSemaforos.EstadoLuz.ROJO, SistemaSemaforos.getEstadoNorteSur(13.0f));
        assertEquals(SistemaSemaforos.EstadoLuz.AMARILLO, SistemaSemaforos.getEstadoEsteOeste(13.0f));

        // Seguridad vial: NUNCA deben estar en verde ambos ejes a la vez
        for (float t = 0; t <= 14.0f; t += 0.2f) {
            boolean ambosVerdes = (SistemaSemaforos.getEstadoNorteSur(t) == SistemaSemaforos.EstadoLuz.VERDE)
                    && (SistemaSemaforos.getEstadoEsteOeste(t) == SistemaSemaforos.EstadoLuz.VERDE);
            assertFalse("Norte-Sur y Este-Oeste nunca pueden ser verdes al mismo tiempo en t=" + t, ambosVerdes);
        }
    }

    /**
     * Comprueba que evaluarSemaforo exige detenerse en rojo y permite avanzar en
     * verde.
     */
    public void testFrenadoSemaforoTrafico() {
        // Vehículo viajando hacia el norte (frenteZ = -1) por X = -30, aproximándose al
        // cruce Z = -10
        // En Z = -3.5m (distancia al cruce 6.5m, antes del paso de cebra)
        float factorEnRojo = SistemaSemaforos.evaluarSemaforo(-30f, -3.5f, 0f, -1f, 9.0f); // En t=9s, Norte-Sur es ROJO
        assertEquals(0.0f, factorEnRojo, 0.001f); // Exige alto total

        float factorEnVerde = SistemaSemaforos.evaluarSemaforo(-30f, -3.5f, 0f, -1f, 2.0f); // En t=2s, Norte-Sur es
                                                                                            // VERDE
        assertEquals(1.0f, factorEnVerde, 0.001f); // Vía libre completa
    }

    /**
     * Comprueba la activación continua de la bocina al mantenerla y su detención al
     * soltarla.
     */
    public void testBocinaTeclaYFeedback() {
        clase2 juego = new clase2();
        assertFalse(juego.bocinaSonando);

        // Simular que el usuario presiona la tecla de bocina
        juego.actualizarEstadoBocina(true);
        assertTrue("La bocina debe estar activa mientras se mantenga presionada", SistemaAudio.isBocinaActiva());

        // Simular que el usuario suelta la tecla
        juego.actualizarEstadoBocina(false);
        assertFalse("La bocina debe detenerse inmediatamente al soltar la tecla", SistemaAudio.isBocinaActiva());

        // Comprobar método de invocación puntual
        juego.tocarBocina();
        assertTrue("tocarBocina debe activar el temporizador de feedback", juego.timerBocina > 0.4f);
    }

    /**
     * Comprueba que hacer sonar la bocina alerta a vehículos de tráfico situados en
     * el cono frontal.
     */
    public void testReaccionTraficoBocina() {
        SistemaTrafico trafico = new SistemaTrafico();
        SistemaTrafico.AutoAutonomo taxi = trafico.getAutos()[0];

        // Posicionar taxi a 6 metros delante del jugador mirando hacia el norte (Z
        // disminuyendo, angulo = 0)
        float jugadorX = taxi.vehiculo.x;
        float jugadorZ = taxi.vehiculo.z + 6.0f; // El jugador está detrás en Z, con su frente hacia -Z
        float jugadorAngulo = 0.0f; // Frente hacia -Z

        taxi.vehiculo.velocidad = 5.0f;
        trafico.alertarPorBocina(jugadorX, jugadorZ, jugadorAngulo);

        // Al sonar la bocina, el taxi debe reaccionar incrementando su velocidad para abrir paso
        assertTrue("El taxi debe acelerar al recibir el bocinazo", taxi.vehiculo.velocidad > 5.0f);
    }

    /** Comprueba la identificación correcta de los sectores urbanos para el HUD. */
    public void testSectoresUrbanos() {
        assertEquals("Sector Noroeste - Residencial", SistemaInterfaz.obtenerSector(-30, -30));
        assertEquals("Sector Noreste - Financiero", SistemaInterfaz.obtenerSector(30, -30));
        assertEquals("Sector Suroeste - Comercial", SistemaInterfaz.obtenerSector(-30, 30));
        assertEquals("Sector Sureste - Industrial", SistemaInterfaz.obtenerSector(30, 30));
        assertEquals("Distrito Centro - Cívico", SistemaInterfaz.obtenerSector(0, 0));
    }

    /** Comprueba que la tecla P o TAB activa y desactiva el estado de pausa. */
    public void testPausaControles() {
        clase4 juego = new clase4();
        // Tecla P activa pausa
        juego.tecla(org.lwjgl.glfw.GLFW.GLFW_KEY_P);
        float zAntes = juego.autoZ;
        juego.velocidad = 10.0f;
        juego.actualizar(1.0f);
        assertEquals(zAntes, juego.autoZ, 0.001f); // Congelado por pausa

        // Tecla P de nuevo desactiva pausa
        juego.tecla(org.lwjgl.glfw.GLFW.GLFW_KEY_P);
        juego.actualizar(0.1f);
        assertTrue("Al reanudar, la física debe continuar", Math.abs(juego.autoZ - zAntes) > 0.001f);

        // Tecla TAB también activa pausa
        juego.tecla(org.lwjgl.glfw.GLFW.GLFW_KEY_TAB);
        float zAntesTab = juego.autoZ;
        juego.actualizar(0.5f);
        assertEquals(zAntesTab, juego.autoZ, 0.001f); // Congelado por tecla TAB

        // Tecla TAB reanuda
        juego.tecla(org.lwjgl.glfw.GLFW.GLFW_KEY_TAB);
        juego.actualizar(0.1f);
        assertTrue("Al reanudar con TAB, la física debe continuar", Math.abs(juego.autoZ - zAntesTab) > 0.001f);
    }

    /**
     * Comprueba las conversiones del tacómetro y velocímetro (km/h y RPM).
     */
    public void testTacometroYVelocimetro() {
        float velMax = 16.0f; // Máxima velocidad de avance
        float kmhMax = velMax * 3.6f;
        assertEquals(57.6f, kmhMax, 0.01f); // Velocidad límite en torno a 58-60 km/h

        float velReversa = -6.0f;
        float kmhReversa = Math.abs(velReversa) * 3.6f;
        assertEquals(21.6f, kmhReversa, 0.01f); // Reversa en torno a 22 km/h
    }

    /**
     * Comprueba que la chata (remolque) permanece matemáticamente enganchada
     * al vehículo del jugador en todo momento, manteniendo la distancia de la lanza
     * tanto en marcha recta como al girar y maniobrar.
     */
    public void testChataSiempreEnganchada() {
        clase2 juego = new clase2();
        Chata chata = juego.getChata();
        assertNotNull(chata);

        // Al inicio, la chata debe estar exactamente detrás del auto a la distancia de la lanza
        float fX = -(float) Math.sin(juego.angulo);
        float fZ = -(float) Math.cos(juego.angulo);
        float hitchXEsperado = juego.autoX - fX * chata.distanciaEnganche;
        float hitchZEsperado = juego.autoZ - fZ * chata.distanciaEnganche;
        assertEquals(hitchXEsperado, chata.hitchX, 0.001f);
        assertEquals(hitchZEsperado, chata.hitchZ, 0.001f);

        // Distancia física entre el enganche y el eje de la chata
        float dx = chata.hitchX - chata.x;
        float dz = chata.hitchZ - chata.z;
        float distLanza = (float) Math.sqrt(dx * dx + dz * dz);
        assertEquals(chata.longitudLanza, distLanza, 0.001f);

        // Simular avance y viraje del vehículo
        juego.velocidad = 8.0f;
        juego.angulo = 0.5f; // Giro hacia la izquierda
        juego.autoX += 5.0f;
        juego.autoZ -= 10.0f;
        chata.actualizar(juego.autoX, juego.autoZ, juego.angulo, juego.velocidad, 0.1f);

        // Tras el movimiento, la distancia entre el nuevo enganche y la chata DEBE seguir siendo longitudLanza
        dx = chata.hitchX - chata.x;
        dz = chata.hitchZ - chata.z;
        distLanza = (float) Math.sqrt(dx * dx + dz * dz);
        assertEquals("La lanza no debe estirarse ni soltarse jamás", chata.longitudLanza, distLanza, 0.005f);

        // Al reiniciar el juego, la chata debe reubicarse directamente detrás
        juego.reiniciar();
        assertEquals(juego.autoX, chata.hitchX, 0.001f);
        assertEquals(juego.autoZ + chata.distanciaEnganche, chata.hitchZ, 0.001f);
        assertEquals(chata.hitchZ + chata.longitudLanza, chata.z, 0.001f);
    }

    /**
     * Comprueba la detección de colisiones de la chata.
     */
    public void testChataColisiones() {
        Chata chata = new Chata();
        chata.reiniciar(0, 0, 0);

        // Colisión sobre el centro de la chata
        assertTrue(chata.colisionaCon(chata.x, chata.z, 1.0f));

        // Separado a 10 metros: no debe haber colisión
        assertFalse(chata.colisionaCon(chata.x + 10.0f, chata.z, 1.0f));
    }

    /**
     * Comprueba que al dar marcha atrás (retroceso), si la chata impacta
     * primero contra un obstáculo o acera, el vehículo se detiene (velocidad = 0)
     * impidiendo que la chata atraviese la estructura.
     */
    public void testColisionChataEnRetroceso() {
        clase2 juego = new clase2();

        // Posicionar el auto en la avenida z = -10, mirando hacia el sur (angulo = PI)
        // La acera de la manzana norte (-20, -20) comienza en z = -15.
        juego.autoX = -20.0f;
        juego.autoZ = -9.5f;
        juego.angulo = (float) Math.PI; // Frente hacia +Z (sur)
        juego.getChata().reiniciar(juego.autoX, juego.autoZ, juego.angulo);

        // El auto está en la avenida y tiene espacio por delante
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));

        // Dar marcha atrás hacia el norte: la chata impactará contra la acera en z = -15
        juego.velocidad = -5.0f;
        for (int frame = 0; frame < 30; frame++) {
            juego.actualizar(0.05f);
            if (juego.velocidad == 0.0f) {
                break; // Se detuvo por el impacto de la chata
            }
        }

        // Debe haberse detenido debido al impacto de la chata
        assertEquals("El vehículo debe detenerse al impactar la chata marcha atrás", 0.0f, juego.velocidad, 0.001f);

        // La chata no debe haber traspasado la acera (el parachoques trasero no debe penetrar más allá de z = -15)
        float chataRearZ = juego.getChata().z - 1.15f; // mirando hacia el sur, trasera está a -Z
        assertTrue("La chata no debe atravesar la acera en z=-15", chataRearZ >= -15.2f);

        // Al acelerar hacia adelante (W), debe poder reanudar la marcha alejándose de la acera
        juego.velocidad = 4.0f;
        float zAntesAvanzar = juego.autoZ;
        juego.actualizar(0.1f);
        assertTrue("Al acelerar hacia adelante, el vehículo debe poder avanzar alejándose del obstáculo",
                juego.autoZ > zAntesAvanzar);
    }

    /**
     * Comprueba que cuando la chata se pliega en tijera máxima (jackknife) al dar
     * marcha atrás (hacia la izquierda o derecha), el vehículo queda paralizado
     * y no puede seguir avanzando hacia atrás, pero sí puede avanzar hacia adelante (W)
     * para enderezar el remolque.
     */
    public void testBloqueoTijeraEnRetroceso() {
        clase2 juego = new clase2();
        juego.autoX = -30.0f;
        juego.autoZ = 30.0f;
        juego.angulo = 0.0f; // Frente hacia -Z
        juego.getChata().reiniciar(juego.autoX, juego.autoZ, juego.angulo);

        // Articular la chata al ángulo máximo de tijera (~70 grados = 1.20 rad)
        juego.getChata().angulo = juego.angulo + Chata.MAX_ANGULO_TIJERA;
        float fChataX = -(float) Math.sin(juego.getChata().angulo);
        float fChataZ = -(float) Math.cos(juego.getChata().angulo);
        juego.getChata().x = juego.getChata().hitchX - fChataX * juego.getChata().longitudLanza;
        juego.getChata().z = juego.getChata().hitchZ - fChataZ * juego.getChata().longitudLanza;

        // Intentar dar marcha atrás en tijera
        juego.velocidad = -4.0f;
        float zAntesRetro = juego.autoZ;
        juego.actualizar(0.1f);

        // El vehículo debe quedar paralizado en retroceso por bloqueo físico contra la chata
        assertEquals("El vehículo debe quedar paralizado marcha atrás al estar en tijera", 0.0f, juego.velocidad, 0.001f);
        assertEquals("El auto no debe moverse hacia atrás", zAntesRetro, juego.autoZ, 0.001f);
        assertTrue("Debe registrarse el estado de bloqueo por tijera", juego.isBloqueadoPorTijera());

        // Ahora avanzar hacia adelante con acelerador (W)
        juego.velocidad = 5.0f;
        juego.actualizar(0.1f);

        // Al avanzar hacia adelante, debe poder avanzar y salir de la tijera
        assertTrue("Al acelerar hacia adelante debe avanzar", juego.autoZ < zAntesRetro);
        assertFalse("El bloqueo debe liberarse al avanzar", juego.isBloqueadoPorTijera());
    }
}
