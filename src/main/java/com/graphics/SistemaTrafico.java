package com.graphics;

/**
 * Gestiona el sistema de tráfico autónomo (Mejora opcional para bonificación de +10 pts).
 * - Circulación estricta por el CARRIL DERECHO (+2.3 m del centro de la calle, sin pisar la línea amarilla).
 * - Detección inteligente de obstáculos al frente (frenado automático ante el jugador u otros autos).
 * - Colisión física bidireccional: ni el jugador ni el tráfico pueden atravesarse.
 */
public class SistemaTrafico {

    public static class AutoAutonomo {
        public final Vehiculo vehiculo;
        public final float[][] waypoints;
        public int indiceDestino = 0;
        public final float velocidadCrucero;

        public AutoAutonomo(Vehiculo vehiculo, float[][] waypoints, float velocidad) {
            this.vehiculo = vehiculo;
            this.waypoints = waypoints;
            this.velocidadCrucero = velocidad;
            this.vehiculo.velocidad = velocidad;
            if (waypoints.length > 0) {
                this.vehiculo.x = waypoints[0][0];
                this.vehiculo.z = waypoints[0][1];
            }
        }

        /**
         * Actualiza el movimiento por el carril derecho y aplica frenado preventivo si hay autos al frente.
         */
        public void actualizar(float deltaTime, float jugadorX, float jugadorZ, float jugadorRadio,
                               float chataX, float chataZ, float chataRadio,
                               AutoAutonomo[] otrosAutos, float tiempo) {
            float objetivoX = waypoints[indiceDestino][0];
            float objetivoZ = waypoints[indiceDestino][1];

            float dx = objetivoX - vehiculo.x;
            float dz = objetivoZ - vehiculo.z;
            float distancia = (float) Math.sqrt(dx * dx + dz * dz);

            // Al aproximarse al punto de giro, pasar al siguiente tramo
            if (distancia < 0.5f) {
                indiceDestino = (indiceDestino + 1) % waypoints.length;
                objetivoX = waypoints[indiceDestino][0];
                objetivoZ = waypoints[indiceDestino][1];
                dx = objetivoX - vehiculo.x;
                dz = objetivoZ - vehiculo.z;
            }

            // Orientación deseada hacia el siguiente punto del carril
            float anguloDeseado = (float) Math.atan2(-dx, -dz);
            float diferencia = anguloDeseado - vehiculo.angulo;
            while (diferencia > (float) Math.PI) diferencia -= 2 * (float) Math.PI;
            while (diferencia < -(float) Math.PI) diferencia += 2 * (float) Math.PI;
            vehiculo.angulo += diferencia * Math.min(1.0f, 12.0f * deltaTime);

            float frenteX = -(float) Math.sin(vehiculo.angulo);
            float frenteZ = -(float) Math.cos(vehiculo.angulo);

            // Detección de obstáculos frontales (jugador u otros vehículos)
            float velocidadObjetivo = velocidadCrucero;

            // 1. Respetar semáforos alternados en las intersecciones (rojo = parar, amarillo = frenar, verde = avanzar)
            float factorSemaforo = SistemaSemaforos.evaluarSemaforo(vehiculo.x, vehiculo.z, frenteX, frenteZ, tiempo);
            velocidadObjetivo *= factorSemaforo;

            // 2. Verificar si el jugador o su chata están adelante en el mismo carril
            velocidadObjetivo = calcularFrenadoObstaculo(jugadorX, jugadorZ, jugadorRadio, frenteX, frenteZ, velocidadObjetivo);
            if (chataRadio > 0) {
                velocidadObjetivo = calcularFrenadoObstaculo(chataX, chataZ, chataRadio, frenteX, frenteZ, velocidadObjetivo);
            }

            // 3. Verificar si hay otro auto de tráfico adelante
            for (AutoAutonomo otro : otrosAutos) {
                if (otro != this) {
                    velocidadObjetivo = calcularFrenadoObstaculo(otro.vehiculo.x, otro.vehiculo.z, otro.vehiculo.radio,
                                                                 frenteX, frenteZ, velocidadObjetivo);
                }
            }

            // Ajuste suave de velocidad
            if (vehiculo.velocidad < velocidadObjetivo) {
                vehiculo.velocidad = Math.min(velocidadObjetivo, vehiculo.velocidad + 6.0f * deltaTime);
            } else {
                vehiculo.velocidad = Math.max(velocidadObjetivo, vehiculo.velocidad - 14.0f * deltaTime);
            }

            // Desplazamiento
            float sigX = vehiculo.x + frenteX * vehiculo.velocidad * deltaTime;
            float sigZ = vehiculo.z + frenteZ * vehiculo.velocidad * deltaTime;

            // Detección estricta de colisiones (evitar atravesar físicamente al jugador o su chata)
            float dxJugador = sigX - jugadorX;
            float dzJugador = sigZ - jugadorZ;
            boolean colisionJugador = (dxJugador * dxJugador + dzJugador * dzJugador) < Math.pow(vehiculo.radio + jugadorRadio, 2);
            if (!colisionJugador && chataRadio > 0) {
                float dxChata = sigX - chataX;
                float dzChata = sigZ - chataZ;
                colisionJugador = (dxChata * dxChata + dzChata * dzChata) < Math.pow(vehiculo.radio + chataRadio, 2);
            }

            boolean colisionTrafico = false;
            for (AutoAutonomo otro : otrosAutos) {
                if (otro != this) {
                    float dxOtro = sigX - otro.vehiculo.x;
                    float dzOtro = sigZ - otro.vehiculo.z;
                    if ((dxOtro * dxOtro + dzOtro * dzOtro) < Math.pow(vehiculo.radio + otro.vehiculo.radio, 2)) {
                        colisionTrafico = true;
                        break;
                    }
                }
            }

            // Verificar límites con el entorno y evitar superposición
            if (!colisionJugador && !colisionTrafico && MapaCiudad.puedeCircular(sigX, sigZ, vehiculo.radio)) {
                vehiculo.x = sigX;
                vehiculo.z = sigZ;
            } else {
                if (colisionJugador || colisionTrafico) {
                    // Detenerse inmediatamente al impactar con otro vehículo
                    vehiculo.velocidad = 0.0f;
                } else {
                    // Si choca con el entorno, deslizarse suavemente hacia el objetivo para destrabarse (sin teletransporte)
                    vehiculo.velocidad = 2.0f;
                    vehiculo.x += (dx / distancia) * 2.0f * deltaTime;
                    vehiculo.z += (dz / distancia) * 2.0f * deltaTime;
                }
            }
        }

