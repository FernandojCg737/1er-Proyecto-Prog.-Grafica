package com.graphics;

/**
 * Representa la chata enganchada a la parte trasera del vehiculo del jugador.
 * Gestiona su cinematica de arrastre, articulacion con limite de tijera,
 * colision perimetrica y dibujo 3D.
 */
public class Chata {

    public float x;
    public float z;
    public float angulo;
    public float hitchX;
    public float hitchZ;

    public final float distanciaEnganche = 1.48f;
    public final float longitudLanza = 2.10f;
    public final float radio = 1.25f;
    public static final float MAX_ANGULO_TIJERA = 1.20f;

    /**
     * Representa un estado propuesto (candidato) para la chata antes de que el
     * motor fisico acepte o rechace el movimiento por colision.
     */
    public static class EstadoCandidato {
        public final float x;
        public final float z;
        public final float angulo;
        public final float hitchX;
        public final float hitchZ;

        public EstadoCandidato(float x, float z, float angulo, float hitchX, float hitchZ) {
            this.x = x;
            this.z = z;
            this.angulo = angulo;
            this.hitchX = hitchX;
            this.hitchZ = hitchZ;
        }

        /** Transforma una coordenada local (px, pz) del remolque a coordenadas mundiales X. */
        public float transformarX(float px, float pz) {
            float cos = (float) Math.cos(angulo);
            float sin = (float) Math.sin(angulo);
            return x + cos * px + sin * pz;
        }

        /** Transforma una coordenada local (px, pz) del remolque a coordenadas mundiales Z. */
        public float transformarZ(float px, float pz) {
            float cos = (float) Math.cos(angulo);
            float sin = (float) Math.sin(angulo);
            return z - sin * px + cos * pz;
        }
    }

    /**
     * Malla de puntos de colision perimetrica que cubre la chata y sus extremos [px, pz, radio].
     */
    public static final float[][] PUNTOS_COLISION = {
        { 0.00f,  1.15f, 0.35f},
        {-0.75f,  1.15f, 0.30f},
        { 0.75f,  1.15f, 0.30f},
        {-0.40f,  1.15f, 0.30f},
        { 0.40f,  1.15f, 0.30f},
        {-0.95f,  0.00f, 0.30f},
        { 0.95f,  0.00f, 0.30f},
        {-0.75f,  0.60f, 0.28f},
        { 0.75f,  0.60f, 0.28f},
        {-0.75f, -0.60f, 0.28f},
        { 0.75f, -0.60f, 0.28f},
        {-0.75f, -1.10f, 0.30f},
        { 0.75f, -1.10f, 0.30f},
        { 0.00f, -1.10f, 0.32f},
        { 0.00f,  0.00f, 0.75f},
        { 0.00f, -1.65f, 0.25f}
    };

    public Chata() {
        reiniciar(-30.0f, 30.0f, 0.0f);
    }

    /**
     * Posiciona y alinea la chata inmediatamente detras del vehiculo.
     */
    public void reiniciar(float autoX, float autoZ, float autoAngulo) {
        float fX = -(float) Math.sin(autoAngulo);
        float fZ = -(float) Math.cos(autoAngulo);
        this.hitchX = autoX - fX * distanciaEnganche;
        this.hitchZ = autoZ - fZ * distanciaEnganche;
        this.angulo = autoAngulo;
        this.x = this.hitchX - fX * longitudLanza;
        this.z = this.hitchZ - fZ * longitudLanza;
    }

