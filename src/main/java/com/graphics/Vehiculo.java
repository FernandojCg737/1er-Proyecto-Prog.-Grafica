package com.graphics;

/**
 * Entidad de vehículo que encapsula su geometría 3D por piezas,
 * física de conducción (aceleración, resistencia y giro) y estado.
 * Se reutiliza tanto para el coche del jugador como para los vehículos del tráfico autónomo.
 */
public class Vehiculo {

    public float x;
    public float z;
    public float angulo; // Radianes; 0 apunta hacia -Z
    public float velocidad; // Unidades por segundo (m/s)
    public final float radio = 1.65f; // Radio para colisiones

    // Colores personalizables
    public float colorR;
    public float colorG;
    public float colorB;
    public float cabinaR = 0.22f;
    public float cabinaG = 0.65f;
    public float cabinaB = 0.78f;

    public Vehiculo(float x, float z, float angulo, float r, float g, float b) {
        this.x = x;
        this.z = z;
        this.angulo = angulo;
        this.velocidad = 0.0f;
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
    }

    /** Restablece la posición, orientación y detiene el vehículo. */
    public void reiniciar(float xInicio, float zInicio, float anguloInicio) {
        this.x = xInicio;
        this.z = zInicio;
        this.angulo = anguloInicio;
        this.velocidad = 0.0f;
    }

    /** Distancia euclidiana hacia una coordenada horizontal. */
    public float distanciaA(float ox, float oz) {
        float dx = this.x - ox;
        float dz = this.z - oz;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    /** Comprueba si una posición propuesta colisiona con este vehículo. */
    public boolean colisionaCon(float ox, float oz, float otroRadio) {
        float dx = this.x - ox;
        float dz = this.z - oz;
        float distMin = this.radio + otroRadio - 0.2f;
        return (dx * dx + dz * dz) < (distMin * distMin);
    }

    /** Actualiza la física del coche según los mandos del usuario. */
    public void actualizarFisicaUsuario(float deltaTime, boolean acelera, boolean retrocede,
                                         boolean frenoMano, boolean giraIzq, boolean giraDer) {
        float acelerador = 0.0f;
        if (acelera) {
            acelerador += 1.0f;
        }
        if (retrocede) {
            acelerador -= 1.0f;
        }

        velocidad += acelerador * 9.0f * deltaTime;
        float resistencia = frenoMano ? 7.0f : 0.7f;
        float factorFrenado = (float) Math.exp(-resistencia * deltaTime);
        velocidad *= factorFrenado;
        velocidad = Math.max(-6.0f, Math.min(16.0f, velocidad));

        float direccion = 0.0f;
        if (giraIzq) {
            direccion += 1.0f;
        }
        if (giraDer) {
            direccion -= 1.0f;
        }

        angulo += direccion * velocidad * 0.11f * deltaTime;
        float frenteX = -(float) Math.sin(angulo);
        float frenteZ = -(float) Math.cos(angulo);

        float sigX = x + frenteX * velocidad * deltaTime;
        float sigZ = z + frenteZ * velocidad * deltaTime;

        if (MapaCiudad.puedeCircular(sigX, sigZ, radio)) {
            x = sigX;
            z = sigZ;
        } else {
            velocidad = 0.0f; // Detener en caso de colisión
        }
    }

    /** Dibuja el coche compuesto por piezas en el espacio del mundo con luces encendidas o apagadas. */
    public void dibujar(clase1 renderer, boolean lucesEncendidas) {
        // Sombra de contacto suave sobre el asfalto
        renderer.cajaGirada(x, 0.03f, z, 2.2f, 0.02f, 3.2f, 0.08f, 0.10f, 0.13f, angulo);

        // Carrocería principal
        pieza(renderer, 0, 0.65f, 0, 1.65f, 0.55f, 2.6f, colorR, colorG, colorB);
        // Cabina
        pieza(renderer, 0, 1.12f, 0.12f, 1.3f, 0.55f, 1.25f, cabinaR, cabinaG, cabinaB);

        // 4 ruedas
        float[] ladosRuedas = {-0.88f, 0.88f};
        float[] ejesRuedas = {-0.82f, 0.82f};
        for (float rx : ladosRuedas) {
            for (float rz : ejesRuedas) {
                pieza(renderer, rx, 0.38f, rz, 0.24f, 0.58f, 0.6f, 0.055f, 0.065f, 0.08f);
            }
        }

        // Faros delanteros y luces traseras (controlados automáticamente según sea día o noche)
        float[] ladosFaros = {-0.55f, 0.55f};
        for (float fx : ladosFaros) {
            if (lucesEncendidas) {
                renderer.entero("uEmision", 1);
                pieza(renderer, fx, 0.68f, -1.32f, 0.38f, 0.2f, 0.07f, 1.0f, 0.96f, 0.68f); // Faro encendido
                pieza(renderer, fx, 0.68f, 1.32f, 0.35f, 0.17f, 0.07f, 0.95f, 0.08f, 0.08f); // Luz trasera encendida
                renderer.entero("uEmision", 0);
            } else {
                pieza(renderer, fx, 0.68f, -1.32f, 0.38f, 0.2f, 0.07f, 0.36f, 0.40f, 0.44f); // Faro apagado
                pieza(renderer, fx, 0.68f, 1.32f, 0.35f, 0.17f, 0.07f, 0.42f, 0.10f, 0.10f); // Luz trasera apagada
            }
        }
    }

    /** Dibuja el coche con luces apagadas por defecto. */
    public void dibujar(clase1 renderer) {
        dibujar(renderer, false);
    }

    /** Transforma las coordenadas locales del vehículo a coordenadas del mundo y dibuja la caja girada. */
    private void pieza(clase1 renderer, float px, float py, float pz,
                       float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(angulo);
        float seno = (float) Math.sin(angulo);
        float mundoX = x + coseno * px + seno * pz;
        float mundoZ = z - seno * px + coseno * pz;
        renderer.cajaGirada(mundoX, py, mundoZ, sx, sy, sz, r, g, b, angulo);
    }
}