        /** Calcula la velocidad reducida según la distancia a un obstáculo detectado al frente. */
        private float calcularFrenadoObstaculo(float ox, float oz, float oradio, float fx, float fz, float velActual) {
            float vx = ox - vehiculo.x;
            float vz = oz - vehiculo.z;
            float dist = (float) Math.sqrt(vx * vx + vz * vz);

            if (dist < 0.1f) return 0.0f;

            // Producto punto para saber si está en el cono frontal de visión (coseno > 0.6)
            float alineacion = (vx * fx + vz * fz) / dist;
            if (alineacion > 0.55f) { // Dentro del cono frontal de avance
                if (dist < 3.8f) {
                    return 0.0f; // Detención total para evitar colisión
                } else if (dist < 8.5f) {
                    float factor = (dist - 3.8f) / (8.5f - 3.8f);
                    return Math.min(velActual, velocidadCrucero * factor);
                }
            }
            return velActual;
        }

        /** Sobrecarga de compatibilidad sin chata. */
        public void actualizar(float deltaTime, float jugadorX, float jugadorZ, float jugadorRadio, AutoAutonomo[] otrosAutos, float tiempo) {
            actualizar(deltaTime, jugadorX, jugadorZ, jugadorRadio, 0, 0, 0, otrosAutos, tiempo);
        }
    }

    private final AutoAutonomo[] autos;

    public SistemaTrafico() {
        autos = new AutoAutonomo[3];

        // 1. Taxi amarillo: Circuito horario por las avenidas intermedias (+-30) en CARRIL DERECHO (+-27.7)
        float[][] ruta1 = {
            {-27.7f, -27.7f}, // Hacia el Norte por carril derecho
            { 27.7f, -27.7f}, // Giro al Este por carril derecho
            { 27.7f,  27.7f}, // Giro al Sur por carril derecho
            {-27.7f,  27.7f}  // Giro al Oeste por carril derecho
        };
        Vehiculo v1 = new Vehiculo(-27.7f, 10.0f, 0, 1.0f, 0.82f, 0.15f);
        v1.cabinaR = 0.15f; v1.cabinaG = 0.15f; v1.cabinaB = 0.18f;
        autos[0] = new AutoAutonomo(v1, ruta1, 7.5f);

        // 2. Deportivo azul: Circuito horario por el anillo exterior (+-50) en CARRIL DERECHO (+-47.7)
        float[][] ruta2 = {
            {-47.7f, -47.7f},
            { 47.7f, -47.7f},
            { 47.7f,  47.7f},
            {-47.7f,  47.7f}
        };
        Vehiculo v2 = new Vehiculo(-47.7f, 20.0f, 0, 0.12f, 0.45f, 0.95f);
        autos[1] = new AutoAutonomo(v2, ruta2, 8.5f);

        // 3. Sedán blanco perla: Circuito horario por avenidas interiores (+-10) en CARRIL DERECHO (+-7.7)
        float[][] ruta3 = {
            {-7.7f, -7.7f},
            { 7.7f, -7.7f},
            { 7.7f,  7.7f},
            {-7.7f,  7.7f}
        };
        Vehiculo v3 = new Vehiculo(-7.7f, -7.7f, 0, 0.92f, 0.94f, 0.96f);
        v3.cabinaR = 0.20f; v3.cabinaG = 0.22f; v3.cabinaB = 0.28f;
        autos[2] = new AutoAutonomo(v3, ruta3, 6.8f);
    }