    /**
     * Calcula la posicion y orientacion propuesta (candidata) para la chata
     * a partir de la nueva posicion y rotacion tentativa del auto.
     */
    public EstadoCandidato calcularCandidato(float candAutoX, float candAutoZ, float candAutoAngulo,
                                            float autoX, float autoZ, float autoAngulo) {
        float fX = -(float) Math.sin(candAutoAngulo);
        float fZ = -(float) Math.cos(candAutoAngulo);
        float candHitchX = candAutoX - fX * distanciaEnganche;
        float candHitchZ = candAutoZ - fZ * distanciaEnganche;

        float dHx = candHitchX - hitchX;
        float dHz = candHitchZ - hitchZ;

        float candAngulo = this.angulo;
        float saltoSq = dHx * dHx + dHz * dHz;
        if (saltoSq > 16.0f) {
            candAngulo = candAutoAngulo;
        } else if (saltoSq > 1e-7f) {
            float lateral = dHx * (float) Math.cos(candAngulo) - dHz * (float) Math.sin(candAngulo);
            float deltaAngulo = -lateral / longitudLanza;
            deltaAngulo = Math.max(-0.25f, Math.min(0.25f, deltaAngulo));
            candAngulo += deltaAngulo;

            float diff = candAngulo - candAutoAngulo;
            while (diff > (float) Math.PI) diff -= 2.0f * (float) Math.PI;
            while (diff < -(float) Math.PI) diff += 2.0f * (float) Math.PI;
            if (diff > MAX_ANGULO_TIJERA) candAngulo = candAutoAngulo + MAX_ANGULO_TIJERA;
            if (diff < -MAX_ANGULO_TIJERA) candAngulo = candAutoAngulo - MAX_ANGULO_TIJERA;
        }

        float fChataX = -(float) Math.sin(candAngulo);
        float fChataZ = -(float) Math.cos(candAngulo);
        float candX = candHitchX - fChataX * longitudLanza;
        float candZ = candHitchZ - fChataZ * longitudLanza;

        return new EstadoCandidato(candX, candZ, candAngulo, candHitchX, candHitchZ);
    }

    /**
     * Comprueba si la articulacion de la chata ha alcanzado el angulo limite de tijera (jackknife).
     */
    public boolean estaEnTijera(EstadoCandidato cand, float autoAngulo) {
        if (cand == null) return false;
        float diff = cand.angulo - autoAngulo;
        while (diff > (float) Math.PI) diff -= 2.0f * (float) Math.PI;
        while (diff < -(float) Math.PI) diff += 2.0f * (float) Math.PI;
        return Math.abs(diff) >= (MAX_ANGULO_TIJERA - 0.015f);
    }

    /**
     * Aplica el estado candidato aprobado a la chata activa.
     */
    public void aplicarCandidato(EstadoCandidato cand) {
        if (cand == null) return;
        this.x = cand.x;
        this.z = cand.z;
        this.angulo = cand.angulo;
        this.hitchX = cand.hitchX;
        this.hitchZ = cand.hitchZ;
    }

