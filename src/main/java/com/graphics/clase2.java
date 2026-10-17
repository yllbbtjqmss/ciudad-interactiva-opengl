package com.graphics; // Agrupa las cuatro lecciones dentro del mismo paquete.

import static org.lwjgl.glfw.GLFW.*; // Permite consultar teclas y cambiar el título de la ventana.

/**
 * CLASE 2: CREACIÓN Y MOVIMIENTO DEL AUTO.
 * Hereda la ciudad de clase1 y agrega únicamente lo necesario para conducir.
 * Orden de lectura: variables, teclado, movimiento, colisiones, cámara y dibujo.
 */
public class clase2 extends clase1 {

    // ==================== 1. VARIABLES DEL AUTO ====================
    protected float autoX = -30; // Posición horizontal inicial: centro de una calle.
    protected float autoZ = 30; // Posición inicial sobre el eje que recorre el fondo de la ciudad.
    protected float angulo = 0; // Orientación en radianes; cero apunta hacia -Z.
    protected float velocidad = 0; // Unidades por segundo; un valor negativo significa reversa.
    protected boolean camaraAerea = false; // false: seguir el auto; true: observar toda la ciudad.
    protected static final float RADIO_AUTO = 1.65f; // Radio que contiene al vehículo para las colisiones.

    //CHATA 
    protected static final float RADIO_CHATA = 0.95f; // tama;o usado para detectar choques
    protected static final float DISTANCIA_CHATA = 3.1f; // Distancia del centro de la chata al centro del auto (en sentido opuesto al frente).
    protected static final float CHATA_SEPARACION = 0.85f; // Separación entre los dos círculos(zonas) de colisión de la chata.

    // ==================== 2. TECLADO Y REINICIO ====================

    /** Atiende acciones que deben ocurrir una sola vez por pulsación. */
    @Override // Sustituye el método de clase1 y conserva sus acciones mediante super.
    protected void tecla(int key) {
        super.tecla(key); // Ejecuta el control ESC definido en clase1.

        if (key == GLFW_KEY_C) { // Comprueba si se presionó la tecla de cámara.
            camaraAerea = !camaraAerea; // Invierte el modo de cámara actual.
        }

        if (key == GLFW_KEY_R) { // Comprueba si el usuario quiere comenzar de nuevo.
            reiniciar(); // Restaura las variables de conducción.
        }
    }

    /** Coloca nuevamente el auto en su punto de partida. */
    protected void reiniciar() {
        autoX = -30; // Recupera la coordenada X de inicio.
        autoZ = 30; // Recupera la coordenada Z de inicio.
        angulo = 0; // Orienta el frente hacia -Z.
        velocidad = 0; // Detiene cualquier movimiento previo.
    }

    // ==================== 3. MOVIMIENTO POR CUADRO ====================