    /** Comprueba si una posición propuesta colisiona físicamente con alguno de los vehículos del tráfico. */
    public boolean colisionaConTrafico(float x, float z, float radio) {
        for (AutoAutonomo a : autos) {
            if (a.vehiculo.colisionaCon(x, z, radio)) {
                return true;
            }
        }
        return false;
    }

    /** Actualiza la cinemática, carril derecho, respeto de semáforos y evasión de obstáculos con chata. */
    public void actualizar(float deltaTime, float jugadorX, float jugadorZ, float jugadorRadio,
                           float chataX, float chataZ, float chataRadio, float tiempo) {
        for (AutoAutonomo auto : autos) {
            auto.actualizar(deltaTime, jugadorX, jugadorZ, jugadorRadio, chataX, chataZ, chataRadio, autos, tiempo);
        }
    }

    /** Actualiza la cinemática, carril derecho, respeto de semáforos y evasión de obstáculos. */
    public void actualizar(float deltaTime, float jugadorX, float jugadorZ, float jugadorRadio, float tiempo) {
        actualizar(deltaTime, jugadorX, jugadorZ, jugadorRadio, 0, 0, 0, tiempo);
    }

    /** Sobrecarga para compatibilidad sin tiempo. */
    public void actualizar(float deltaTime, float jugadorX, float jugadorZ, float jugadorRadio) {
        actualizar(deltaTime, jugadorX, jugadorZ, jugadorRadio, 0.0f);
    }

    /** Dibuja todos los autos autónomos con sus luces encendidas de noche y apagadas de día. */
    public void dibujar(clase1 renderer, boolean noche) {
        for (AutoAutonomo auto : autos) {
            auto.vehiculo.dibujar(renderer, noche);
        }
    }

    /** Sobrecarga para dibujo básico con luces apagadas. */
    public void dibujar(clase1 renderer) {
        dibujar(renderer, false);
    }

    /** Dibuja los indicadores de los vehículos en el minimapa. */
    public void dibujarEnMinimapa(clase1 renderer) {
        for (AutoAutonomo auto : autos) {
            Vehiculo v = auto.vehiculo;
            renderer.cajaGirada(v.x, 24, v.z, 2.0f, 0.1f, 3.0f, v.colorR, v.colorG, v.colorB, v.angulo);
        }
    }

    /** Reinicia la posición de los vehículos de tráfico a sus puntos iniciales del carril. */
    public void reiniciar() {
        for (AutoAutonomo auto : autos) {
            auto.indiceDestino = 0;
            auto.vehiculo.x = auto.waypoints[0][0];
            auto.vehiculo.z = auto.waypoints[0][1];
            auto.vehiculo.angulo = 0;
            auto.vehiculo.velocidad = auto.velocidadCrucero;
        }
    }

    public int getCantidadAutos() {
        return autos.length;
    }

    public AutoAutonomo[] getAutos() {
        return autos;
    }

    /** Alerta a los vehículos de tráfico que estén adelante del jugador al sonar la bocina. */
    public void alertarPorBocina(float jugadorX, float jugadorZ, float jugadorAngulo) {
        float fx = -(float) Math.sin(jugadorAngulo);
        float fz = -(float) Math.cos(jugadorAngulo);

        for (AutoAutonomo a : autos) {
            float dx = a.vehiculo.x - jugadorX;
            float dz = a.vehiculo.z - jugadorZ;
            float dist = (float) Math.sqrt(dx * dx + dz * dz);

            // Si está adelante del jugador (dentro de 16 metros y cono frontal de 45 grados)
            if (dist > 1.0f && dist < 16.0f) {
                float alineacion = (dx * fx + dz * fz) / dist;
                if (alineacion > 0.45f) {
                    // El vehículo reacciona acelerando para despejar la vía
                    a.vehiculo.velocidad = Math.min(a.velocidadCrucero * 1.25f, 10.5f);
                }
            }
        }
    }
}