    /**
     * Comprueba si la chata propuesta y todos sus extremos caben en la calzada sin colisionar.
     */
    public boolean puedeCircular(EstadoCandidato cand) {
        if (cand == null) return true;
        for (float[] punto : PUNTOS_COLISION) {
            float wx = cand.transformarX(punto[0], punto[1]);
            float wz = cand.transformarZ(punto[0], punto[1]);
            float r = punto[2];
            if (!MapaCiudad.puedeCircular(wx, wz, r)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sobrecarga para verificar transitabilidad dadas coordenadas explicitas.
     */
    public boolean puedeCircular(float cx, float cz, float cAngulo) {
        EstadoCandidato cand = new EstadoCandidato(cx, cz, cAngulo, hitchX, hitchZ);
        return puedeCircular(cand);
    }

    /**
     * Actualiza la cinematica de arrastre segun la posicion del auto.
     */
    public void actualizar(float autoX, float autoZ, float autoAngulo, float velocidad, float deltaTime) {
        EstadoCandidato cand = calcularCandidato(autoX, autoZ, autoAngulo, autoX, autoZ, autoAngulo);
        aplicarCandidato(cand);
    }

    /**
     * Comprueba si una posicion propuesta colisiona con el volumen de la chata.
     */
    public boolean colisionaCon(float ox, float oz, float otroRadio) {
        float dx = this.x - ox;
        float dz = this.z - oz;
        float distMin = this.radio + otroRadio - 0.2f;
        return (dx * dx + dz * dz) < (distMin * distMin);
    }

    /**
     * Dibuja el modelo 3D de la chata en la escena.
     */
    public void dibujar(clase1 renderer, boolean lucesEncendidas) {
        renderer.cajaGirada(x, 0.03f, z, 1.85f, 0.02f, 2.7f, 0.08f, 0.10f, 0.13f, angulo);

        pieza(renderer, 0, 0.44f, -longitudLanza, 0.16f, 0.16f, 0.18f, 0.22f, 0.24f, 0.26f);
        pieza(renderer, 0, 0.54f, -longitudLanza, 0.08f, 0.10f, 0.08f, 0.85f, 0.85f, 0.85f);
        pieza(renderer, 0, 0.42f, -1.62f, 0.12f, 0.10f, 0.98f, 0.28f, 0.30f, 0.34f);
        pieza(renderer, -0.30f, 0.42f, -1.55f, 0.08f, 0.08f, 0.88f, 0.28f, 0.30f, 0.34f);
        pieza(renderer, 0.30f, 0.42f, -1.55f, 0.08f, 0.08f, 0.88f, 0.28f, 0.30f, 0.34f);
        pieza(renderer, 0.16f, 0.38f, -1.82f, 0.06f, 0.22f, 0.06f, 0.65f, 0.65f, 0.70f);
        pieza(renderer, 0.16f, 0.24f, -1.82f, 0.08f, 0.08f, 0.08f, 0.14f, 0.14f, 0.16f);

        pieza(renderer, 0, 0.42f, 0, 1.50f, 0.10f, 2.30f, 0.22f, 0.24f, 0.27f);
        pieza(renderer, 0, 0.48f, 0, 1.42f, 0.04f, 2.22f, 0.60f, 0.44f, 0.28f);
        pieza(renderer, -0.35f, 0.49f, 0, 0.02f, 0.02f, 2.20f, 0.46f, 0.32f, 0.18f);
        pieza(renderer, 0.35f, 0.49f, 0, 0.02f, 0.02f, 2.20f, 0.46f, 0.32f, 0.18f);

        pieza(renderer, 0, 0.68f, -1.11f, 1.44f, 0.36f, 0.08f, 0.90f, 0.22f, 0.14f);
        pieza(renderer, 0, 0.68f, 1.11f, 1.44f, 0.36f, 0.08f, 0.88f, 0.20f, 0.12f);
        pieza(renderer, -0.68f, 0.68f, 0, 0.08f, 0.36f, 2.22f, 0.90f, 0.22f, 0.14f);
        pieza(renderer, 0.68f, 0.68f, 0, 0.08f, 0.36f, 2.22f, 0.90f, 0.22f, 0.14f);

        pieza(renderer, -0.68f, 0.70f, -1.11f, 0.12f, 0.42f, 0.12f, 0.24f, 0.26f, 0.28f);
        pieza(renderer, 0.68f, 0.70f, -1.11f, 0.12f, 0.42f, 0.12f, 0.24f, 0.26f, 0.28f);
        pieza(renderer, -0.68f, 0.70f, 1.11f, 0.12f, 0.42f, 0.12f, 0.24f, 0.26f, 0.28f);
        pieza(renderer, 0.68f, 0.70f, 1.11f, 0.12f, 0.42f, 0.12f, 0.24f, 0.26f, 0.28f);

        pieza(renderer, 0, 0.88f, -1.11f, 1.44f, 0.05f, 0.05f, 0.80f, 0.82f, 0.86f);
        pieza(renderer, -0.68f, 0.88f, 0, 0.05f, 0.05f, 2.22f, 0.80f, 0.82f, 0.86f);
        pieza(renderer, 0.68f, 0.88f, 0, 0.05f, 0.05f, 2.22f, 0.80f, 0.82f, 0.86f);

        pieza(renderer, 0, 0.36f, 0, 1.80f, 0.08f, 0.08f, 0.20f, 0.20f, 0.22f);
        pieza(renderer, -0.85f, 0.38f, 0, 0.22f, 0.58f, 0.60f, 0.055f, 0.065f, 0.08f);
        pieza(renderer, 0.85f, 0.38f, 0, 0.22f, 0.58f, 0.60f, 0.055f, 0.065f, 0.08f);
        pieza(renderer, -0.92f, 0.38f, 0, 0.10f, 0.30f, 0.30f, 0.75f, 0.78f, 0.82f);
        pieza(renderer, 0.92f, 0.38f, 0, 0.10f, 0.30f, 0.30f, 0.75f, 0.78f, 0.82f);

        pieza(renderer, -0.85f, 0.70f, 0, 0.24f, 0.06f, 0.74f, 0.22f, 0.24f, 0.26f);
        pieza(renderer, -0.85f, 0.56f, -0.34f, 0.24f, 0.24f, 0.06f, 0.22f, 0.24f, 0.26f);
        pieza(renderer, -0.85f, 0.56f, 0.34f, 0.24f, 0.24f, 0.06f, 0.22f, 0.24f, 0.26f);
        pieza(renderer, 0.85f, 0.70f, 0, 0.24f, 0.06f, 0.74f, 0.22f, 0.24f, 0.26f);
        pieza(renderer, 0.85f, 0.56f, -0.34f, 0.24f, 0.24f, 0.06f, 0.22f, 0.24f, 0.26f);
        pieza(renderer, 0.85f, 0.56f, 0.34f, 0.24f, 0.24f, 0.06f, 0.22f, 0.24f, 0.26f);

        float[] ladosLuces = {-0.52f, 0.52f};
        for (float lx : ladosLuces) {
            if (lucesEncendidas) {
                renderer.entero("uEmision", 1);
                pieza(renderer, lx, 0.62f, 1.16f, 0.22f, 0.14f, 0.04f, 0.98f, 0.10f, 0.08f);
                renderer.entero("uEmision", 0);
            } else {
                pieza(renderer, lx, 0.62f, 1.16f, 0.22f, 0.14f, 0.04f, 0.45f, 0.10f, 0.10f);
            }
        }

        pieza(renderer, -0.28f, 0.62f, 1.16f, 0.10f, 0.10f, 0.03f, 1.0f, 0.30f, 0.05f);
        pieza(renderer, 0.28f, 0.62f, 1.16f, 0.10f, 0.10f, 0.03f, 1.0f, 0.30f, 0.05f);

        pieza(renderer, 0, 0.52f, 1.16f, 0.38f, 0.14f, 0.03f, 0.94f, 0.94f, 0.90f);

        pieza(renderer, -0.73f, 0.60f, -0.55f, 0.03f, 0.08f, 0.16f, 1.0f, 0.65f, 0.05f);
        pieza(renderer, -0.73f, 0.60f, 0.55f, 0.03f, 0.08f, 0.16f, 1.0f, 0.65f, 0.05f);
        pieza(renderer, 0.73f, 0.60f, -0.55f, 0.03f, 0.08f, 0.16f, 1.0f, 0.65f, 0.05f);
        pieza(renderer, 0.73f, 0.60f, 0.55f, 0.03f, 0.08f, 0.16f, 1.0f, 0.65f, 0.05f);
    }

    /**
     * Dibuja la chata y su lanza de acople en la vista cenital del minimapa.
     */
    public void dibujarEnMinimapa(clase1 renderer) {
        renderer.cajaGirada(x, 25, z, 1.6f, 0.1f, 2.2f, 1.0f, 0.65f, 0.15f, angulo);
        float midX = (hitchX + x) * 0.5f;
        float midZ = (hitchZ + z) * 0.5f;
        renderer.cajaGirada(midX, 25, midZ, 0.30f, 0.1f, longitudLanza, 0.85f, 0.85f, 0.85f, angulo);
    }

    /**
     * Transforma coordenadas locales de la chata a coordenadas mundiales y la dibuja rotada.
     */
    private void pieza(clase1 renderer, float px, float py, float pz,
                       float sx, float sy, float sz, float r, float g, float b) {
        float cos = (float) Math.cos(angulo);
        float sin = (float) Math.sin(angulo);
        float mundoX = x + cos * px + sin * pz;
        float mundoZ = z - sin * px + cos * pz;
        renderer.cajaGirada(mundoX, py, mundoZ, sx, sy, sz, r, g, b, angulo);
    }
}