    /** Actualiza la conducción; deltaTime contiene los segundos transcurridos entre cuadros. */
    @Override // Cambia la cámara orbital de clase1 por el control del vehículo.
    protected void actualizar(float deltaTime) {
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

        float factorFrenado = (float) Math.exp(-resistencia * deltaTime); // Calcula la fracción de velocidad conservada.
        velocidad *= factorFrenado; // Aplica resistencia de forma proporcional al tiempo transcurrido.
        velocidad = Math.max(-6, Math.min(16, velocidad)); // Limita la reversa a -6 y el avance a 16.
        float direccion = 0; // Sin dirección presionada el volante permanece recto.

        if (pulsada(GLFW_KEY_A) || pulsada(GLFW_KEY_LEFT)) { // Comprueba el giro hacia la izquierda.
            direccion += 1; // Selecciona el sentido positivo de rotación.
        }

        if (pulsada(GLFW_KEY_D) || pulsada(GLFW_KEY_RIGHT)) { // Comprueba el giro hacia la derecha.
            direccion -= 1; // Selecciona el sentido negativo de rotación.
        }

        // Calcula el ángulo que tendría el auto después de girar en este cuadro.
        // Se guarda aparte para no aceptar el giro hasta confirmar que el auto y la chata caben.
        float anguloPropuesto = angulo + direccion * velocidad * 0.11f * deltaTime; // Gira según la velocidad; en reversa invierte el giro.

        float frenteX = -(float) Math.sin(anguloPropuesto); // Obtiene la componente X del frente propuesto.
        float frenteZ = -(float) Math.cos(anguloPropuesto); // Obtiene la componente Z; con ángulo cero vale -1.


        // *********LIMITES DE CIRCULACION*************
        // Antes de mover el auto, se calcula la posición que tendría en el siguiente instante.
        float siguienteX = autoX + frenteX * velocidad * deltaTime; // Propone la nueva posición X.
        float siguienteZ = autoZ + frenteZ * velocidad * deltaTime; // Propone la nueva posición Z.




        //***************************************

        // CALCULA LA POSICIÓN FUTURA DE LA CHATA DETRÁS DEL AUTOMÓVIL.
        float chataSiguienteX = siguienteX + (float) Math.sin(anguloPropuesto) * DISTANCIA_CHATA; // X futura de la chata.
        float chataSiguienteZ = siguienteZ + (float) Math.cos(anguloPropuesto) * DISTANCIA_CHATA; // Z futura de la chata.

        // COMPRUEBA SI LA CHATA PUEDE OCUPAR LA POSICIÓN PROPUESTA.
        // SOLO MUEVE EL VEHÍCULO SI EL AUTO Y LA CHATA ESTÁN LIBRES DE COLISIÓN.

        if (puedeCircular(siguienteX, siguienteZ) // Comprueba la posición propuesta del auto.
                && chataPuedeCircular(chataSiguienteX, chataSiguienteZ, anguloPropuesto)) { // Comprueba la chata futura.
            autoX = siguienteX; // Acepta el desplazamiento horizontal del auto.
            autoZ = siguienteZ; // Acepta el desplazamiento en profundidad del auto.
            angulo = anguloPropuesto; // Acepta también el giro porque la chata cabe en su nueva orientación.
        } else { // El auto o la chata invadirían una manzana o saldrían del mapa.
            velocidad = 0; // Detiene el auto: el conjunto completo (auto + chata) no avanza.
        }

        actualizarTitulo(); // Muestra los controles y la velocidad actual.
    }

    /** Compone el texto de la ventana sin mezclarlo con las fórmulas de conducción. */
    private void actualizarTitulo() {
        int kilometrosPorHora = Math.round(Math.abs(velocidad) * 3.6f); // Convierte m/s a km/h y redondea.
        String titulo = getClass().getSimpleName(); // Obtiene el nombre de la etapa que se está ejecutando.
        titulo += " | WASD/flechas: conducir | Espacio: freno"; // Añade los controles del vehículo.
        titulo += " | C: camara | R: reiniciar | "; // Añade los controles de cámara y reinicio.
        titulo += kilometrosPorHora + " km/h"; // Añade la magnitud de la velocidad.
        titulo += estadoExtra(); // Permite a clase3 y clase4 añadir luces y entregas.
        glfwSetWindowTitle(ventana, titulo); // Publica el texto en la barra superior de la ventana.
    }

    /** Deja un espacio para los indicadores de las próximas lecciones. */
    protected String estadoExtra() {
        return ""; // En clase2 todavía no hay información adicional.
    }

    // ==================== 4. COLISIONES CON LA CIUDAD ====================

    /** Comprueba si el círculo del auto cabe en una posición sin tocar manzanas ni bordes. */


    protected boolean puedeCircular(float x, float z) {
        return puedeCircularConRadio(x, z, RADIO_AUTO); // Comprueba el auto reutilizando el método general.
    }

    /** Comprueba si un círculo de radio dado cabe en una posición sin tocar manzanas ni bordes. */





    //REVISA COLISIÓN GENERAL SEGÚN EL RADIO

