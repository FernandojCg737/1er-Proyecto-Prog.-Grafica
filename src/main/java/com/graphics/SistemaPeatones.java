package com.graphics;

public class SistemaPeatones {

    public static class Peaton {
        public float x, z;
        public float angulo;
        public float[][] waypoints;
        public int indiceDestino = 0;
        public float velocidad;
        public float faseAnimacion = 0;
        public float r, g, b;

        public float base_y = 0.3f;

        public Peaton(float[][] waypoints, float velocidad, float r, float g, float b) {
            this.waypoints = waypoints;
            this.velocidad = velocidad;
            this.r = r;
            this.g = g;
            this.b = b;
            if (waypoints.length > 0) {
                this.x = waypoints[0][0];
                this.z = waypoints[0][1];
            }
        }

        private float getAlturaSuelo(float px, float pz) {
            int col = (int) Math.floor((px + 55.0f) / 10.0f);
            int fila = (int) Math.floor((pz + 55.0f) / 10.0f);
            if (fila >= 0 && fila < 11 && col >= 0 && col < 11) {
                if (MapaCiudad.MAPA[fila][col] != 0) return 0.3f;
            }
            return 0.0f;
        }

        public void actualizar(float deltaTime, float tiempo, float jugadorX, float jugadorZ, float jugadorRadio,
                               float chataX, float chataZ, float chataRadio, Peaton[] todosPeatones) {
            float objetivoX = waypoints[indiceDestino][0];
            float objetivoZ = waypoints[indiceDestino][1];

            float dx = objetivoX - x;
            float dz = objetivoZ - z;
            float distancia = (float) Math.sqrt(dx * dx + dz * dz);

            if (distancia < 0.2f) {
                indiceDestino = (indiceDestino + 1) % waypoints.length;
                objetivoX = waypoints[indiceDestino][0];
                objetivoZ = waypoints[indiceDestino][1];
                dx = objetivoX - x;
                dz = objetivoZ - z;
            }

            float anguloDeseado = (float) Math.atan2(-dx, -dz);
            float diferencia = anguloDeseado - angulo;
            while (diferencia > (float) Math.PI) diferencia -= 2 * (float) Math.PI;
            while (diferencia < -(float) Math.PI) diferencia += 2 * (float) Math.PI;
            angulo += diferencia * Math.min(1.0f, 15.0f * deltaTime);

            float frenteX = -(float) Math.sin(angulo);
            float frenteZ = -(float) Math.cos(angulo);

            boolean puedeAvanzar = true;

            if (!puedeCruzarSemaforo(x, z, frenteX, frenteZ, tiempo)) {
                puedeAvanzar = false;
            }

            float dxJugador = (x + frenteX * 1.0f) - jugadorX;
            float dzJugador = (z + frenteZ * 1.0f) - jugadorZ;
            if (dxJugador * dxJugador + dzJugador * dzJugador < Math.pow(0.5f + jugadorRadio, 2)) {
                puedeAvanzar = false;
            }
            if (chataRadio > 0) {
                float dxChata = (x + frenteX * 1.0f) - chataX;
                float dzChata = (z + frenteZ * 1.0f) - chataZ;
                if (dxChata * dxChata + dzChata * dzChata < Math.pow(0.5f + chataRadio, 2)) {
                    puedeAvanzar = false;
                }
            }

            // Evitar que los peatones se atraviesen entre sí (colisión frontal)
            for (Peaton otro : todosPeatones) {
                if (otro != null && otro != this) {
                    float dxOtro = (x + frenteX * 0.8f) - otro.x;
                    float dzOtro = (z + frenteZ * 0.8f) - otro.z;
                    if (dxOtro * dxOtro + dzOtro * dzOtro < 0.6f * 0.6f) { // Radio de colisión peatón-peatón
                        puedeAvanzar = false;
                        break;
                    }
                }
            }

            if (puedeAvanzar) {
                x += frenteX * velocidad * deltaTime;
                z += frenteZ * velocidad * deltaTime;
                faseAnimacion += velocidad * deltaTime * 3.0f;
            } else {
                faseAnimacion *= 0.9f;
            }

            float alturaDeseada = getAlturaSuelo(x, z);
            base_y += (alturaDeseada - base_y) * 10.0f * deltaTime;
        }

