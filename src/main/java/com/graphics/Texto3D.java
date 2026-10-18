package com.graphics;

public class Texto3D {
    // Basic 5x3 pixel font for A-Z
    private static final String[] FONT = {
        "A:010,101,111,101,101",
        "B:110,101,110,101,110",
        "C:011,100,100,100,011",
        "D:110,101,101,101,110",
        "E:111,100,110,100,111",
        "F:111,100,110,100,100",
        "G:011,100,101,101,011",
        "H:101,101,111,101,101",
        "I:111,010,010,010,111",
        "J:001,001,001,101,010",
        "K:101,101,110,101,101",
        "L:100,100,100,100,111",
        "M:101,111,101,101,101",
        "N:111,101,101,101,101",
        "O:010,101,101,101,010",
        "P:110,101,110,100,100",
        "Q:010,101,101,010,001",
        "R:110,101,110,101,101",
        "S:011,100,010,001,110",
        "T:111,010,010,010,010",
        "U:101,101,101,101,011",
        "V:101,101,101,101,010",
        "W:101,101,101,111,101",
        "X:101,101,010,101,101",
        "Y:101,101,010,010,010",
        "Z:111,001,010,100,111",
        " :000,000,000,000,000",
        "0:010,101,101,101,010",
        "1:010,110,010,010,111",
        "2:110,001,010,100,111",
        "3:110,001,011,001,110",
        "4:101,101,111,001,001",
        "5:111,100,110,001,110",
        "6:011,100,110,101,011",
        "7:111,001,001,010,010",
        "8:010,101,010,101,010",
        "9:010,101,011,001,010",
        "[:110,100,100,100,110",
        "]:011,001,001,001,011",
        "|:010,010,010,010,010",
        "-:000,000,111,000,000",
        "::000,010,000,010,000"
    };

    public static float calcularAncho(String texto, float scale) {
        return texto.length() * (4 * scale);
    }

    public static void dibujar(clase1 renderer, String texto, float startX, float startY, float startZ, float scale, float r, float g, float b, float anguloY) {
        texto = texto.toUpperCase();
        float cursorX = 0;
        
        float cos = (float)Math.cos(anguloY);
        float sin = (float)Math.sin(anguloY);

        renderer.entero("uEmision", 1);
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            String[] sprite = getSprite(c);
            
            for (int fila = 0; fila < 5; fila++) {
                for (int col = 0; col < 3; col++) {
                    if (sprite[fila].charAt(col) == '1') {
                        float lx = cursorX + (col * scale);
                        float ly = startY + ((4 - fila) * scale); // Invert Y
                        float lz = 0;
                        
                        // Rotar alrededor de Y
                        float wX = startX + (lx * cos + lz * sin);
                        float wZ = startZ + (-lx * sin + lz * cos);
                        
                        renderer.cajaGirada(wX, ly, wZ, scale*1.05f, scale*1.05f, scale*0.2f, r, g, b, anguloY);
                    }
                }
            }
            cursorX += 4 * scale; // Move cursor for next letter
        }
        renderer.entero("uEmision", 0);
    }
    
    private static String[] getSprite(char c) {
        for (String f : FONT) {
            if (f.charAt(0) == c) {
                return f.substring(2).split(",");
            }
        }
        return FONT[26].substring(2).split(","); // Espacio
    }
}