    protected boolean puedeCircularConRadio(float x, float z, float radio) {
        float limitePermitido = LIMITE - radio; // Reserva espacio para que todo el círculo quede dentro.


       // Evita que el cuerpo salga del mapa.
       
       // Se resta el radio porque no basta con que el centro
       // esté dentro del límite; también debe quedar dentro todo el cuerpo.

        if (Math.abs(x) > limitePermitido || Math.abs(z) > limitePermitido) { // Detecta salida por cualquier borde.
            return false; // Rechaza la posición exterior.
        }

        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre las filas del mapa.
            for (int columna = 0; columna < MAPA[fila].length; columna++) { // Recorre las celdas de cada fila.
                if (MAPA[fila][columna] == 0) { 
                    continue; // Omite la calle porque no es un obstáculo.
                }

                float centroX = centro(columna); // Convierte la columna al centro X de la manzana.
                float centroZ = centro(fila); // Convierte la fila al centro Z de la manzana.
                float cercaX = Math.max(centroX - 5, Math.min(x, centroX + 5)); // Busca el X más cercano dentro de la acera.
                float cercaZ = Math.max(centroZ - 5, Math.min(z, centroZ + 5)); // Busca el Z más cercano dentro de la acera.
                float distanciaX = x - cercaX; // Calcula la separación horizontal del círculo al rectángulo.
                float distanciaZ = z - cercaZ; // Calcula la separación en profundidad al rectángulo.
                float distanciaCuadrada = distanciaX * distanciaX + distanciaZ * distanciaZ; // Aplica Pitágoras sin raíz.

                if (distanciaCuadrada < radio * radio) { // Comprueba si el círculo invade la manzana.
                    return false; // Rechaza el movimiento que produciría una colisión.
                }
            }
        }

