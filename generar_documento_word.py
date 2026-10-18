import os
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

def set_cell_background(cell, fill_hex):
    tcPr = cell._element.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_hex}"/>')
    tcPr.append(shd)

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    tcPr = cell._element.get_or_add_tcPr()
    tcMar = parse_xml(f'<w:tcMar {nsdecls("w")}>'
                      f'<w:top w:w="{top}" w:type="dxa"/>'
                      f'<w:bottom w:w="{bottom}" w:type="dxa"/>'
                      f'<w:left w:w="{left}" w:type="dxa"/>'
                      f'<w:right w:w="{right}" w:type="dxa"/>'
                      f'</w:tcMar>')
    tcPr.append(tcMar)

def set_table_borders(table, color="CCCCCC", sz="4", val="single"):
    tblPr = table._element.xpath('w:tblPr')
    if tblPr:
        borders = parse_xml(f'<w:tblBorders {nsdecls("w")}>'
                            f'<w:top w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
                            f'<w:bottom w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
                            f'<w:left w:val="none"/>'
                            f'<w:right w:val="none"/>'
                            f'<w:insideH w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
                            f'<w:insideV w:val="none"/>'
                            f'</w:tblBorders>')
        tblPr[0].append(borders)

def create_document():
    doc = docx.Document()

    # Configuración de márgenes de página (2.5 cm = ~0.98 pulgadas)
    for section in doc.sections:
        section.top_margin = Inches(0.98)
        section.bottom_margin = Inches(0.98)
        section.left_margin = Inches(0.98)
        section.right_margin = Inches(0.98)
        section.different_first_page_header_footer = False
        
        # Pie de página con numeración y título
        footer = section.footer
        f_p = footer.paragraphs[0]
        f_p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        f_run = f_p.add_run("Proyecto Ciudad y Conducción 3D | Documentación Técnica")
        f_run.font.name = "Calibri"
        f_run.font.size = Pt(8.5)
        f_run.font.color.rgb = RGBColor(120, 130, 140)

    # Colores temáticos
    C_PRIMARY = RGBColor(27, 54, 93)     # Azul marino institucional #1B365D
    C_SECONDARY = RGBColor(46, 107, 158) # Azul cobalto #2E6B9E
    C_DARK = RGBColor(40, 45, 55)        # Gris texto principal #282D37
    C_MUTED = RGBColor(100, 110, 125)    # Gris secundario
    C_ACCENT = RGBColor(200, 138, 36)    # Ámbar dorado

    # --- PORTADA / ENCABEZADO PRINCIPAL ---
    p_meta = doc.add_paragraph()
    p_meta.paragraph_format.space_before = Pt(0)
    p_meta.paragraph_format.space_after = Pt(4)
    r_meta = p_meta.add_run("PROGRAMACIÓN GRÁFICA • PROYECTO INTEGRADOR POR ETAPAS")
    r_meta.font.name = "Calibri"
    r_meta.font.size = Pt(9.5)
    r_meta.font.bold = True
    r_meta.font.color.rgb = C_ACCENT

    p_title = doc.add_paragraph()
    p_title.paragraph_format.space_before = Pt(0)
    p_title.paragraph_format.space_after = Pt(6)
    r_title = p_title.add_run("Ciudad 3D y Simulación de Conducción Urbana")
    r_title.font.name = "Calibri"
    r_title.font.size = Pt(24)
    r_title.font.bold = True
    r_title.font.color.rgb = C_PRIMARY

    p_sub = doc.add_paragraph()
    p_sub.paragraph_format.space_before = Pt(0)
    p_sub.paragraph_format.space_after = Pt(14)
    r_sub = p_sub.add_run("Documento Breve de Diseño, Controles, Arquitectura, Limitaciones y Guía de Ejecución")
    r_sub.font.name = "Calibri"
    r_sub.font.size = Pt(12)
    r_sub.font.color.rgb = C_SECONDARY

    # Tarjeta de metadatos del proyecto
    tbl_meta = doc.add_table(rows=1, cols=3)
    tbl_meta.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl_meta.autofit = False
    set_table_borders(tbl_meta, color="E2E8F0")

    col_widths = [Inches(2.2), Inches(2.2), Inches(2.2)]
    meta_data = [
        ("Lenguaje y Gráficos", "Java 17 / OpenGL 3.3 Core Profile\nLWJGL 3.3.3 + GLSL"),
        ("Módulos Integrados", "11x11 Grid, IA Tráfico, Entregas\nHUD Doble Esfera, Menú Pausa"),
        ("Estado de Pruebas", "13/13 Tests Unitarios Aprobados\n100% Procedural sin Assets")
    ]
    for i, (k, v) in enumerate(meta_data):
        cell = tbl_meta.rows[0].cells[i]
        cell.width = col_widths[i]
        set_cell_background(cell, "F8FAFC")
        set_cell_margins(cell, top=120, bottom=120, left=150, right=150)
        cp = cell.paragraphs[0]
        cp.paragraph_format.space_after = Pt(2)
        rk = cp.add_run(k + "\n")
        rk.font.name = "Calibri"
        rk.font.size = Pt(8.5)
        rk.font.bold = True
        rk.font.color.rgb = C_PRIMARY
        rv = cp.add_run(v)
        rv.font.name = "Calibri"
        rv.font.size = Pt(8)
        rv.font.color.rgb = C_MUTED

    doc.add_paragraph().paragraph_format.space_after = Pt(8)

    def add_h1(text):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(14)
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(text)
        run.font.name = "Calibri"
        run.font.size = Pt(14)
        run.font.bold = True
        run.font.color.rgb = C_PRIMARY
        return p

    def add_h2(text):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(10)
        p.paragraph_format.space_after = Pt(3)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(text)
        run.font.name = "Calibri"
        run.font.size = Pt(11.5)
        run.font.bold = True
        run.font.color.rgb = C_SECONDARY
        return p

    def add_body(text, bold_prefix=None):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.line_spacing = 1.15
        if bold_prefix:
            rb = p.add_run(bold_prefix)
            rb.font.name = "Calibri"
            rb.font.size = Pt(9.5)
            rb.font.bold = True
            rb.font.color.rgb = C_DARK
        r = p.add_run(text)
        r.font.name = "Calibri"
        r.font.size = Pt(9.5)
        r.font.color.rgb = C_DARK
        return p

    def add_callout(text, title=None):
        tbl = doc.add_table(rows=1, cols=1)
        tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
        cell = tbl.rows[0].cells[0]
        cell.width = Inches(6.6)
        set_cell_background(cell, "F1F5F9")
        set_cell_margins(cell, top=140, bottom=140, left=200, right=200)
        
        # Borde izquierdo grueso azul cobalto
        tcPr = cell._element.get_or_add_tcPr()
        borders = parse_xml(f'<w:tcBorders {nsdecls("w")}>'
                            f'<w:left w:val="single" w:sz="24" w:space="0" w:color="2E6B9E"/>'
                            f'<w:top w:val="none"/>'
                            f'<w:right w:val="none"/>'
                            f'<w:bottom w:val="none"/>'
                            f'</w:tcBorders>')
        tcPr.append(borders)
        
        cp = cell.paragraphs[0]
        cp.paragraph_format.space_after = Pt(2)
        if title:
            rt = cp.add_run(title + "\n")
            rt.font.name = "Calibri"
            rt.font.size = Pt(9.5)
            rt.font.bold = True
            rt.font.color.rgb = C_PRIMARY
        rtxt = cp.add_run(text)
        rtxt.font.name = "Calibri"
        rtxt.font.size = Pt(9)
        rtxt.font.italic = True
        rtxt.font.color.rgb = C_DARK
        doc.add_paragraph().paragraph_format.space_after = Pt(4)

    # ==================== SECCIÓN 1: DISEÑO DE LA CIUDAD ====================
    add_h1("1. Descripción del Diseño y Distribución Urbana")
    add_body(
        "El entorno tridimensional se estructura a partir de una matriz espacial continua de 11x11 celdas "
        "interconectadas que cubre una superficie total de asfalto de 110 × 110 unidades mundiales (delimitada "
        "estrictamente en el rango de -55 a +55 en los ejes X y Z). Dicha escala garantiza el despliegue de una urbe "
        "completa, funcional y transitable, diseñada bajo las siguientes premisas arquitectónicas:"
    )

    add_body(
        " Distribución de 12 manzanas edificadas y 4 parques temáticos: Las manzanas edificadas cuentan con "
        "rascacielos y bloques corporativos de altura volumétrica variable (entre 7 y 28 unidades) con paletas "
        "cromáticas distintivas (marfil corporativo, azul acero, terracota residencial, gris granito) y fachadas "
        "detalladas mediante matrices de ventanas procedimentales iluminadas. Los 4 parques temáticos (Distrito Verde "
        "/ Parque Central y parques auxiliares en cuadrantes) incorporan césped natural, árboles tridimensionales "
        "compuestos por troncos leñosos y copas poliédricas de follaje, bancos de madera de descanso y farolas viales.",
        bold_prefix="• Zonificación y Manzanas: "
    )

    add_body(
        " Coherencia vial y accesibilidad absoluta: Todas las calles poseen un ancho reglamentario de 10 unidades "
        "con avenidas perimetrales e interiores de doble vía. Todas las manzanas, destinos de entrega y cruces "
        "están perfectamente conectados por calzadas asfaltadas transitables sin callejones sin salida ni obstáculos "
        "que impidan la circulación continua.",
        bold_prefix="• Red de Circulación: "
    )

    add_body(
        " Señalización y mobiliario urbano: Cada una de las intersecciones viales incorpora pasos peatonales "
        "cebrados reglamentarios de acera a acera con franjas amarillas de seguridad, marcas longitudinales discontinuas "
        "en los carriles, tótems direccionales con flechas de señalización urbana en las esquinas, 16 farolas de "
        "iluminación nocturna distribuidas homogéneamente y semáforos animados con alternancia cromática de fases.",
        bold_prefix="• Señalización e Identidad: "
    )

    add_body(
        " División sectorial por cuadrantes: La urbe se divide en cinco distritos reconocibles: Sector Noroeste "
        "(Residencial), Sector Noreste (Financiero), Sector Suroeste (Comercial), Sector Sureste (Industrial) y Distrito "
        "Centro (Cívico y Parque Central), reflejados activamente en el cuadro de mandos del vehículo.",
        bold_prefix="• Sectores Urbanos: "
    )

    # ==================== SECCIÓN 2: CONTROLES ====================
    add_h1("2. Guía de Controles del Sistema")
    add_body(
        "El vehículo del jugador y el entorno se controlan en tiempo real mediante el teclado. La siguiente tabla "
        "resume el mapa completo de asignación de teclas, su función y su alcance operativo:"
    )

    tbl_ctrl = doc.add_table(rows=1, cols=3)
    tbl_ctrl.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl_ctrl.autofit = False
    set_table_borders(tbl_ctrl, color="CBD5E0")

    headers_ctrl = ["Tecla(s)", "Acción y Comportamiento Dinámico", "Etapa"]
    col_w_ctrl = [Inches(1.5), Inches(4.3), Inches(0.8)]
    for j, h in enumerate(headers_ctrl):
        c = tbl_ctrl.rows[0].cells[j]
        c.width = col_w_ctrl[j]
        set_cell_background(c, "1B365D")
        set_cell_margins(c, top=80, bottom=80, left=100, right=100)
        p = c.paragraphs[0]
        r = p.add_run(h)
        r.font.name = "Calibri"
        r.font.size = Pt(9)
        r.font.bold = True
        r.font.color.rgb = RGBColor(255, 255, 255)

    data_ctrl = [
        ("W  /  Flecha Arriba", "Aceleración continua del motor hacia delante (+9 u/s²; hasta 60 km/h). Reactiva aguja de RPM.", "Clase 2+"),
        ("S  /  Flecha Abajo", "Freno motor progresivo y marcha atrás / reversa (hasta -22 km/h).", "Clase 2+"),
        ("A / D  o  Flechas Izq/Der", "Dirección del volante; ángulo de giro reactivo a la velocidad de marcha.", "Clase 2+"),
        ("ESPACIO", "Freno de mano instantáneo con desaceleración exponencial rápida.", "Clase 2+"),
        ("C", "Alternar cíclicamente los 4 modos de cámara estilo GTA (Cercana, Lejana, Capó, Aérea).", "Clase 2+"),
        ("H  /  B", "Bocina continua bitonal (435/515 Hz) mientras se mantenga pulsada; alerta al tráfico.", "Clase 2+"),
        ("N", "Alternar ciclo Día / Noche (activa iluminación lunar, cielo nocturno y encendido de farolas).", "Clase 3+"),
        ("F", "Interruptor manual de faros delanteros del vehículo del jugador (haces cónicos de luz).", "Clase 3+"),
        ("M", "Mostrar / Ocultar radar minimapa cenital interactivo en la esquina superior derecha.", "Clase 4"),
        ("P  /  TAB", "Pausar o reanudar el juego; despliega la guía interactiva y congela física y cronómetro.", "Clase 4"),
        ("R", "Reiniciar partida: restaura vehículo, entregas, tráfico autónomo y cronómetro a cero.", "Clase 2+"),
        ("ESC", "Cerrar la aplicación de manera ordenada liberando los recursos OpenGL y GLFW.", "Todas")
    ]

    for idx, (k, desc, et) in enumerate(data_ctrl):
        row = tbl_ctrl.add_row()
        bg = "F8FAFC" if idx % 2 == 0 else "FFFFFF"
        for j, val in enumerate([k, desc, et]):
            cell = row.cells[j]
            cell.width = col_w_ctrl[j]
            set_cell_background(cell, bg)
            set_cell_margins(cell, top=60, bottom=60, left=100, right=100)
            p = cell.paragraphs[0]
            p.paragraph_format.space_after = Pt(0)
            p.paragraph_format.line_spacing = 1.05
            r = p.add_run(val)
            r.font.name = "Calibri"
            r.font.size = Pt(8.5)
            if j == 0:
                r.font.bold = True
                r.font.color.rgb = C_PRIMARY
            elif j == 2:
                r.font.color.rgb = C_MUTED
            else:
                r.font.color.rgb = C_DARK

    doc.add_paragraph().paragraph_format.space_after = Pt(6)

    # ==================== SECCIÓN 3: FUNCIONES IMPLEMENTADAS ====================
    add_h1("3. Funciones Implementadas y Arquitectura Técnica")
    add_body(
        "El proyecto se construyó bajo una arquitectura modular y progresiva en 4 clases principales que heredan "
        "y amplían componentes, cumpliendo con la totalidad de los requisitos de la rúbrica de evaluación y bonificaciones:"
    )

    add_h2("A. Pipeline Gráfico y Renderizado Procedural (Clase 1)")
    add_body(
        "• Shaders GLSL Personalizados: Vertex shader y Fragment shader programados en GLSL 3.30 Core sin intermediarios. "
        "Incorporan 3 modos de proyección gestionados mediante el uniform 'uMapa':\n"
        "   - uMapa = 0: Cámara tridimensional con perspectiva y relación de aspecto dinámica (55° FOV).\n"
        "   - uMapa = 1: Proyección ortográfica cenital para el minimapa superior con orden de profundidad invertido.\n"
        "   - uMapa = 2: Proyección bidimensional directa en pantalla con rotación 2D en plano XY para agujas del tablero y HUD.\n"
        "• Geometría Explícita mediante VAO y VBO: Un único cubo canónico de 36 vértices con normales explícitas "
        "en memoria de video GPU. A través de matrices de escalado, traslación y rotación ('caja' y 'cajaGirada') se "
        "instancian todos los edificios, calzadas, farolas, vehículos, agujas y paneles."
    )

    add_h2("B. Física de Conducción, Colisiones y Cámaras GTA (Clase 2)")
    add_body(
        "• Cinemática Vehicular Realista: Aceleración por integración continua (9 u/s²), rozamiento exponencial de rodadura, "
        "reversa suave (-6 u/s) y velocidad punta de avance de 16 u/s (equivalente a ~58-60 km/h reales).\n"
        "• Detección Continua de Colisiones: Modelo de proximidad círculo-rectángulo que impide invasiones de aceras, choques "
        "contra edificios, obstáculos urbanos, salida del perímetro perimetral (55 unidades) o colisión con otros vehículos.\n"
        "• Cuatro Modos de Cámara Estilo GTA (Tecla C): Cercana (seguimiento suave tras el maletero), Lejana (órbita amplia de "
        "seguimiento), Capó/Primera persona (vista de conducción inmersiva al ras) y Aérea cenital GTA clásico."
    )

    add_h2("C. Iluminación Dinámica Diurna/Nocturna y Focos de Vehículo (Clase 3)")
    add_body(
        "• Ciclo Día / Noche (Tecla N): El ambiente diurno presenta cielo celeste brillante (#3D9EFA), luz ambiental clara "
        "y luz direccional solar cálida tipo Lambertiana. El ambiente nocturno reduce la luz ambiental a tonos azulados oscuros "
        "(#0A101F) con luz lunar fría y encendido automático de las 16 farolas urbanas.\n"
        "• Iluminación Puntual por Farolas: 16 fuentes de luz cálida (1.0, 0.73, 0.34) con atenuación cuadrática en función "
        "de la distancia física (1.0 / (1.0 + 0.12*d + 0.045*d²)) calculada por fragmento.\n"
        "• Faros Cónicos Delanteros del Auto (Tecla F): Doble cono proyector frontal con haz angular suavizado mediante la "
        "función 'smoothstep(0.85, 0.97, alineacion)' que ilumina el asfalto y fachadas nocturnas."
    )

    add_h2("D. Cuadro de Mandos Deportivo HUD y Menú de Pausa (SistemaInterfaz)")
    add_body(
        "• Tacómetro Analógico (RPM): Esfera circular graduada de 0 a 8 (x1000 RPM) con marcas radiales, rótulos 'RPM' y "
        "'x1000', zona roja ('Redline') en arco vivo de 5.5k a 8.0k RPM y aguja analógica deportiva naranja con inercia de "
        "motor reactiva que sube dinámicamente al presionar W/Acelerador y desciende al ralentí (1000 RPM) al frenar o soltar.\n"
        "• Velocímetro Analógico (KM/H): Esfera circular graduada de 0 a 60 km/h con aguja analógica azul ártico y display "
        "digital integrado de 7 segmentos LED que proyecta numéricamente la velocidad exacta en tiempo real.\n"
        "• Consola Central de Testigos: Indicadores luminosos de marcha (P en rojo, D en verde, R en ámbar), testigo de faros "
        "(LUCES ON en verde / LUCES OFF en gris), modo ambiental (DIA / NOCHE) y testigo luminoso de claxon.\n"
        "• Menú de Pausa Profesional (Teclas P / TAB): Superposición translúcida con tarjeta dorada, tipografía vectorial procedural "
        "3x5 y teclas 3D estilizadas con sus nombres y descripciones legibles, congelando la física y el cronómetro."
    )

    add_h2("E. Tráfico Autónomo Inteligente con Respeto de Semáforos (SistemaTrafico)")
    add_body(
        "• Vehículos Autónomos en Red Vial: Múltiples unidades (Taxi urbano, Autobús, Sedan, Coupé deportivo) con colores y "
        "carrocerías diferenciadas que patrullan la ciudad.\n"
        "• Circulación Estricta por Carril Derecho: Trayectorias basadas en waypoints desplazados ±2.3 unidades respecto al eje "
        "central de la calle, respetando las normas de tránsito vehicular.\n"
        "• Respeto Obligatorio de Semáforos: Los vehículos monitorean la señalización del cruce frontal y realizan una frenada suave "
        "ante luces rojas o amarillas, reanudando la marcha únicamente cuando el eje se encuentra en verde.\n"
        "• Respuesta Activa a la Bocina: Al sonar el claxon del jugador (H/B), los vehículos de tráfico que se encuentran delante "
        "en el cono frontal aceleran para ceder el paso."
    )

    add_h2("F. Sistema de Entregas y Semáforos Sincronizados (SistemaEntregas y SistemaSemaforos)")
    add_body(
        "• Misiones de Entrega: Sistema secuencial a 4 destinos urbanos representados mediante balizas luminosas verticales doradas. "
        "Para completar una entrega, el jugador debe detenerse a menos de 3 unidades del destino con velocidad inferior a 1 u/s.\n"
        "• Semáforos Temporizados: Ciclo continuo de 14 segundos (verde 5s, amarillo 2s, rojo 5s) con alternancia estricta entre "
        "calles Norte-Sur y Este-Oeste, garantizando que nunca coincidan ambos ejes en verde."
    )

    add_h2("G. Motor de Audio Sintético Procedural (SistemaAudio)")
    add_body(
        "• Síntesis Acústica en Tiempo Real: Generación bitonal simultánea de frecuencias automotrices de bocina (435 Hz y 515 Hz) "
        "mediante la API estándar Java Sound (javax.sound.sampled) en formato PCM estéreo de 44.1 kHz a 16 bits. Se reproduce de forma "
        "continua mientras se mantenga presionada la tecla H o B y cesa inmediatamente al soltarla."
    )

    # ==================== SECCIÓN 4: LIMITACIONES ====================
    add_h1("4. Limitaciones del Sistema")
    add_body(
        "En concordancia con el alcance didáctico y los requerimientos estrictos de independencia de librerías externas del proyecto, "
        "se reconocen las siguientes limitaciones técnicas deliberadas:"
    )
    add_body(
        "• Geometría Exclusivamente Paramétrica: No se realiza carga de modelos 3D en formatos externos (.obj, .fbx, .gltf); "
        "toda la ciudad, vehículos, árboles y detalles están modelados proceduralmente mediante prismas y rotaciones ortogonales."
    )
    add_body(
        "• Ausencia de Mapas de Sombras Proyectadas (Shadow Mapping): La iluminación difusa y cónica se evalúa por fragmento en tiempo "
        "real mediante el modelo Lambertiano, por lo que las superficies no proyectan sombras oclusivas duras sobre otras estructuras."
    )
    add_body(
        "• Cinemática Vehicular en el Plano Terrestre: El vehículo opera sobre la rasante de la calzada (plano Y = constante), sin "
        "simulación de suspensión física avanzada, vuelcos, deformación de carrocería ni saltos parabólicos en 3D."
    )
    add_body(
        "• Dependencia de Contexto Gráfico de Ventana: Requiere un entorno gráfico con soporte nativo de OpenGL 3.3 Core Profile; "
        "no puede ejecutarse en terminales de servidor headless que carezcan de servidor X o subsistema de ventanas."
    )

    # ==================== SECCIÓN 5: INSTRUCCIONES DE EJECUCIÓN ====================
    add_h1("5. Instrucciones de Compilación y Ejecución")
    add_body(
        "Para ejecutar el proyecto se requiere Java Development Kit (JDK 17 o superior) y Apache Maven instalados y configurados "
        "en las variables de entorno del sistema."
    )

    add_callout(
        "Comandos de consola (ejecutar en la raíz del proyecto donde se ubica pom.xml):\n\n"
        "1. Compilar todo el proyecto y verificar dependencias:\n"
        "   mvn clean compile\n\n"
        "2. Ejecutar la versión final completa (Recomendado - Clase 4):\n"
        "   mvn compile exec:exec -DmainClass=com.graphics.clase4\n\n"
        "3. Ejecutar las etapas individuales de enseñanza:\n"
        "   mvn compile exec:exec -DmainClass=com.graphics.clase1  (Ciudad base y cámara orbital)\n"
        "   mvn compile exec:exec -DmainClass=com.graphics.clase2  (Conducción, colisiones y cámaras)\n"
        "   mvn compile exec:exec -DmainClass=com.graphics.clase3  (Iluminación diurna/nocturna y faros)\n\n"
        "4. Ejecutar la suite automatizada de pruebas unitarias:\n"
        "   mvn test",
        title="INSTRUCCIONES DE EJECUCIÓN PASO A PASO"
    )

    add_body(
        "Al ejecutar 'mvn test', el sistema ejecuta automáticamente los 13 casos de prueba en ConduccionTest, validando la "
        "transitabilidad urbana, colisiones contra aceras y vehículos, carril derecho del tráfico, alternancia segura de semáforos, "
        "bocina y pausas, obteniendo el 100% de éxito (BUILD SUCCESS)."
    )

    # ==================== SECCIÓN 6: RECURSOS EXTERNOS ====================
    add_h1("6. Declaración de Recursos y Fragmentos Externos Utilizados")
    add_body(
        "En cumplimiento de los principios de transparencia académica y originalidad, se declaran los siguientes recursos:"
    )

    add_body(
        "• Librerías de Terceros (Gestionadas en pom.xml):\n"
        "   - LWJGL 3.3.3 (Lightweight Java Game Library): Enlaces oficiales a GLFW para creación de ventana y eventos de entrada, "
        "y bindings de OpenGL 3.3 Core Profile.\n"
        "   - JUnit 3.8.1: Marco para la ejecución de pruebas unitarias de lógica y física en modo sin ventana."
    )

    add_body(
        "• Recursos Multimedia y Assets Externos (0% Utilizados):\n"
        "   - 0 Imágenes / Texturas externas: No se utiliza ningún archivo PNG, JPG o BMP. Todo color, patrón, asfalto y cristal es procedural.\n"
        "   - 0 Modelos 3D externos: No se utiliza ningún archivo OBJ o FBX. Toda la geometría se genera en código mediante VAO/VBO.\n"
        "   - 0 Archivos de Audio: No se utiliza ningún archivo WAV o MP3. La bocina es sintetizada en tiempo real mediante cálculo senoidal.\n"
        "   - 0 Fuentes tipográficas: El texto del HUD y pausa utiliza un rasterizador de matrices de bits 3x5 implementado en Java puro."
    )

    add_body(
        "• Fragmentos Base de Referencia Docente:\n"
        "   - Código conceptual de partida basado en los ejemplos de aula 'AppCamara.java' y 'AppLaberinto.java' para el VAO/VBO unitario "
        "del cubo canónico y el ciclo de eventos GLFW. Todas las ampliaciones de ciudad 11x11, física de conducción, iluminación, "
        "tráfico autónomo, misiones, cuadro de instrumentos y menús han sido desarrolladas integralmente para esta práctica."
    )

    # Guardar documento
    output_path = os.path.abspath("Documento_Breve_CiudadConduccion.docx")
    doc.save(output_path)
    print(f"Documento generado exitosamente en: {output_path}")

if __name__ == "__main__":
    create_document()