        public void actualizar(float deltaTime, float tiempo, float jugadorX, float jugadorZ, float jugadorRadio, Peaton[] todosPeatones) {
            actualizar(deltaTime, tiempo, jugadorX, jugadorZ, jugadorRadio, 0, 0, 0, todosPeatones);
        }

        private boolean puedeCruzarSemaforo(float posX, float posZ, float fX, float fZ, float tiempo) {
            float[] avenidas = {-30, -10, 10, 30};
            boolean moviendoEnX = Math.abs(fX) > Math.abs(fZ);

            for (float avenida : avenidas) {
                if (moviendoEnX) {
                    float distAlCruce = (avenida - posX) * (fX > 0 ? 1 : -1);
                    if (distAlCruce > 5.4f && distAlCruce < 6.5f) {
                        if (SistemaSemaforos.getEstadoNorteSur(tiempo) != SistemaSemaforos.EstadoLuz.ROJO) return false;
                    }
                } else {
                    float distAlCruce = (avenida - posZ) * (fZ > 0 ? 1 : -1);
                    if (distAlCruce > 5.4f && distAlCruce < 6.5f) {
                        if (SistemaSemaforos.getEstadoEsteOeste(tiempo) != SistemaSemaforos.EstadoLuz.ROJO) return false;
                    }
                }
            }
            return true;
        }

        public void dibujar(clase1 renderer) {
            float oscilacion = (float) Math.sin(faseAnimacion);
            
            float torsoY = base_y + 0.9f;
            renderer.cajaGirada(x, torsoY, z, 0.4f, 0.6f, 0.2f, r, g, b, angulo);
            
            float cabezaY = base_y + 1.4f;
            renderer.cajaGirada(x, cabezaY, z, 0.35f, 0.35f, 0.35f, 0.9f, 0.7f, 0.5f, angulo);

            // Cara (Roblox style)
            float fX = -(float)Math.sin(angulo);
            float fZ = -(float)Math.cos(angulo);
            float caraX = x + fX * 0.18f;
            float caraZ = z + fZ * 0.18f;
            
            float ojoIzqX = caraX + (float)Math.cos(angulo) * 0.1f;
            float ojoIzqZ = caraZ - (float)Math.sin(angulo) * 0.1f;
            renderer.cajaGirada(ojoIzqX, cabezaY + 0.05f, ojoIzqZ, 0.05f, 0.05f, 0.02f, 0.1f, 0.1f, 0.1f, angulo);
            
            float ojoDerX = caraX - (float)Math.cos(angulo) * 0.1f;
            float ojoDerZ = caraZ + (float)Math.sin(angulo) * 0.1f;
            renderer.cajaGirada(ojoDerX, cabezaY + 0.05f, ojoDerZ, 0.05f, 0.05f, 0.02f, 0.1f, 0.1f, 0.1f, angulo);
            
            renderer.cajaGirada(caraX, cabezaY - 0.1f, caraZ, 0.15f, 0.03f, 0.02f, 0.1f, 0.1f, 0.1f, angulo);

            float piernaZ = 0.1f * oscilacion;
            float pIzqX = x + (float)Math.cos(angulo) * 0.15f - (float)Math.sin(angulo) * piernaZ;
            float pIzqZ = z - (float)Math.sin(angulo) * 0.15f - (float)Math.cos(angulo) * piernaZ;
            renderer.cajaGirada(pIzqX, base_y + 0.3f, pIzqZ, 0.15f, 0.6f, 0.15f, 0.2f, 0.2f, 0.6f, angulo);

            float pDerX = x - (float)Math.cos(angulo) * 0.15f + (float)Math.sin(angulo) * piernaZ;
            float pDerZ = z + (float)Math.sin(angulo) * 0.15f + (float)Math.cos(angulo) * piernaZ;
            renderer.cajaGirada(pDerX, base_y + 0.3f, pDerZ, 0.15f, 0.6f, 0.15f, 0.2f, 0.2f, 0.6f, angulo);

            float brazoZ = -0.15f * oscilacion;
            float bIzqX = x + (float)Math.cos(angulo) * 0.28f - (float)Math.sin(angulo) * brazoZ;
            float bIzqZ = z - (float)Math.sin(angulo) * 0.28f - (float)Math.cos(angulo) * brazoZ;
            renderer.cajaGirada(bIzqX, base_y + 0.9f, bIzqZ, 0.15f, 0.5f, 0.15f, 0.9f, 0.7f, 0.5f, angulo);

            float bDerX = x - (float)Math.cos(angulo) * 0.28f + (float)Math.sin(angulo) * brazoZ;
            float bDerZ = z + (float)Math.sin(angulo) * 0.28f + (float)Math.cos(angulo) * brazoZ;
            renderer.cajaGirada(bDerX, base_y + 0.9f, bDerZ, 0.15f, 0.5f, 0.15f, 0.9f, 0.7f, 0.5f, angulo);
        }
    }