        return true; // Acepta la posición porque no se encontró ningún obstáculo.
    }


     // COMPRUEBA SI LA CHATA PUEDE OCUPAR LA POSICIÓN PROPUESTA.
     // REVISA LOS DOS PUNTOS DE COLISIÓN DE LA CHATA


    protected boolean chataPuedeCircular(float chataX, float chataZ, float anguloFuturo) {
        float[] circulos = calcularCirculosChata(chataX, chataZ, anguloFuturo); // Reutiliza los mismos centros para ciudad y tráfico.
        return puedeCircularConRadio(circulos[0], circulos[1], RADIO_CHATA)
                && puedeCircularConRadio(circulos[2], circulos[3], RADIO_CHATA); // Ambos círculos deben quedar libres.
    }

    /** Devuelve {traseroX, traseroZ, delanteroX, delanteroZ} desde el centro de la chata. */
    protected float[] calcularCirculosChata(float chataX, float chataZ, float anguloFuturo) {
        float seno = (float) Math.sin(anguloFuturo); // Seno de la orientación futura del conjunto.
        float coseno = (float) Math.cos(anguloFuturo); // Coseno de la orientación futura del conjunto.

        // Círculo trasero de la chata: el más alejado del auto (local +Z).
        float traseroX = chataX + seno * CHATA_SEPARACION; // Centro X de la mitad trasera.
        float traseroZ = chataZ + coseno * CHATA_SEPARACION; // Centro Z de la mitad trasera.

        // Círculo delantero de la chata: el más cercano al auto (local -Z).
        float delanteroX = chataX - seno * CHATA_SEPARACION; // Centro X de la mitad delantera.
        float delanteroZ = chataZ - coseno * CHATA_SEPARACION; // Centro Z de la mitad delantera.
        return new float[]{traseroX, traseroZ, delanteroX, delanteroZ}; // Comparte los centros sin repetir sus fórmulas.
    }

    // ==================== 5. CÁMARA ====================


    // la logica es: si está activada la cámara aérea, usa la cámara que ya programé en clase1.
    // y si no está activa la cámara aérea, usa la cámara que sigue al auto.


    @Override // Personaliza la cámara creada en clase1.
    protected void configurarCamara() {
        if (camaraAerea) { // Comprueba si está activa la vista general.
            super.configurarCamara(); // Reutiliza la cámara oblicua de la primera lección.
            return; // Evita reemplazarla con la cámara de seguimiento.
        }

        //POSICION CAMARA QUE SIGUE AUTO

        float camaraX = autoX + (float) Math.sin(angulo) * 12; // Coloca/ALEJA la cámara 12 unidades detrás en X.
        float camaraZ = autoZ + (float) Math.cos(angulo) * 12; // Coloca/ALEJA la cámara 12 unidades detrás en Z.
        vector("uOjo", camaraX, 9, camaraZ); // Envía la posición de la cámara, a 9 unidades de ALTURA.



        vector("uObjetivo", autoX, 0.8f, autoZ); // HACE QUE LA CAMARA MIRE AL AUTO
        decimal("uAspecto", (float) ancho / alto); // Mantiene las proporciones al redimensionar la ventana.
    }

    // ==================== 6. DIBUJO DEL AUTO ====================

    /** Dibuja primero la ciudad existente y luego el vehículo. */
    @Override // Amplía el dibujo de clase1 sin copiar su código.
    protected void escena() {
        super.escena(); // Dibuja asfalto, calles, edificios y parques.
        dibujarAuto(); // Añade el modelo del vehículo sobre la ciudad.
    }

    /** Construye el auto con cajas; argumentos: posición XYZ, tamaño XYZ y color RGB. */
    protected void dibujarAuto() {
        pieza(0, 0.65f, 0, 1.65f, 0.55f, 2.6f, 0.78f, 0.25f, 0.18f); // Rojo terracota distintivo del jugador; mismas dimensiones.
        pieza(0, 1.12f, 0.12f, 1.3f, 0.55f, 1.25f, 0.15f, 0.24f, 0.30f); // Cabina acristalada oscura.
        float[] ladosRuedas = {-0.88f, 0.88f}; // Ubica ruedas a izquierda y derecha del auto.
        float[] ejesRuedas = {-0.82f, 0.82f}; // Ubica las ruedas delanteras y traseras.

        for (float x : ladosRuedas) { // Selecciona uno de los dos lados del vehículo.
            for (float z : ejesRuedas) { // Selecciona el eje delantero o trasero.
                pieza(x, 0.38f, z, 0.24f, 0.58f, 0.6f, 0.055f, 0.065f, 0.08f); // Dibuja una rueda oscura.
                if (!vistaMapa) {
                    pieza(x * 1.10f, 0.38f, z, 0.065f, 0.29f, 0.31f, 0.57f, 0.62f, 0.65f); // Llanta visible sobre el neumático original.
                }
            }
        }

        if (!vistaMapa) {
            dibujarDetallesAuto();
            dibujarLucesAuto(); // Solo la superficie del foco cambia con F; el haz permanece en clase3.
        }

        dibujarChata(); // Añade la chata/remolque detrás del automóvil del jugador.
    }

    /** Acabados de carrocería dentro de la silueta original, sin alterar la hitbox. */
    private void dibujarDetallesAuto() {
        pieza(0, 1.41f, 0.12f, 1.32f, 0.08f, 1.28f, 0.84f, 0.31f, 0.23f); // Techo pintado.
        pieza(0, 1.12f, -0.52f, 1.1f, 0.38f, 0.025f, 0.23f, 0.37f, 0.43f); // Parabrisas.
        pieza(0, 1.12f, 0.76f, 1.08f, 0.35f, 0.025f, 0.12f, 0.20f, 0.25f); // Vidrio trasero.
        pieza(0, 0.46f, -1.31f, 1.55f, 0.16f, 0.12f, 0.19f, 0.24f, 0.27f); // Parachoques delantero.
        pieza(0, 0.46f, 1.31f, 1.55f, 0.16f, 0.12f, 0.19f, 0.24f, 0.27f);
        pieza(0, 0.64f, -1.315f, 0.52f, 0.20f, 0.06f, 0.08f, 0.12f, 0.15f); // Parrilla entre los focos.
        pieza(0, 0.66f, 1.335f, 0.38f, 0.15f, 0.035f, 0.79f, 0.81f, 0.76f); // Matrícula.
        for (int lado = -1; lado <= 1; lado += 2) {
            pieza(lado * 0.661f, 1.13f, 0.12f, 0.025f, 0.36f, 1.01f, 0.19f, 0.31f, 0.37f); // Cristal lateral.
            pieza(lado * 0.68f, 1.12f, 0.12f, 0.035f, 0.42f, 0.07f, 0.23f, 0.28f, 0.31f); // Pilar lateral.
            pieza(lado * 0.83f, 0.79f, 0.25f, 0.035f, 0.055f, 0.23f, 0.70f, 0.73f, 0.72f); // Manija.
        }
        pieza(-0.28f, 0.938f, -0.93f, 0.12f, 0.025f, 0.60f, 0.88f, 0.81f, 0.62f); // Franja del jugador sobre el capó.
    }

    /** La etapa de iluminación consulta F sin cambiar sus controles ni el cálculo del haz. */
    protected boolean farosVisiblesEncendidos() {
        return false; // Las primeras etapas muestran focos físicos sin iluminación funcional.
    }

    /** Lentes físicos y emisión exclusivamente visual, restaurada al terminar. */
    private void dibujarLucesAuto() {
        for (float x : new float[]{-0.55f, 0.55f}) {
            pieza(x, 0.68f, -1.32f, 0.43f, 0.25f, 0.07f, 0.21f, 0.25f, 0.28f); // Marco del foco.
            entero("uEmision", farosVisiblesEncendidos() ? 1 : 0);
            pieza(x, 0.68f, -1.365f, 0.34f, 0.15f, 0.025f, 0.94f, 0.90f, 0.74f);
            entero("uEmision", 1);
            pieza(x, 0.68f, 1.32f, 0.35f, 0.17f, 0.07f, 0.72f, 0.13f, 0.10f); // Luz trasera tenue, sin nueva luz real.
            entero("uEmision", 0);
        }
    }








    //DIBUJA LA CHATA (REMOLQUE PEQUEÑO) DETRÁS DEL AUTOMÓVIL DEL JUGADOR.
    // Reutiliza pieza() con coordenadas locales: el eje +Z del auto apunta hacia atrás.
    



    protected void dibujarChata() {
        // Plataforma rectangular baja del remolque.
        pieza(0, 0.60f, DISTANCIA_CHATA, 1.70f, 0.30f, 2.20f, 0.34f, 0.40f, 0.43f); // Acero azulado; misma plataforma.

        float[] ladosChata = {-0.78f, 0.78f}; // Define los dos laterales de la chata.
        for (float x : ladosChata) { // Repite el dibujo para cada lateral.
            pieza(x, 0.85f, DISTANCIA_CHATA, 0.14f, 0.35f, 2.10f, 0.24f, 0.26f, 0.30f); // Borde lateral de la chata.
        }

        pieza(0, 0.88f, DISTANCIA_CHATA + 1.05f, 1.60f, 0.40f, 0.14f, 0.24f, 0.26f, 0.30f); // Compuerta trasera del remolque.

        float[] ejesChata = {DISTANCIA_CHATA - 0.65f, DISTANCIA_CHATA + 0.65f}; // Ubica las ruedas delanteras y traseras de la chata.
        for (float x : ladosChata) { // Selecciona uno de los dos lados de la chata.
            for (float z : ejesChata) { // Selecciona el eje delantero o trasero de la chata.
                pieza(x * 1.04f, 0.40f, z, 0.24f, 0.60f, 0.62f, 0.055f, 0.065f, 0.08f); // Dibuja una rueda de la chata.
            }
        }

        // UNIÓN ENTRE AUTO Y CHATA: barra de tiro más una rótula de enganche.

        pieza(0, 0.55f, 1.70f, 0.14f, 0.14f, 1.10f, 0.25f, 0.25f, 0.28f); // Barra que conecta el auto con la chata.
        pieza(0, 0.55f, 2.20f, 0.28f, 0.28f, 0.28f, 0.20f, 0.20f, 0.22f); // Rótula de enganche sobre la chata.
        if (!vistaMapa) { // Detalles decorativos: no afectan posición, enganche ni círculos de colisión.
            pieza(0, 0.76f, DISTANCIA_CHATA, 1.38f, 0.025f, 1.90f, 0.43f, 0.39f, 0.31f); // Superficie interior.
            for (float lado : ladosChata) {
                pieza(lado, 1.045f, DISTANCIA_CHATA, 0.17f, 0.07f, 2.10f, 0.62f, 0.68f, 0.69f); // Perfil superior metálico.
                pieza(lado * 1.04f, 0.74f, DISTANCIA_CHATA, 0.27f, 0.09f, 1.90f, 0.24f, 0.30f, 0.34f); // Guardabarros simple.
                entero("uEmision", 1);
                pieza(lado * 0.8f, 0.86f, DISTANCIA_CHATA + 1.13f, 0.24f, 0.13f, 0.04f, 0.72f, 0.13f, 0.10f);
                pieza(lado * 1.12f, 0.87f, DISTANCIA_CHATA - 0.7f, 0.025f, 0.10f, 0.20f, 0.88f, 0.57f, 0.21f); // Reflector lateral.
                entero("uEmision", 0);
            }
        }
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
