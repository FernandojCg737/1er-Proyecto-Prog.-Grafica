package com.graphics;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * Gestiona los efectos de sonido sintetizados proceduralmente mediante Java Sound API (javax.sound.sampled).
 * - Control por pulsación continua: suena ininterrumpidamente mientras se mantenga presionada la tecla (H o B).
 * - Al soltar la tecla, se detiene de inmediato con un micro-fade out para evitar chasquidos.
 * - Síntesis bitonal auténtica (435 Hz y 515 Hz con armónico a 870 Hz) con bucle armónico continuo.
 */
public class SistemaAudio {

    private static final int SAMPLE_RATE = 44100;
    // Bucle armónico perfecto: MCD(435, 515) = 5 Hz -> período fundamental de 0.20s = 8820 muestras (17640 bytes)
    private static final int SAMPLES_BUCLE = 8820;
    private static final byte[] BUFFER_BUCLE = generarBucleBocina();

    private static volatile boolean activa = false;
    private static volatile boolean hiloIniciado = false;
    private static final Object lock = new Object();
    private static SourceDataLine lineaAudio = null;

    private static final ExecutorService ejecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Audio-Bocina");
        t.setDaemon(true);
        return t;
    });

    /** Genera un ciclo continuo de 0.20 s donde todas las frecuencias completan un número entero exacto de ciclos. */
    private static byte[] generarBucleBocina() {
        byte[] buffer = new byte[SAMPLES_BUCLE * 2];
        float f1 = 435.0f; // 435 * 0.2 = 87 ciclos exactos
        float f2 = 515.0f; // 515 * 0.2 = 103 ciclos exactos
        float f3 = 870.0f; // 870 * 0.2 = 174 ciclos exactos

        for (int i = 0; i < SAMPLES_BUCLE; i++) {
            float t = (float) i / SAMPLE_RATE;
            float muestra = (0.45f * (float) Math.sin(2.0 * Math.PI * f1 * t)
                           + 0.45f * (float) Math.sin(2.0 * Math.PI * f2 * t)
                           + 0.10f * (float) Math.sin(2.0 * Math.PI * f3 * t));

            short valor16 = (short) (muestra * 32767.0f * 0.85f);
            buffer[i * 2] = (byte) (valor16 & 0xFF);
            buffer[i * 2 + 1] = (byte) ((valor16 >> 8) & 0xFF);
        }
        return buffer;
    }

    /**
     * Actualiza el estado de la bocina según si la tecla está presionada o no.
     * @param presionado true mientras se mantenga presionada la tecla (H o B); false al soltarla.
     */
    public static void setBocinaPresionada(boolean presionado) {
        synchronized (lock) {
            activa = presionado;
            if (presionado && !hiloIniciado) {
                hiloIniciado = true;
                ejecutor.submit(SistemaAudio::bucleAudio);
            }
            lock.notifyAll();
        }
    }

    /** Ciclo continuo de reproducción en segundo plano. */
    private static void bucleAudio() {
        try {
            AudioFormat formato = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            lineaAudio = AudioSystem.getSourceDataLine(formato);
            int tamBufferLinea = 4096; // ~46 ms para balance óptimo de baja latencia y estabilidad
            lineaAudio.open(formato, tamBufferLinea);

            int offset = 0;
            int chunkLen = 1024; // ~11.6 ms por fragmento
            byte[] bloqueAtaque = new byte[chunkLen];

            while (true) {
                synchronized (lock) {
                    while (!activa) {
                        lock.wait();
                    }
                }

                lineaAudio.start();

                // Ataque suave inicial para eliminar cualquier chasquido o artefacto
                System.arraycopy(BUFFER_BUCLE, offset, bloqueAtaque, 0, chunkLen);
                aplicarEnvolventeAtaque(bloqueAtaque);
                lineaAudio.write(bloqueAtaque, 0, chunkLen);
                offset = (offset + chunkLen) % BUFFER_BUCLE.length;

                // Transmisión continua mientras la tecla se mantenga presionada
                while (activa) {
                    int bytesAEscribir = Math.min(chunkLen, BUFFER_BUCLE.length - offset);
                    lineaAudio.write(BUFFER_BUCLE, offset, bytesAEscribir);
                    offset = (offset + bytesAEscribir) % BUFFER_BUCLE.length;
                }

                // Al soltar la tecla: micro-fade out rápido y detención inmediata
                byte[] bloqueSalida = new byte[chunkLen];
                int bytesFinal = Math.min(chunkLen, BUFFER_BUCLE.length - offset);
                System.arraycopy(BUFFER_BUCLE, offset, bloqueSalida, 0, bytesFinal);
                aplicarEnvolventeDecaimiento(bloqueSalida, bytesFinal);
                lineaAudio.write(bloqueSalida, 0, bytesFinal);

                lineaAudio.drain();
                lineaAudio.stop();
                lineaAudio.flush();
            }
        } catch (Exception e) {
            // Silencioso si no hay dispositivo de audio disponible en el sistema
        } finally {
            synchronized (lock) {
                hiloIniciado = false;
            }
            if (lineaAudio != null) {
                try {
                    lineaAudio.close();
                } catch (Exception ignored) {}
            }
        }
    }

    private static void aplicarEnvolventeAtaque(byte[] datos) {
        int muestras = datos.length / 2;
        int duracionAtaque = Math.min(muestras, (int) (SAMPLE_RATE * 0.015f)); // 15 ms
        for (int i = 0; i < duracionAtaque; i++) {
            float factor = (float) i / duracionAtaque;
            short val = (short) ((datos[i * 2] & 0xFF) | (datos[i * 2 + 1] << 8));
            val = (short) (val * factor);
            datos[i * 2] = (byte) (val & 0xFF);
            datos[i * 2 + 1] = (byte) ((val >> 8) & 0xFF);
        }
    }

    private static void aplicarEnvolventeDecaimiento(byte[] datos, int bytesValidos) {
        int muestras = bytesValidos / 2;
        for (int i = 0; i < muestras; i++) {
            float factor = 1.0f - ((float) i / muestras);
            short val = (short) ((datos[i * 2] & 0xFF) | (datos[i * 2 + 1] << 8));
            val = (short) (val * factor);
            datos[i * 2] = (byte) (val & 0xFF);
            datos[i * 2 + 1] = (byte) ((val >> 8) & 0xFF);
        }
    }

    public static boolean isBocinaActiva() {
        return activa;
    }

    /** Toca la bocina puntualmente por 300 ms (compatibilidad con pruebas unitarias o llamadas puntuales). */
    public static void tocarBocina() {
        setBocinaPresionada(true);
        ejecutor.submit(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {}
            setBocinaPresionada(false);
        });
    }
}