    private final Peaton[] peatones;

    public boolean colisionaCon(float cx, float cz, float radio) {
        for (Peaton p : peatones) {
            if (p != null) {
                float dx = p.x - cx;
                float dz = p.z - cz;
                if (dx * dx + dz * dz < (0.35f + radio) * (0.35f + radio)) {
                    return true;
                }
            }
        }
        return false;
    }

    public SistemaPeatones() {
        peatones = new Peaton[8];
        int index = 0;

        float[][] rutaNorte = { {-35.8f, -35.8f}, { 35.8f, -35.8f}, { 35.8f, -24.2f}, {-35.8f, -24.2f} };
        float[][] rutaEste = { { 35.8f, -35.8f}, { 35.8f,  35.8f}, { 24.2f,  35.8f}, { 24.2f, -35.8f} };
        float[][] rutaSur = { { 35.8f,  35.8f}, {-35.8f,  35.8f}, {-35.8f,  24.2f}, { 35.8f,  24.2f} };
        float[][] rutaOeste = { {-35.8f,  35.8f}, {-35.8f, -35.8f}, {-24.2f, -35.8f}, {-24.2f,  35.8f} };

        peatones[index++] = new Peaton(rutaNorte, 1.8f, 0.8f, 0.2f, 0.2f);
        peatones[index++] = new Peaton(rutaEste, 2.1f, 0.2f, 0.8f, 0.2f);
        peatones[index++] = new Peaton(rutaSur, 1.9f, 0.2f, 0.2f, 0.8f);
        peatones[index++] = new Peaton(rutaOeste, 2.0f, 0.8f, 0.8f, 0.2f);

        // Bloques centrales están en 0 (de -5 a 5). La acera está dentro de esos límites.
        float[][] rutaHorizontalCentro = { {-35.8f, -4.2f}, { 35.8f, -4.2f}, { 35.8f,  4.2f}, {-35.8f,  4.2f} };
        float[][] rutaVerticalCentro = { {-4.2f, -35.8f}, {-4.2f,  35.8f}, { 4.2f,  35.8f}, { 4.2f, -35.8f} };

        peatones[index++] = new Peaton(rutaHorizontalCentro, 2.2f, 0.8f, 0.4f, 0.8f);
        peatones[index] = new Peaton(rutaHorizontalCentro, 1.7f, 0.1f, 0.8f, 0.8f);
        peatones[index].indiceDestino = 2; // Empieza desde el otro extremo
        index++;
        
        peatones[index++] = new Peaton(rutaVerticalCentro, 2.3f, 0.8f, 0.8f, 0.8f);
        peatones[index] = new Peaton(rutaVerticalCentro, 1.6f, 0.4f, 0.4f, 0.4f);
        peatones[index].indiceDestino = 2; // Empieza desde el otro extremo
    }

    public void actualizar(float deltaTime, float tiempo, float jugadorX, float jugadorZ, float jugadorRadio,
                           float chataX, float chataZ, float chataRadio) {
        for (Peaton p : peatones) {
            p.actualizar(deltaTime, tiempo, jugadorX, jugadorZ, jugadorRadio, chataX, chataZ, chataRadio, peatones);
        }
    }

    public void actualizar(float deltaTime, float tiempo, float jugadorX, float jugadorZ, float jugadorRadio) {
        actualizar(deltaTime, tiempo, jugadorX, jugadorZ, jugadorRadio, 0, 0, 0);
    }

    public void dibujar(clase1 renderer) {
        for (Peaton p : peatones) {
            p.dibujar(renderer);
        }
    }
}
