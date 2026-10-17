package com.graphics; // Reúne la versión final con las otras tres lecciones.

import static org.lwjgl.glfw.GLFW.*; // Permite consultar la tecla que controla el minimapa.
import static org.lwjgl.opengl.GL33.*; // Permite cambiar viewport, recorte y buffers de dibujo.

/**
 * CLASE 4: CIUDAD TERMINADA, ENTREGAS Y MINIMAPA.
 * Orden de lectura: estado, controles, entregas, decoración y segundo pase de dibujo.
 * Hereda clase3, que hereda clase2, que a su vez hereda clase1.
 * Los semáforos son decorativos; el ejemplo no incluye tráfico autónomo.
 */
public class clase4 extends clase3 {

    // ==================== 1. ESTADO DEL JUEGO ====================
    private boolean mostrarMapa = true; // Muestra el minimapa desde el inicio.
    private int entregas = 0; // Cuenta las entregas completadas; también identifica el siguiente destino.
    private float tiempo = 0; // Acumula los segundos de la partida hasta completar el recorrido.
    
    
    //Garantizar el acceso por calles a todos los destinos de entrega:
    //Esas coordenadas están sobre calles del mapa,
    //Por eso los tres destinos son accesibles conduciendo.
    
    private static final float[][] DESTINOS = { // Cada fila contiene X y Z de una parada sobre la calle de la ciudad ampliada.
        {50, -50}, // Primera entrega: sector noreste de la ciudad ampliada.
        {-50, -50}, // Segunda entrega: sector noroeste de la ciudad ampliada.
        {50, 50} // Tercera entrega: sector sureste para recorrer también la zona nueva del mapa.
    };

    // ==================== 1.1 TRÁFICO AUTÓNOMO (MEJORA OPCIONAL) ====================
    private static final float RADIO_AUTONOMO = 1.45f; // Cubre la carrocería de 1,55 por 2,4 unidades sin un margen excesivo.
    private static final float[][][] RUTAS = { // Cada ruta es una lista de waypoints {X, Z} situados sobre calles.
        { {centro(0), centro(0)}, {centro(10), centro(0)}, {centro(10), centro(10)}, {centro(0), centro(10)} }, // Anillo exterior de la ciudad.
        { {centro(8), centro(2)}, {centro(8), centro(8)}, {centro(2), centro(8)}, {centro(2), centro(2)} }, // Anillo intermedio de la ciudad.
        { {centro(4), centro(4)}, {centro(6), centro(4)}, {centro(6), centro(6)}, {centro(4), centro(6)} } // Anillo interior de la ciudad.
    };

    /** Guarda el estado mínimo de un vehículo que circula solo por las calles. */
    private static class VehiculoAutonomo {
        float x; // Posición horizontal dentro de la ciudad.
        float z; // Posición en profundidad dentro de la ciudad.
        float angulo; // Orientación en radianes, coherente con el auto principal.
        float velocidad; // Unidades que avanza por segundo hacia su waypoint.
        int indiceObjetivo; // Waypoint de la ruta al que apunta en este momento.
        int ruta; // Índice de la ruta que recorre este vehículo.
        float rojo; // Componente roja de su color identificativo.
        float verde; // Componente verde de su color identificativo.
        float azul; // Componente azul de su color identificativo.
    }

    private final VehiculoAutonomo[] vehiculos = { // Tres vehículos con rutas y colores distintos.
        crearVehiculo(0, 0.95f, 0.80f, 0.15f), // Vehículo amarillo sobre el anillo exterior.
        crearVehiculo(1, 0.25f, 0.75f, 0.40f), // Vehículo verde sobre el anillo intermedio.
        crearVehiculo(2, 0.80f, 0.45f, 0.90f) // Vehículo violeta sobre el anillo interior.
    };

    // ==================== 2. CONTROLES Y REINICIO ====================

    /** Añade el interruptor del minimapa a los controles anteriores. */
    @Override // Amplía las teclas definidas por clase3.
    protected void tecla(int key) {
        super.tecla(key); // Conserva salida, cámara, reinicio y luces.
        if (key == GLFW_KEY_M) { // Comprueba si se pulsó la tecla del mapa.
            mostrarMapa = !mostrarMapa; // Alterna entre mostrar y ocultar la vista superior.
        }
    }

    /** Reinicia el vehículo y el progreso de las entregas. */
    @Override // Amplía el reinicio definido en clase2.
    protected void reiniciar() {
        super.reiniciar(); // Restaura posición, velocidad y orientación del vehículo.
        entregas = 0; // Vuelve a seleccionar la primera parada.
        tiempo = 0; // Reinicia el cronómetro de la partida.
        reiniciarTrafico(); // Devuelve los vehículos autónomos a sus posiciones iniciales.
        glfwSetInputMode(ventana, GLFW_CURSOR, modoCursorAnterior); // Restaura el cursor que había antes del panel.
        panelVictoriaVisible = false; // Vuelve a preparar el cursor en la próxima victoria.
        mouseSobreJugar = false; // Limpia el resaltado del botón JUGAR.
        mouseSobreSalir = false; // Limpia el resaltado del botón SALIR.
    }

    // ==================== 3. REGLAS DE LAS ENTREGAS ====================

    /** Actualiza el vehículo y comprueba si llegó y frenó en el destino activo. */
    @Override // Añade el objetivo del juego al movimiento heredado.
    protected void actualizar(float deltaTime) {
        if (juegoTerminado()) { // Congela la conducción desde el cuadro siguiente a la última entrega.
            if (!panelVictoriaVisible) { // Detecta la primera vez que aparece el panel.
                modoCursorAnterior = glfwGetInputMode(ventana, GLFW_CURSOR); // Guarda el modo de cursor previo.
                glfwSetInputMode(ventana, GLFW_CURSOR, GLFW_CURSOR_NORMAL); // Muestra el cursor para pulsar los botones.
                panelVictoriaVisible = true; // Evita repetir la preparación del cursor en cada cuadro.
            }
            actualizarPanelVictoria(); // Procesa el hover y el clic de los botones del panel.
            float posicionX = autoX; // Guarda la posición para impedir que el auto se mueva.
            float posicionZ = autoZ; // Guarda la profundidad antes de refrescar el título.
            float orientacion = angulo; // Guarda la orientación original del vehículo.
            super.actualizar(deltaTime); // Refresca el título con el mensaje de victoria.
            autoX = posicionX; // Restaura la posición congelada.
            autoZ = posicionZ; // Restaura la profundidad congelada.
            angulo = orientacion; // Restaura la orientación congelada.
            velocidad = 0; // Detiene por completo el vehículo.
            return; // Evita mover el tráfico y avanzar el cronómetro mientras se muestra el panel.
        }
        float anteriorX = autoX; // Guarda la última posición antes de conducir.
        float anteriorZ = autoZ; // Guarda la profundidad anterior.
        float anguloAnterior = angulo; // Guarda también la orientación del conjunto.
        super.actualizar(deltaTime); // Procesa aceleración, giro, colisiones y título de la ventana.
        // SI EL JUGADOR CHOCA CON UN AUTÓNOMO, RESTAURA SU ÚLTIMA POSICIÓN VÁLIDA.
        if (jugadorColisionaConTrafico(autoX, autoZ, angulo)) {
            autoX = anteriorX;
            autoZ = anteriorZ;
            angulo = anguloAnterior;
            velocidad = 0; // Rechaza el movimiento completo, incluido el giro de la chata.
        }
        actualizarTrafico(deltaTime); // Mueve el tráfico antes de cualquier salida por entregas completadas.
        tiempo += deltaTime; // Suma los segundos de este cuadro al cronómetro.
        float distanciaX = autoX - DESTINOS[entregas][0]; // Calcula la separación horizontal al destino activo.
        float distanciaZ = autoZ - DESTINOS[entregas][1]; // Calcula la separación en profundidad al destino.
        float distanciaCuadrada = distanciaX * distanciaX + distanciaZ * distanciaZ; // Mide cercanía sin calcular raíz cuadrada.
        boolean estaCerca = distanciaCuadrada < 3 * 3; // Acepta un radio de llegada de tres unidades.
        boolean estaFrenando = Math.abs(velocidad) < 1; // Exige circular a menos de una unidad por segundo.
        if (estaCerca && estaFrenando) { // Solo completa la entrega si ambas condiciones se cumplen.
            entregas++; // Selecciona la siguiente parada o completa el juego.
        }
    }

    /** Compone el progreso que se añade al título de la ventana. */
    @Override // Amplía los indicadores de día/noche y faros de clase3.
    protected String estadoExtra() {
        String mensaje = super.estadoExtra(); // Recupera los indicadores de iluminación.
        mensaje += " | M: mapa | "; // Muestra la tecla que alterna el minimapa.
        if (entregas == DESTINOS.length) { // Selecciona el texto de victoria al completar las tres paradas.
            mensaje += "GANASTE en " + (int) tiempo + " s! R: jugar otra vez"; // Muestra tiempo final y opción de reinicio.
        } else { // Durante el recorrido muestra progreso e instrucciones.
            mensaje += "Entregas " + entregas + "/3"; // Indica cuántas paradas se completaron.
            mensaje += " | Frena en la marca dorada | " + (int) tiempo + " s"; // Explica la condición de entrega y el tiempo.
        }
        return mensaje; // Entrega el texto a actualizarTitulo() de clase2.
    }

    // ==================== 4. ESCENA FINAL Y DESTINO ====================

    /** Añade decoración y señal del destino a la escena iluminada. */
    @Override // Amplía el dibujo acumulado de las tres etapas anteriores.
    protected void escena() {
        super.escena(); // Dibuja la ciudad, el auto y las farolas.
        if (!vistaMapa) { // Los detalles pequeños solo son necesarios en la vista principal.
            decorarCiudad(); // Añade árboles, bancos, ventanas y señalización urbana.
            dibujarSenalesTransito(); // Añade señales de tránsito decorativas junto a las calles.
        }
        dibujarTrafico(); // Dibuja los vehículos autónomos, visibles también en el minimapa.
        if (entregas < DESTINOS.length) { // Dibuja un objetivo únicamente mientras queden entregas.
            dibujarDestino(); // Coloca la marca dorada en la parada activa.
        }
    }

    /** Marca la próxima parada con una plataforma y una baliza flotante. */
    private void dibujarDestino() {
        float x = DESTINOS[entregas][0]; // Lee el X de la próxima entrega.
        float z = DESTINOS[entregas][1]; // Lee el Z de la próxima entrega.
        entero("uEmision", 1); // Hace que el objetivo sea visible incluso de noche.
        for (int lado = -1; lado <= 1; lado += 2) { // Marco de entrega: permite seguir viendo el asfalto.
            caja(x + lado * 2.35f, 0.07f, z, 0.12f, 0.04f, 4.8f, 0.96f, 0.73f, 0.31f);
            caja(x, 0.07f, z + lado * 2.35f, 4.6f, 0.04f, 0.12f, 0.96f, 0.73f, 0.31f);
        }
        if (!vistaMapa) { // Evita añadir una baliza tridimensional al mapa pequeño.
            float alturaBaliza = 3.5f + (float) Math.sin(tiempo * 2) * 0.3f; // Hace oscilar la baliza suavemente.
            dibujarMarcadorDestino(x, alturaBaliza, z); // Flecha geométrica hacia abajo, sin cambiar el área de entrega.
        } else {
            cajaGirada(x, 24, z, 2.5f, 0.10f, 2.5f, 0.96f, 0.73f, 0.31f, 0.7854f); // Símbolo legible a escala de minimapa.
        }
        entero("uEmision", 0); // Devuelve a los siguientes objetos su iluminación normal.
    }

    /** Flecha low-poly de cuatro piezas; su altura solo tiene efecto visual. */
    private void dibujarMarcadorDestino(float x, float y, float z) {
        caja(x, y + 0.78f, z, 0.24f, 0.72f, 0.24f, 0.97f, 0.87f, 0.60f);
        caja(x, y + 0.35f, z, 1.20f, 0.22f, 0.38f, 0.96f, 0.73f, 0.31f);
        caja(x, y + 0.12f, z, 0.78f, 0.24f, 0.34f, 0.96f, 0.73f, 0.31f);
        caja(x, y - 0.10f, z, 0.34f, 0.22f, 0.30f, 0.97f, 0.87f, 0.60f);
    }

    // ==================== 5. DECORACIÓN DE LAS MANZANAS ====================
    // ******** DECORACIÓN URBANA: ÁRBOLES, BANCOS, VENTANAS Y PASOS PEATONALES ********

    /** Decide qué decoración corresponde a cada tipo de parcela. */

    // Según el tipo de celda se dibujan parques, árboles, bancos, ventanas y pasos peatonales.

    private void decorarCiudad() {
        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre las filas del mapa.
            for (int columna = 0; columna < MAPA[fila].length; columna++) { // Recorre las columnas de esa fila.
                float x = centro(columna); // Obtiene el centro horizontal de la parcela.
                float z = centro(fila); // Obtiene el centro de la parcela en profundidad.
                int tipo = MAPA[fila][columna]; // Lee el contenido de la celda.
                if (tipo == 2) { // Detecta una parcela de parque.
                    dibujarParque(x, z); // Añade árboles y un banco.
                }
                if (tipo == 1) { // Detecta una parcela con edificio.
                    float altura = 5 + (fila * 3 + columna * 7) % 9; // Recupera la misma altura calculada en clase1.
                    dibujarVentanas(x, z, altura); // Coloca ventanas en sus cuatro fachadas.
                }
                if (tipo != 0) { // La señalización se coloca junto a las manzanas, no en celdas de calle.
                    dibujarPasoPeatonal(x, z); // Añade el cruce pintado sobre la calle contigua.
                    dibujarSemaforo(x + 4, z - 4); // Coloca el semáforo dentro de la acera.
                }
            }
        }
    }

    /** Construye cuatro árboles y un banco utilizando cajas. */
    private void dibujarParque(float x, float z) {
        caja(x, 0.395f, z, 8.5f, 0.03f, 1.05f, 0.66f, 0.64f, 0.55f); // Sendero sobre el césped, sin z-fighting.
        caja(x, 0.397f, z, 1.05f, 0.03f, 8.5f, 0.66f, 0.64f, 0.55f);
        float[] posiciones = {-2.5f, 2.5f}; // Define desplazamientos respecto al centro de la parcela.
        for (float desplazamientoX : posiciones) { // Selecciona el lado izquierdo o derecho del parque.
            for (float desplazamientoZ : posiciones) { // Selecciona el lado delantero o trasero.
                float arbolX = x + desplazamientoX; // Convierte el desplazamiento local en coordenada X mundial.
                float arbolZ = z + desplazamientoZ; // Convierte el desplazamiento local en coordenada Z mundial.
                int variante = Math.floorMod((int) (arbolX * 3 + arbolZ), 3); // Variación fija, nunca aleatoria por cuadro.
                dibujarArbol(arbolX, arbolZ, variante);
            }
        }
        caja(x, 0.75f, z + 1.2f, 2.5f, 0.18f, 0.70f, 0.52f, 0.37f, 0.23f); // Banco junto al sendero.
        caja(x, 1.12f, z + 1.48f, 2.5f, 0.58f, 0.12f, 0.57f, 0.41f, 0.26f);
        for (int lado = -1; lado <= 1; lado += 2) {
            caja(x + lado * 0.88f, 0.56f, z + 1.2f, 0.12f, 0.38f, 0.60f, 0.22f, 0.28f, 0.28f);
            caja(x + lado * 3.65f, 0.66f, z, 0.75f, 0.55f, 0.75f, 0.24f, 0.38f, 0.27f); // Dos arbustos bajos.
        }
        if (x == centro(5) && z == centro(1)) { // Una única fuente decorativa, sin agua animada ni nueva mecánica.
            caja(x, 0.57f, z - 2.2f, 1.8f, 0.35f, 1.8f, 0.55f, 0.61f, 0.62f);
            caja(x, 0.76f, z - 2.2f, 1.45f, 0.035f, 1.45f, 0.24f, 0.47f, 0.53f);
            caja(x, 0.94f, z - 2.2f, 0.32f, 0.38f, 0.32f, 0.67f, 0.71f, 0.69f);
        }
    }

    /** Copa escalonada de tres volúmenes, conservando el estilo geométrico del proyecto. */
    private void dibujarArbol(float x, float z, int variante) {
        float extra = variante * 0.15f;
        caja(x, 1.25f, z, 0.28f, 1.85f, 0.28f, 0.37f, 0.28f, 0.19f);
        caja(x, 2.1f + extra, z, 2.05f, 1.15f, 2.05f, 0.18f, 0.36f + extra * 0.12f, 0.25f);
        cajaGirada(x, 2.9f + extra, z, 1.65f, 0.9f, 1.65f, 0.24f, 0.43f, 0.29f, 0.35f);
        caja(x, 3.48f + extra, z, 1.05f, 0.55f, 1.05f, 0.31f, 0.49f, 0.33f);
    }

    /** Distribuye ventanas por pisos en las cuatro paredes del edificio. */
    private void dibujarVentanas(float x, float z, float altura) {
        int fila = Math.round((z + LIMITE) / CELDA - 0.5f);
        int columna = Math.round((x + LIMITE) / CELDA - 0.5f);
        int piso = 0;
        for (float y = 1.7f; y < altura; y += 2, piso++) {
            for (int ventana = 0; ventana < 3; ventana++) {
                float desplazamiento = (ventana - 1) * 2;
                for (int cara = 0; cara < 4; cara++) {
                    int patron = (fila * 7 + columna * 3 + piso * 5 + ventana * 2 + cara) % 7;
                    boolean encendida = noche && patron < 3; // Solo parte de las ventanas se enciende; patrón estable.
                    float variacion = patron * 0.012f;
                    float rojo = encendida ? 0.88f + variacion : 0.15f + variacion;
                    float verde = encendida ? 0.69f + variacion : 0.26f + variacion;
                    float azul = encendida ? 0.39f + variacion : 0.33f + variacion;
                    entero("uEmision", encendida ? 1 : 0);
                    if (cara < 2) {
                        if (!(cara == 0 && ventana == 1 && piso == 0)) { // Deja libre la puerta frontal.
                            caja(x + desplazamiento, y, z + (cara == 0 ? -3.53f : 3.53f), 0.9f, 1.05f, 0.04f, rojo, verde, azul);
                        }
                    } else {
                        caja(x + (cara == 2 ? -3.53f : 3.53f), y, z + desplazamiento, 0.04f, 1.05f, 0.9f, rojo, verde, azul);
                    }
                }
            }
        }
        entero("uEmision", 0); // No deja la emisión activa para el resto de la ciudad.
    }

    /** Dibuja las franjas blancas sobre la calle contigua al norte de la manzana. */
    private void dibujarPasoPeatonal(float x, float z) {
        for (int desplazamiento = -3; desplazamiento <= 3; desplazamiento++) { // Coloca siete franjas paralelas.
            caja(x + desplazamiento, 0.045f, z - 8, 0.48f, 0.025f, 2, 0.86f, 0.87f, 0.80f); // Pintura marfil sobre el asfalto.
        }
    }

    //***************8* SEMAFORO ROJO, AMARILLO Y VERDE CADA 12S************8888


    private void dibujarSemaforo(float x, float z) {
        caja(x, 1.7f, z, 0.13f, 2.8f, 0.13f, 0.24f, 0.29f, 0.31f); // Poste fino, en la misma posición.
        caja(x, 3.1f, z + 0.015f, 0.64f, 1.30f, 0.45f, 0.30f, 0.35f, 0.36f); // Marco exterior.
        caja(x, 3.1f, z, 0.55f, 1.2f, 0.45f, 0.08f, 0.10f, 0.12f); // Dibuja la carcasa de las tres luces.

        // REPITE EL CICLO CADA 12 SEGUNDOS USANDO EL RESTO DEL TIEMPO TOTAL.

        int fase = (int) (tiempo % 12); 

        for (int indice = 0; indice < 3; indice++) { // Recorre rojo arriba, amarillo al centro y verde abajo.
            boolean encendida = false; // Parte de una bombilla apagada.
            if (indice == 0) { // Selecciona la bombilla roja.
                encendida = fase < 5; // Mantiene rojo durante los primeros cinco segundos.
            } else if (indice == 1) { // Selecciona la bombilla amarilla.
                encendida = fase >= 10; // Mantiene amarillo en los últimos dos segundos del ciclo.
            } else { // Selecciona la bombilla verde.
                encendida = fase >= 5 && fase < 10; // Mantiene verde durante los cinco segundos intermedios.
            }
            float brillo = 0.15f; // Conserva un color tenue cuando la bombilla está apagada.
            entero("uEmision", 0); // Configura inicialmente una superficie sin emisión.
            if (encendida) { // Comprueba si esta bombilla corresponde a la fase activa.
                brillo = 1; // Usa intensidad completa para su color.
                entero("uEmision", 1); // Hace que la bombilla se vea encendida.
            }
            float rojo = 0; // Componente roja inicialmente ausente.
            float verde = 0; // Componente verde inicialmente ausente.
            if (indice < 2) { // Rojo y amarillo necesitan componente roja.
                rojo = brillo; // Añade rojo con la intensidad elegida.
            }
            if (indice > 0) { // Amarillo y verde necesitan componente verde.
                verde = brillo; // Añade verde; rojo más verde produce amarillo.
            }
            float altura = 3.45f - indice * 0.35f; // Separa verticalmente las tres bombillas.
            caja(x, altura, z - 0.24f, 0.28f, 0.25f, 0.06f, rojo, verde, 0.02f); // Dibuja la bombilla frente a la carcasa.
            entero("uEmision", 0);
            caja(x, altura + 0.15f, z - 0.30f, 0.38f, 0.05f, 0.24f, 0.12f, 0.16f, 0.18f); // Visera sobre cada lente.
        }
        entero("uEmision", 0); // Evita que el siguiente objeto herede la emisión del semáforo.
    }

    // ==================== 5.1 TRÁFICO AUTÓNOMO ====================

    /** Comprueba dos círculos usando distancia al cuadrado, sin raíz cuadrada. */
    private boolean circulosColisionan(float x1, float z1, float radio1, float x2, float z2, float radio2) {
        // COMPRUEBA COLISIÓN ENTRE DOS VEHÍCULOS USANDO SUS RADIOS.
        float dx = x1 - x2;
        float dz = z1 - z2;
        float radioTotal = radio1 + radio2;
        return dx * dx + dz * dz < radioTotal * radioTotal;
    }

    /** Comprueba el conjunto del jugador en una posición y orientación dadas contra un autónomo. */
    private boolean jugadorColisionaConVehiculo(float x, float z, float orientacion, float vehiculoX, float vehiculoZ) {
        if (circulosColisionan(x, z, RADIO_AUTO, vehiculoX, vehiculoZ, RADIO_AUTONOMO)) {
            return true; // El cuerpo del auto principal toca al autónomo.
        }
        float chataX = x + (float) Math.sin(orientacion) * DISTANCIA_CHATA; // Mismo centro que en la conducción de clase2.
        float chataZ = z + (float) Math.cos(orientacion) * DISTANCIA_CHATA;
        float[] circulos = calcularCirculosChata(chataX, chataZ, orientacion); // Usa exactamente los dos círculos existentes.
        return circulosColisionan(circulos[0], circulos[1], RADIO_CHATA, vehiculoX, vehiculoZ, RADIO_AUTONOMO)
                || circulosColisionan(circulos[2], circulos[3], RADIO_CHATA, vehiculoX, vehiculoZ, RADIO_AUTONOMO);
    }

    /** Revisa la posición propuesta del jugador contra los tres vehículos autónomos. */
    private boolean jugadorColisionaConTrafico(float x, float z, float orientacion) {
        // REVISA EL AUTO Y LOS DOS CÍRCULOS DE LA CHATA CONTRA EL TRÁFICO.
        for (VehiculoAutonomo vehiculo : vehiculos) {
            if (jugadorColisionaConVehiculo(x, z, orientacion, vehiculo.x, vehiculo.z)) {
                return true;
            }
        }
        return false; // El conjunto puede ocupar la posición sin tocar el tráfico.
    }

    /** Crea un vehículo autónomo asignándole una ruta y un color identificativo. */
    private VehiculoAutonomo crearVehiculo(int ruta, float rojo, float verde, float azul) {
        VehiculoAutonomo vehiculo = new VehiculoAutonomo(); // Reserva el estado del nuevo vehículo.
        vehiculo.ruta = ruta; // Guarda la ruta que seguirá por las calles.
        vehiculo.rojo = rojo; // Guarda su color para distinguirlo de los demás.
        vehiculo.verde = verde; // Guarda la componente verde del color.
        vehiculo.azul = azul; // Guarda la componente azul del color.
        colocarEnInicio(vehiculo); // Sitúa el vehículo en el primer waypoint de su ruta.
        return vehiculo; // Devuelve el vehículo listo para circular.
    }

    /** Coloca un vehículo en el primer punto de su ruta y lo orienta hacia el siguiente. */
    private void colocarEnInicio(VehiculoAutonomo vehiculo) {
        vehiculo.x = RUTAS[vehiculo.ruta][0][0]; // Recupera la coordenada X del punto de partida.
        vehiculo.z = RUTAS[vehiculo.ruta][0][1]; // Recupera la coordenada Z del punto de partida.
        vehiculo.indiceObjetivo = 1; // El primer objetivo es el segundo waypoint de la ruta.
        vehiculo.velocidad = 6; // Avanza a seis unidades por segundo por las calles.
        apuntarAlObjetivo(vehiculo); // Calcula la orientación inicial hacia ese waypoint.
    }

    /** Calcula el ángulo hacia el waypoint actual con la convención del auto principal. */
    private void apuntarAlObjetivo(VehiculoAutonomo vehiculo) {
        float objetivoX = RUTAS[vehiculo.ruta][vehiculo.indiceObjetivo][0]; // X del waypoint buscado.
        float objetivoZ = RUTAS[vehiculo.ruta][vehiculo.indiceObjetivo][1]; // Z del waypoint buscado.
        float haciaX = objetivoX - vehiculo.x; // Separación horizontal hasta el waypoint.
        float haciaZ = objetivoZ - vehiculo.z; // Separación en profundidad hasta el waypoint.
        vehiculo.angulo = (float) Math.atan2(-haciaX, -haciaZ); // Conserva frenteX = -sin y frenteZ = -cos.
    }

    /** Mueve cada vehículo autónomo hacia su waypoint y avanza al siguiente al llegar. */
    private void actualizarTrafico(float deltaTime) {
        for (VehiculoAutonomo vehiculo : vehiculos) { // Recorre los vehículos para mantenerlos circulando.
            float objetivoX = RUTAS[vehiculo.ruta][vehiculo.indiceObjetivo][0]; // X del waypoint actual.
            float objetivoZ = RUTAS[vehiculo.ruta][vehiculo.indiceObjetivo][1]; // Z del waypoint actual.
            float haciaX = objetivoX - vehiculo.x; // Distancia horizontal restante.
            float haciaZ = objetivoZ - vehiculo.z; // Distancia en profundidad restante.
            float distancia = (float) Math.sqrt(haciaX * haciaX + haciaZ * haciaZ); // Mide la cercanía al waypoint.
            int objetivoAnterior = vehiculo.indiceObjetivo; // Conserva el waypoint si el paso queda bloqueado.
            float anguloAnterior = vehiculo.angulo; // Conserva también la dirección anterior si no puede avanzar.
            if (distancia < 1.5f) { // Detecta la llegada a una esquina o intersección.
                vehiculo.indiceObjetivo = (vehiculo.indiceObjetivo + 1) % RUTAS[vehiculo.ruta].length; // Pasa al siguiente waypoint.
                apuntarAlObjetivo(vehiculo); // Gira hacia el nuevo tramo de la ruta.
            }
            float frenteX = -(float) Math.sin(vehiculo.angulo); // Frente horizontal, igual que el auto principal.
            float frenteZ = -(float) Math.cos(vehiculo.angulo); // Frente en profundidad, coherente con el jugador.
            float siguienteX = vehiculo.x + frenteX * vehiculo.velocidad * deltaTime; // Propone el siguiente paso en X.
            float siguienteZ = vehiculo.z + frenteZ * vehiculo.velocidad * deltaTime; // Propone el siguiente paso en Z.
            // EVITA QUE UN AUTÓNOMO AVANCE SI EL JUGADOR O LA CHATA BLOQUEAN SU CAMINO.
            if (jugadorColisionaConVehiculo(autoX, autoZ, angulo, siguienteX, siguienteZ)) {
                vehiculo.indiceObjetivo = objetivoAnterior; // No consume el waypoint mientras está detenido.
                vehiculo.angulo = anguloAnterior; // No cambia de dirección al quedar bloqueado.
                continue; // Espera este cuadro; conserva posición, ruta y velocidad de circulación.
            }
            vehiculo.x = siguienteX; // Acepta el paso únicamente cuando el espacio está libre.
            vehiculo.z = siguienteZ; // Continúa normalmente por su ruta original.
        }
    }

    /** Devuelve todos los vehículos autónomos a su punto de partida al reiniciar. */
    private void reiniciarTrafico() {
        for (VehiculoAutonomo vehiculo : vehiculos) { // Recorre los vehículos creados.
            colocarEnInicio(vehiculo); // Repite la colocación inicial de cada uno.
        }
    }

    /** Dibuja los vehículos que circulan automáticamente por las calles. */
    private void dibujarTrafico() {
        for (VehiculoAutonomo vehiculo : vehiculos) { // Recorre los vehículos para dibujarlos.
            piezaVehiculo(vehiculo, 0, 0.6f, 0, 1.55f, 0.5f, 2.4f, vehiculo.rojo, vehiculo.verde, vehiculo.azul); // Carrocería con su color.
            piezaVehiculo(vehiculo, 0, 1.02f, 0.1f, 1.2f, 0.5f, 1.15f, 0.18f, 0.20f, 0.24f); // Cabina oscura del vehículo.
            float[] lados = {-0.82f, 0.82f}; // Define los lados izquierdo y derecho del vehículo.
            float[] ejes = {-0.75f, 0.75f}; // Define los ejes delantero y trasero del vehículo.
            for (float lado : lados) { // Repite las ruedas de cada lado.
                for (float eje : ejes) { // Repite la rueda delantera y la trasera.
                    piezaVehiculo(vehiculo, lado, 0.34f, eje, 0.22f, 0.52f, 0.55f, 0.06f, 0.07f, 0.09f); // Rueda oscura del vehículo.
                    if (!vistaMapa) {
                        piezaVehiculo(vehiculo, lado * 1.10f, 0.34f, eje, 0.05f, 0.25f, 0.28f, 0.53f, 0.59f, 0.62f);
                    }
                }
            }
            if (!vistaMapa) { dibujarDetallesTrafico(vehiculo); } // El minimapa conserva modelos simples.
        }
    }

    /** Completa el modelo sin escribir posición, ruta, waypoint, velocidad ni color del vehículo. */
    private void dibujarDetallesTrafico(VehiculoAutonomo vehiculo) {
        piezaVehiculo(vehiculo, 0, 1.30f, 0.1f, 1.22f, 0.07f, 1.17f, vehiculo.rojo, vehiculo.verde, vehiculo.azul);
        piezaVehiculo(vehiculo, 0, 1.03f, -0.49f, 1.02f, 0.34f, 0.025f, 0.24f, 0.36f, 0.42f); // Parabrisas.
        piezaVehiculo(vehiculo, 0, 1.03f, 0.69f, 1.02f, 0.32f, 0.025f, 0.14f, 0.23f, 0.28f);
        for (int lado = -1; lado <= 1; lado += 2) {
            piezaVehiculo(vehiculo, lado * 0.61f, 1.04f, 0.1f, 0.025f, 0.32f, 0.94f, 0.21f, 0.32f, 0.38f);
            piezaVehiculo(vehiculo, lado * 0.63f, 1.04f, 0.1f, 0.025f, 0.36f, 0.065f, 0.32f, 0.37f, 0.38f); // Pilar lateral.
            piezaVehiculo(vehiculo, 0, 0.42f, lado * 1.22f, 1.43f, 0.14f, 0.10f, 0.21f, 0.26f, 0.28f); // Ambos parachoques.
            entero("uEmision", 1);
            piezaVehiculo(vehiculo, lado * 0.50f, 0.64f, -1.22f, 0.30f, 0.14f, 0.045f, 0.83f, 0.80f, 0.64f);
            piezaVehiculo(vehiculo, lado * 0.50f, 0.64f, 1.22f, 0.28f, 0.13f, 0.045f, 0.67f, 0.13f, 0.10f);
            entero("uEmision", 0); // Lentes visuales únicamente: no añade focos al shader.
        }
    }

    /** Transforma una pieza local de un vehículo autónomo al espacio de la ciudad. */
    private void piezaVehiculo(VehiculoAutonomo vehiculo, float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(vehiculo.angulo); // Coseno de la orientación del vehículo.
        float seno = (float) Math.sin(vehiculo.angulo); // Seno de la orientación del vehículo.
        float mundoX = vehiculo.x + coseno * x + seno * z; // Gira la pieza y suma la posición X del vehículo.
        float mundoZ = vehiculo.z - seno * x + coseno * z; // Gira la pieza y suma la posición Z del vehículo.
        cajaGirada(mundoX, y, mundoZ, sx, sy, sz, r, g, b, vehiculo.angulo); // Dibuja la pieza orientada como el vehículo.
    }

    // ==================== 5.2 SEÑALES DE TRÁNSITO ====================

    /** Reparte señales de tránsito decorativas por distintas zonas de la ciudad. */
    private void dibujarSenalesTransito() {
        dibujarSenalAlto(centro(1) + 4.5f, centro(1) - 4.5f, 0); // ALTO en una esquina del sector norte.
        dibujarSenalEstacionamiento(centro(9) + 4.5f, centro(5) - 4.5f, 1.5708f); // Estacionamiento en el sector este.
        dibujarSenalLimite(centro(9) - 4.5f, centro(3) + 4.5f, 1.5708f); // Límite de velocidad sobre la avenida este.
        dibujarSenalAlto(centro(1) + 4.5f, centro(9) - 4.5f, 0); // Segundo ALTO en el sector sur del mapa.
        dibujarFlechaDireccion(centro(0), centro(4), 0); // Flecha que apunta al norte sobre la avenida oeste.
        dibujarFlechaDireccion(centro(4), centro(10), 1.5708f); // Flecha que apunta al oeste sobre la avenida sur.
    }

    /** Dibuja una señal de ALTO con poste y placa roja enmarcada en blanco. */
    private void dibujarSenalAlto(float x, float z, float angulo) {
        caja(x, 1.4f, z, 0.14f, 2.8f, 0.14f, 0.55f, 0.58f, 0.60f); // Poste metálico de la señal.
        cajaGirada(x, 2.55f, z, 1.0f, 1.0f, 0.10f, 0.92f, 0.92f, 0.92f, angulo); // Placa clara que simula el contorno octogonal.
        cajaGirada(x, 2.55f, z, 0.82f, 0.82f, 0.16f, 0.80f, 0.09f, 0.09f, angulo); // Placa roja frontal del ALTO.
    }

    /** Dibuja una señal de estacionamiento azul con una marca blanca central. */
    private void dibujarSenalEstacionamiento(float x, float z, float angulo) {
        caja(x, 1.4f, z, 0.14f, 2.8f, 0.14f, 0.55f, 0.58f, 0.60f); // Poste metálico de la señal.
        cajaGirada(x, 2.55f, z, 1.0f, 1.0f, 0.16f, 0.90f, 0.90f, 0.90f, angulo); // Marco blanco de la placa.
        cajaGirada(x, 2.55f, z, 0.82f, 0.82f, 0.18f, 0.10f, 0.32f, 0.75f, angulo); // Placa azul de estacionamiento.
        cajaGirada(x, 2.55f, z, 0.42f, 0.42f, 0.22f, 0.90f, 0.90f, 0.90f, angulo); // Marca blanca que identifica la placa.
    }

    /** Dibuja una señal de límite de velocidad con placa roja y centro blanco. */
    private void dibujarSenalLimite(float x, float z, float angulo) {
        caja(x, 1.4f, z, 0.14f, 2.8f, 0.14f, 0.55f, 0.58f, 0.60f); // Poste metálico de la señal.
        cajaGirada(x, 2.55f, z, 1.0f, 1.0f, 0.16f, 0.80f, 0.09f, 0.09f, angulo); // Anillo rojo exterior del límite.
        cajaGirada(x, 2.55f, z, 0.70f, 0.70f, 0.20f, 0.95f, 0.95f, 0.95f, angulo); // Interior blanco de la señal.
    }

    /** Dibuja una flecha de dirección pintada sobre el asfalto y girada según el ángulo. */
    private void dibujarFlechaDireccion(float x, float z, float angulo) {
        piezaFlecha(x, z, angulo, 0, 0, 0.42f, 1.3f, angulo); // Cuerpo blanco de la flecha sobre la calle.
        piezaFlecha(x, z, angulo, -0.34f, -0.72f, 0.42f, 0.66f, angulo + 0.8f); // Lado izquierdo de la punta.
        piezaFlecha(x, z, angulo, 0.34f, -0.72f, 0.42f, 0.66f, angulo - 0.8f); // Lado derecho de la punta.
    }

    /** Coloca una pieza blanca de la flecha en coordenadas locales y la gira con la calle. */
    private void piezaFlecha(float x, float z, float angulo, float localX, float localZ, float ancho, float largo, float giro) {
        float coseno = (float) Math.cos(angulo); // Coseno de la dirección de la flecha.
        float seno = (float) Math.sin(angulo); // Seno de la dirección de la flecha.
        float mundoX = x + coseno * localX + seno * localZ; // Rota la posición local y la suma al centro de la flecha.
        float mundoZ = z - seno * localX + coseno * localZ; // Aplica la misma rotación en profundidad.
        cajaGirada(mundoX, 0.04f, mundoZ, ancho, 0.03f, largo, 0.95f, 0.95f, 0.95f, giro); // Pinta la pieza sobre el asfalto.
    }

    // ==================== 6. MINIMAPA: SEGUNDO PASE DE DIBUJO ====================

    /** Dibuja la escena principal y después la misma ciudad desde arriba, en un recuadro. */
    @Override // Amplía el cuadro completo definido en clase1.
    protected void dibujarFrame() {
        super.dibujarFrame(); // Dibuja primero la vista normal de la ciudad.
        if (mostrarMapa) { // Dibuja el minimapa solo si el usuario no lo ocultó con M.
            int dimensionMenor = Math.min(ancho, alto); // Busca la dimensión que limita el espacio disponible.
            int lado = Math.min(260, dimensionMenor / 3); // Limita el mapa a 260 píxeles y a un tercio de la ventana.
            int margen = Math.min(18, dimensionMenor / 20); // Calcula una separación adaptable respecto a los bordes.
            int x = ancho - lado - margen; // Ubica el recuadro cerca del borde derecho.
            int y = alto - lado - margen; // Ubica el recuadro arriba; OpenGL mide Y desde abajo.
            glEnable(GL_SCISSOR_TEST); // Activa el recorte para no borrar el resto de la escena.
            glScissor(x - 3, y - 3, lado + 6, lado + 6); // Selecciona el mapa más un borde de tres píxeles.
            glClearColor(0.23f, 0.31f, 0.36f, 1); // Marco pizarra discreto; mismas dimensiones y proyección.
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Borra solo el recuadro exterior gracias al scissor.
            glScissor(x, y, lado, lado); // Reduce el recorte al interior del minimapa.
            glClearColor(0.06f, 0.10f, 0.15f, 1); // Define el fondo oscuro del mapa.
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Limpia color y profundidad dentro del mapa.
            glViewport(x, y, lado, lado); // Redirige la proyección al recuadro cuadrado.
            entero("uMapa", 1); // Selecciona la proyección ortográfica del shader de clase1.
            vistaMapa = true; // Indica a escena() que omita decoración pequeña y baliza flotante.
            try { // Asegura que el estado de dibujo se restaure incluso si el segundo pase falla.
                escena(); // Dibuja otra vez la misma ciudad, ahora vista desde arriba.
                dibujarIndicadorAuto(); // Resalta la posición y el frente del jugador en el mapa.
                dibujarNorteMapa(); // Rótulo visual en el margen norte; no gira ni reescala el mapa.
            } finally { // El siguiente cuadro debe volver a la configuración de pantalla completa.
                vistaMapa = false; // Reactiva los detalles de la escena principal.
                entero("uMapa", 0); // Recupera la proyección en perspectiva.
                entero("uEmision", 0); // Restablece también la emisión tras los rótulos.
                glDisable(GL_SCISSOR_TEST); // Permite que la próxima limpieza abarque toda la pantalla.
                glViewport(0, 0, ancho, alto); // Recupera el área de dibujo de la ventana completa.
            }
            if (glGetError() != GL_NO_ERROR) { // Comprueba que el pase del mapa no haya generado errores OpenGL.
                throw new IllegalStateException("Error OpenGL en minimapa"); // Expone el error en la consola.
            }
        }
        dibujarHUD(); // Dibuja el panel de información aunque el minimapa esté oculto con M.

        // SI YA GANÓ, DIBUJA EL PANEL DE VICTORIA ENCIMA DE TODO.

        if (juegoTerminado()) { // Superpone el cartel de victoria cuando se completaron las tres entregas.
            dibujarPanelVictoria(); // Dibuja el panel final con trofeo, tiempo y botones.
        }
    }

    /** Dibuja una marca cian y una punta blanca por encima de los edificios del minimapa. */
    private void dibujarIndicadorAuto() {
        cajaGirada(autoX, 24.8f, autoZ, 2.7f, 0.1f, 3.7f, 0.05f, 0.10f, 0.14f, angulo); // Contorno oscuro para contraste.
        cajaGirada(autoX, 25, autoZ, 2.2f, 0.1f, 3.2f, 0.36f, 0.86f, 0.88f, angulo); // Mismo indicador, acabado menos saturado.
        float frenteX = -(float) Math.sin(angulo); // Calcula la dirección frontal en el eje X.
        float frenteZ = -(float) Math.cos(angulo); // Calcula la dirección frontal en el eje Z.
        float puntaX = autoX + frenteX * 2; // Desplaza la punta dos unidades hacia delante en X.
        float puntaZ = autoZ + frenteZ * 2; // Desplaza la punta dos unidades hacia delante en Z.
        caja(puntaX, 26, puntaZ, 0.9f, 0.1f, 0.9f, 1, 1, 1); // Dibuja la punta blanca encima del indicador cian.
    }

    /** Usa la misma proyección ortográfica; el norte permanece arriba. */
    private void dibujarNorteMapa() {
        entero("uEmision", 1);
        caja(0, 27, -55.2f, 4.0f, 0.12f, 4.7f, 0.07f, 0.12f, 0.17f);
        dibujarPalabra("N", 0, 27.2f, -55.2f, 0.65f, 0.91f, 0.94f, 0.91f);
        entero("uEmision", 0);
    }

/** Dibuja un tablero visual con velocidad, ambiente, faros y controles. */
private void dibujarHUD() {
    int anchoPanel = Math.min(360, ancho - 36); // Panel compacto; se adapta a ventanas pequeñas.
    int altoPanel = Math.min(190, alto - 36);
    int margen = 18; // Mantiene una separación respecto a los bordes de la ventana.
    int x = margen; // Coloca el HUD cerca del borde izquierdo.
    int y = margen; // Coloca el HUD cerca del borde inferior.
    if (anchoPanel <= 0 || altoPanel <= 0) { return; }
    glEnable(GL_SCISSOR_TEST); // Limita la limpieza únicamente al espacio ocupado por el HUD.
    glScissor(x, y, anchoPanel, altoPanel); // Define el rectángulo del tablero.
    glClear(GL_DEPTH_BUFFER_BIT); // Limpia profundidad para asegurar que el HUD quede encima de la ciudad.
    glViewport(x, y, anchoPanel, altoPanel); // Redirige temporalmente el dibujo al área del HUD.
    entero("uMapa", 1); // Utiliza la proyección ortográfica para dibujar la interfaz.
    entero("uEmision", 1); // Evita que la iluminación de la ciudad oscurezca los indicadores.
    try {
        caja(0, 25, 0, 113, 0.12f, 109, 0.25f, 0.32f, 0.36f); // Borde fino de todo el tablero.
        caja(0, 25.15f, 0, 111, 0.12f, 107, 0.045f, 0.065f, 0.09f);
        caja(-21, 25.27f, -3, 64, 0.10f, 66, 0.07f, 0.095f, 0.12f); // Zona del velocímetro.
        caja(-49, 25.4f, -43, 1, 0.10f, 9, 0.36f, 0.78f, 0.80f); // Acento vertical.
        dibujarPalabra("VELOCIDAD", -21, 25.6f, -43, 0.9f, 0.72f, 0.80f, 0.82f);

        int velocidadKm = Math.round(Math.abs(velocidad) * 3.6f); // Misma conversión; nunca modifica velocidad.
        if (velocidadKm > 99) { velocidadKm = 99; }
        dibujarDigitoHUD(velocidadKm / 10, -35, -12);
        dibujarDigitoHUD(velocidadKm % 10, -14, -12);
        dibujarPalabra("KM/H", -23, 25.6f, 9, 0.95f, 0.70f, 0.77f, 0.79f);

        float velocidadNormalizada = Math.min(Math.abs(velocidad) / 40.0f, 1.0f); // Conserva la escala visual anterior.
        caja(-22, 25.4f, 23, 52, 0.10f, 3, 0.18f, 0.23f, 0.27f);
        if (velocidadNormalizada > 0) { // Evita cajas de ancho cero al estar detenido.
            caja(-48 + velocidadNormalizada * 26, 25.55f, 23, velocidadNormalizada * 52, 0.10f, 2, 0.36f, 0.78f, 0.80f);
        }
        dibujarEstadoHUD();
        dibujarProgresoHUD();
        caja(0, 25.35f, 33, 102, 0.08f, 0.5f, 0.22f, 0.29f, 0.33f); // Separador inferior.
        dibujarPalabra("WASD  ESPACIO", -19, 25.6f, 43, 0.70f, 0.65f, 0.73f, 0.77f);
        dibujarPalabra("N F M R", 32, 25.6f, 43, 0.65f, 0.65f, 0.73f, 0.77f);
    } finally { // El HUD nunca deja el viewport, el recorte ni los uniforms activos para la escena 3D.
        entero("uEmision", 0);
        entero("uMapa", 0);
        glDisable(GL_SCISSOR_TEST);
        glViewport(0, 0, ancho, alto);
    }
}

/** Indicadores compactos que solo leen N y F, sin cambiar sus estados. */
private void dibujarEstadoHUD() {
    caja(14, 25.4f, -40, 0.5f, 0.10f, 67, 0.22f, 0.29f, 0.33f); // División vertical.
    dibujarPalabra(noche ? "NOCHE" : "DIA", 34, 25.6f, -40, 0.85f, 0.78f, 0.84f, 0.85f);
    caja(23, 25.5f, -27, 3, 0.10f, 3, noche ? 0.46f : 0.93f, noche ? 0.61f : 0.76f, noche ? 0.80f : 0.40f);
    dibujarPalabra("N", 35, 25.6f, -27, 0.70f, 0.57f, 0.68f, 0.74f);
    dibujarPalabra("FAROS", 34, 25.6f, -13, 0.80f, 0.63f, 0.73f, 0.77f);
    dibujarPalabra(faros ? "ON" : "OFF", 34, 25.6f, -2, 1, faros ? 0.74f : 0.38f, faros ? 0.86f : 0.44f, faros ? 0.68f : 0.48f);
}

/** Progreso de entregas completadas; no altera objetivos ni condiciones de llegada. */
private void dibujarProgresoHUD() {
    dibujarPalabra("ENTREGAS", 34, 25.6f, 12, 0.58f, 0.63f, 0.73f, 0.77f);
    dibujarPalabra(entregas + "/3", 34, 25.6f, 24, 1, 0.95f, 0.77f, 0.43f);
}

/** Dibuja una ayuda sencilla de conducción mediante cuatro botones con flechas. */
private void dibujarFlechasHUD() {
    // Flecha hacia delante.
    caja(-8, 25.2f, 10, 9, 0.15f, 7,
            0.18f, 0.21f, 0.26f); // Fondo del botón superior.

    caja(-8, 25.4f, 10, 1.2f, 0.18f, 4,
            0.85f, 0.90f, 0.95f); // Cuerpo vertical de la flecha.

    caja(-10, 25.4f, 8.5f, 2, 0.18f, 1,
            0.85f, 0.90f, 0.95f); // Parte izquierda de la punta.

    caja(-6, 25.4f, 8.5f, 2, 0.18f, 1,
            0.85f, 0.90f, 0.95f); // Parte derecha de la punta.

    // Flecha izquierda.
    caja(-20, 25.2f, 18, 9, 0.15f, 7,
            0.18f, 0.21f, 0.26f); // Fondo del botón izquierdo.

    caja(-20, 25.4f, 18, 4, 0.18f, 1.2f,
            0.85f, 0.90f, 0.95f); // Cuerpo horizontal de la flecha izquierda.

    caja(-22, 25.4f, 18, 1, 0.18f, 3,
            0.85f, 0.90f, 0.95f); // Punta de la flecha izquierda.

    // Flecha hacia atrás.
    caja(-8, 25.2f, 18, 9, 0.15f, 7,
            0.18f, 0.21f, 0.26f); // Fondo del botón inferior.

    caja(-8, 25.4f, 18, 1.2f, 0.18f, 4,
            0.85f, 0.90f, 0.95f); // Cuerpo vertical de la flecha inferior.

    caja(-10, 25.4f, 19.5f, 2, 0.18f, 1,
            0.85f, 0.90f, 0.95f); // Parte izquierda de la punta inferior.

    caja(-6, 25.4f, 19.5f, 2, 0.18f, 1,
            0.85f, 0.90f, 0.95f); // Parte derecha de la punta inferior.

    // Flecha derecha.
    caja(4, 25.2f, 18, 9, 0.15f, 7,
            0.18f, 0.21f, 0.26f); // Fondo del botón derecho.

    caja(4, 25.4f, 18, 4, 0.18f, 1.2f,
            0.85f, 0.90f, 0.95f); // Cuerpo horizontal de la flecha derecha.

    caja(6, 25.4f, 18, 1, 0.18f, 3,
            0.85f, 0.90f, 0.95f); // Punta de la flecha derecha.
}

/** Dibuja un número de 0 a 9 utilizando siete segmentos luminosos. */
private void dibujarDigitoHUD(int numero, float x, float z) {
    boolean[] segmentos = new boolean[7]; // Guarda qué segmentos deben encenderse.

    switch (numero) {
        case 0 -> segmentos = new boolean[]{true, true, true, true, true, true, false};
        case 1 -> segmentos = new boolean[]{false, true, true, false, false, false, false};
        case 2 -> segmentos = new boolean[]{true, true, false, true, true, false, true};
        case 3 -> segmentos = new boolean[]{true, true, true, true, false, false, true};
        case 4 -> segmentos = new boolean[]{false, true, true, false, false, true, true};
        case 5 -> segmentos = new boolean[]{true, false, true, true, false, true, true};
        case 6 -> segmentos = new boolean[]{true, false, true, true, true, true, true};
        case 7 -> segmentos = new boolean[]{true, true, true, false, false, false, false};
        case 8 -> segmentos = new boolean[]{true, true, true, true, true, true, true};
        case 9 -> segmentos = new boolean[]{true, true, true, true, false, true, true};
    }

    float apagado = 0.14f; // Segmentos apagados discretos.
    float rojo = 0.90f; // Dígitos grandes en blanco frío.
    float verde = 0.95f;
    float azul = 0.94f;

    dibujarSegmentoHUD(segmentos[0], x, z - 11, 11, 1.4f,
            apagado, rojo, verde, azul); // Segmento superior.

    dibujarSegmentoHUD(segmentos[1], x + 6, z - 5.5f, 1.4f, 9.6f,
            apagado, rojo, verde, azul); // Segmento superior derecho.

    dibujarSegmentoHUD(segmentos[2], x + 6, z + 5.5f, 1.4f, 9.6f,
            apagado, rojo, verde, azul); // Segmento inferior derecho.

    dibujarSegmentoHUD(segmentos[3], x, z + 11, 11, 1.4f,
            apagado, rojo, verde, azul); // Segmento inferior.

    dibujarSegmentoHUD(segmentos[4], x - 6, z + 5.5f, 1.4f, 9.6f,
            apagado, rojo, verde, azul); // Segmento inferior izquierdo.

    dibujarSegmentoHUD(segmentos[5], x - 6, z - 5.5f, 1.4f, 9.6f,
            apagado, rojo, verde, azul); // Segmento superior izquierdo.

    dibujarSegmentoHUD(segmentos[6], x, z, 11, 1.4f,
            apagado, rojo, verde, azul); // Segmento central.
}

/** Dibuja un segmento encendido o apagado del velocímetro digital. */
private void dibujarSegmentoHUD(
        boolean encendido,
        float x,
        float z,
        float anchoSegmento,
        float largoSegmento,
        float apagado,
        float rojo,
        float verde,
        float azul) {

    if (encendido) { // Usa un color luminoso cuando el segmento forma parte del número.
        caja(
                x,
                25.5f,
                z,
                anchoSegmento,
                0.18f,
                largoSegmento,
                rojo,
                verde,
                azul
        );
    } else { // Conserva una marca oscura para que el display siga siendo reconocible.
        caja(
                x,
                25.4f,
                z,
                anchoSegmento,
                0.15f,
                largoSegmento,
                apagado,
                apagado,
                apagado
        );
    }
}

    // ==================== 7. PANEL DE VICTORIA ====================

    private static final float ORTO_PANEL = 58; // Extensión ortográfica que el shader usa para la interfaz.
    private static final float BOTON_ANCHO = 38; // Ancho del área interactiva de los botones del panel.
    private static final float BOTON_ALTO = 15; // Alto del área interactiva de los botones del panel.
    private static final float BOTON_JUGAR_X = -22.5f; // Centro horizontal del botón JUGAR.
    private static final float BOTON_SALIR_X = 22.5f; // Centro horizontal del botón SALIR.
    private static final float BOTON_Z = 21; // Centro vertical común de los dos botones.
    private boolean mouseSobreJugar = false; // Indica si el cursor está encima del botón JUGAR.
    private boolean mouseSobreSalir = false; // Indica si el cursor está encima del botón SALIR.
    private boolean mousePresionadoAntes = false; // Recuerda si el clic izquierdo ya estaba pulsado.
    private boolean panelVictoriaVisible = false; // Recuerda si el cursor ya se preparó para el panel.
    private int modoCursorAnterior = GLFW_CURSOR_NORMAL; // Guarda el modo de cursor previo al panel.

    /** Indica si el jugador ya completó todas las entregas pendientes. */

    // DEVUELVE TRUE CUANDO YA SE COMPLETARON LAS 3 ENTREGAS.

    private boolean juegoTerminado() {
        return entregas >= DESTINOS.length; // La victoria se alcanza al cerrar la última parada.
    }

    /** Calcula el rectángulo centrado del panel como {x, y, lado} en píxeles. */
    private int[] calcularPanel() {
        int dimensionMenor = Math.min(ancho, alto); // Busca el lado que limita el espacio disponible.
        int lado = Math.min(560, (int) (dimensionMenor * 0.85f)); // Mantiene el panel cuadrado y adaptable.
        int x = (ancho - lado) / 2; // Centra el panel en horizontal.
        int y = (alto - lado) / 2; // Centra el panel en vertical.
        return new int[]{x, y, lado}; // Comparte posición y tamaño con el dibujo y con el mouse.
    }

    /** Convierte la posición del cursor a coordenadas X/Z del panel, o null si no hay panel. */
    private float[] cursorEnPanel() {
        int[] marco = calcularPanel(); // Recupera el mismo rectángulo que usa el dibujo.
        if (marco[2] <= 0) { // Comprueba que exista un área de panel válida.
            return null; // Sin panel no hay zona interactiva.
        }
        double[] cursorX = new double[1]; // Reserva espacio para la coordenada X del cursor.
        double[] cursorY = new double[1]; // Reserva espacio para la coordenada Y del cursor.
        glfwGetCursorPos(ventana, cursorX, cursorY); // Lee el cursor en píxeles con origen arriba-izquierda.
        float pantallaX = (float) ((cursorX[0] - marco[0]) / marco[2] * 2 - 1); // Lleva la X al rango -1 a 1 del panel.
        float pantallaY = (float) (((alto - cursorY[0]) - marco[1]) / marco[2] * 2 - 1); // Invierte la Y al origen de OpenGL.
        return new float[]{pantallaX * ORTO_PANEL, -pantallaY * ORTO_PANEL}; // Devuelve las coordenadas X y Z del panel.
    }

    /** Comprueba si el cursor se encuentra dentro del área de un botón del panel. */
    private boolean mouseSobreBoton(float centroX, float centroZ) {
        float[] cursor = cursorEnPanel(); // Convierte el cursor a coordenadas del panel.
        if (cursor == null) { // Comprueba que el panel esté disponible.
            return false; // Sin panel no hay botón que pulsar.
        }
        boolean dentroX = Math.abs(cursor[0] - centroX) <= BOTON_ANCHO / 2; // Compara la distancia horizontal al botón.
        boolean dentroZ = Math.abs(cursor[1] - centroZ) <= BOTON_ALTO / 2; // Compara la distancia vertical al botón.
        return dentroX && dentroZ; // El cursor está encima solo si cae dentro de la caja.
    }

    /** Actualiza el resaltado y el clic de los botones del panel de victoria. */
    private void actualizarPanelVictoria() {
        mouseSobreJugar = mouseSobreBoton(BOTON_JUGAR_X, BOTON_Z); // Comprueba si el cursor está sobre JUGAR.
        mouseSobreSalir = mouseSobreBoton(BOTON_SALIR_X, BOTON_Z); // Comprueba si el cursor está sobre SALIR.
        boolean mousePresionado = glfwGetMouseButton(ventana, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS; // Lee el clic izquierdo actual.
        if (mousePresionado && !mousePresionadoAntes) { // Actúa solo en el instante en que comienza la pulsación(EL CLIC).


            if (mouseSobreJugar) { // Comprueba si el clic cayó sobre JUGAR.
                reiniciar(); // Reutiliza el reinicio existente para comenzar otra partida.
            } else if (mouseSobreSalir) { // Comprueba si el clic cayó sobre SALIR.
                glfwSetWindowShouldClose(ventana, true); // Solicita cerrar la ventana por el camino normal de GLFW.
            }
        }
        mousePresionadoAntes = mousePresionado; // Conserva el estado para detectar el próximo flanco de pulsación.
    }




    // construye y muestra el menú final de victoria encima de la escena, 
    // con el mensaje GANASTE, el tiempo y los botones de reinicio y salida.

    private void dibujarPanelVictoria() {
        int[] marco = calcularPanel(); // Obtiene el mismo rectángulo que usa la detección del mouse.
        int lado = marco[2]; // Extrae el tamaño cuadrado del panel.
        int x = marco[0]; // Extrae la posición horizontal del panel.
        int y = marco[1]; // Extrae la posición vertical del panel.

        glEnable(GL_SCISSOR_TEST); // Limita la limpieza únicamente al recuadro del panel.
        glScissor(x, y, lado, lado); // Selecciona solo el área que ocupará el cartel.
        glClear(GL_DEPTH_BUFFER_BIT); // Borra la profundidad para que el cartel quede delante de la ciudad.

        glViewport(x, y, lado, lado); // Redirige el dibujo al recuadro cuadrado del panel.
        entero("uMapa", 1); // activa uMapa = 1 para usar la proyección 2D;
        entero("uEmision", 1); // activa uEmision = 1 para que los colores se vean brillantes;

        try { // Solo cambia la presentación; posiciones y áreas de los botones permanecen intactas.
        caja(0, 25.0f, 0, 100, 0.10f, 64, 0.02f, 0.03f, 0.05f); // Sombra que separa el cartel de la escena.
        caja(0, 25.15f, 0, 96, 0.14f, 60, 0.41f, 0.47f, 0.49f); // Marco metálico discreto.
        caja(0, 25.3f, 0, 94, 0.14f, 58, 0.055f, 0.08f, 0.11f); // Interior oscuro de dos niveles.
        caja(0, 25.4f, -20, 90, 0.08f, 16, 0.09f, 0.13f, 0.16f); // Cabecera elevada.
        caja(0, 25.45f, -11, 82, 0.06f, 0.45f, 0.78f, 0.65f, 0.37f); // Separación limpia entre título y trofeo.

        dibujarPalabra("GANASTE", 0, 25.5f, -20, 2.6f, 1.0f, 0.86f, 0.30f); // Título dorado centrado.
        dibujarTrofeoVictoria(0, 25.5f, -3); // Trofeo que celebra la victoria.
        dibujarPalabra("TIEMPO " + (int) tiempo + "S", 0, 25.5f, 9, 1.5f, 0.90f, 0.92f, 0.95f); // Muestra el tiempo final.

        dibujarBotonReiniciar(BOTON_JUGAR_X, BOTON_Z, mouseSobreJugar); // Botón verde, resaltado si el cursor está encima.
        dibujarBotonSalir(BOTON_SALIR_X, BOTON_Z, mouseSobreSalir); // Botón rojo, resaltado si el cursor está encima.

        } finally {
        entero("uEmision", 0); // Restablece la iluminación normal de la ciudad.
        entero("uMapa", 0); // Regresa a la proyección en perspectiva.
        glDisable(GL_SCISSOR_TEST); // Permite volver a dibujar sobre toda la ventana.
        glViewport(0, 0, ancho, alto); // Recupera el área de dibujo completa de la ventana.
        }
    }

    /** Dibuja un trofeo dorado utilizando cajas sencillas. */
    private void dibujarTrofeoVictoria(float x, float y, float z) {
        caja(x, y, z - 6, 7.5f, 0.12f, 3.5f, 0.95f, 0.78f, 0.25f); // Copa ancha del trofeo.
        caja(x, y + 0.04f, z - 6, 6, 0.14f, 2.2f, 1.0f, 0.88f, 0.35f); // Brillo interior de la copa.
        caja(x, y, z - 2.5f, 2, 0.12f, 4.5f, 0.80f, 0.62f, 0.16f); // Cuerpo que sostiene la copa.
        caja(x, y, z + 2, 4, 0.12f, 2, 0.80f, 0.62f, 0.16f); // Tallo del trofeo.
        caja(x, y, z + 5, 7, 0.12f, 2.5f, 0.70f, 0.52f, 0.12f); // Base del trofeo.
        caja(x - 4.5f, y, z - 5, 2, 0.12f, 4, 0.85f, 0.68f, 0.18f); // Asa izquierda de la copa.
        caja(x + 4.5f, y, z - 5, 2, 0.12f, 4, 0.85f, 0.68f, 0.18f); // Asa derecha de la copa.
    }

    /** Dibuja el botón verde que permite reiniciar la partida con la tecla R. */
    private void dibujarBotonReiniciar(float x, float z, boolean seleccionado) {
        float brillo = seleccionado ? 1.35f : 1.0f; // Hover más evidente sin cambiar el área interactiva.
        caja(x, 25.5f, z, BOTON_ANCHO, 0.15f, BOTON_ALTO, seleccionado ? 0.55f : 0.24f, 0.61f, 0.48f);
        caja(x, 25.65f, z, 35, 0.15f, 12.5f, 0.12f * brillo, 0.30f * brillo, 0.25f * brillo);
        dibujarPalabra("JUGAR", x, 25.8f, z, 1.6f, 0.95f, 1.0f, 0.95f); // Etiqueta que invita a reiniciar.
    }

    /** Dibuja el botón rojo que permite salir de la aplicación con la tecla ESC. */
    private void dibujarBotonSalir(float x, float z, boolean seleccionado) {
        float brillo = seleccionado ? 1.35f : 1.0f;
        caja(x, 25.5f, z, BOTON_ANCHO, 0.15f, BOTON_ALTO, seleccionado ? 0.80f : 0.53f, 0.38f, 0.32f);
        caja(x, 25.65f, z, 35, 0.15f, 12.5f, 0.31f * brillo, 0.16f * brillo, 0.15f * brillo);
        dibujarPalabra("SALIR", x, 25.8f, z, 1.6f, 1.0f, 0.94f, 0.92f); // Etiqueta que invita a salir.
    }

    /** Escribe una palabra usando el tipo de letra de 3x5 celdas, centrada en su posición. */
    private void dibujarPalabra(String texto, float x, float y, float z, float escala, float r, float g, float b) {
        float ancho = 4 * escala; // Reserva tres columnas de letra más una de separación.
        float inicio = x - (texto.length() - 1) * ancho / 2; // Desplaza el texto para centrarlo.
        for (int i = 0; i < texto.length(); i++) { // Recorre cada carácter de la palabra.
            dibujarLetra(patronLetra(texto.charAt(i)), inicio + i * ancho, y, z, escala, r, g, b); // Dibuja la letra en su celda.
        }
    }

    /** Dibuja una letra de 3x5 celdas a partir de su patrón de bits. */
    private void dibujarLetra(int[] patron, float x, float y, float z, float escala, float r, float g, float b) {
        for (int fila = 0; fila < 5; fila++) { // Recorre las cinco filas del glifo.
            for (int columna = 0; columna < 3; columna++) { // Recorre las tres columnas del glifo.
                int mascara = 1 << (2 - columna); // Prepara la máscara de la columna actual.
                if ((patron[fila] & mascara) != 0) { // Comprueba si la celda forma parte de la letra.
                    float celdaX = x + (columna - 1) * escala; // Calcula el centro horizontal de la celda.
                    float celdaZ = z + (fila - 2) * escala; // Coloca la primera fila arriba en pantalla.
                    caja(celdaX, y, celdaZ, escala, 0.10f, escala, r, g, b); // Dibuja una celda de la letra.
                }
            }
        }
    }

    /** Devuelve el patrón de 3x5 celdas correspondiente a un carácter. */
    private int[] patronLetra(char caracter) {
        return switch (caracter) { // Selecciona el glifo según la letra o el dígito solicitado.
            case 'A' -> new int[]{0b010, 0b101, 0b111, 0b101, 0b101}; // A mayúscula.
            case 'C' -> new int[]{0b111, 0b100, 0b100, 0b100, 0b111}; // Rótulos del tablero, sin fuentes externas.
            case 'D' -> new int[]{0b110, 0b101, 0b101, 0b101, 0b110};
            case 'F' -> new int[]{0b111, 0b100, 0b110, 0b100, 0b100};
            case 'H' -> new int[]{0b101, 0b101, 0b111, 0b101, 0b101};
            case 'K' -> new int[]{0b101, 0b101, 0b110, 0b101, 0b101};
            case 'V' -> new int[]{0b101, 0b101, 0b101, 0b101, 0b010};
            case 'W' -> new int[]{0b101, 0b101, 0b101, 0b111, 0b101};
            case '/' -> new int[]{0b001, 0b001, 0b010, 0b100, 0b100};
            case 'E' -> new int[]{0b111, 0b100, 0b111, 0b100, 0b111}; // E mayúscula.
            case 'G' -> new int[]{0b111, 0b100, 0b101, 0b101, 0b011}; // G mayúscula.
            case 'I' -> new int[]{0b111, 0b010, 0b010, 0b010, 0b111}; // I mayúscula.
            case 'J' -> new int[]{0b001, 0b001, 0b001, 0b101, 0b111}; // J mayúscula.
            case 'L' -> new int[]{0b100, 0b100, 0b100, 0b100, 0b111}; // L mayúscula.
            case 'M' -> new int[]{0b101, 0b111, 0b101, 0b101, 0b101}; // M mayúscula.
            case 'N' -> new int[]{0b101, 0b111, 0b111, 0b101, 0b101}; // N mayúscula.
            case 'O' -> new int[]{0b111, 0b101, 0b101, 0b101, 0b111}; // O mayúscula.
            case 'P' -> new int[]{0b111, 0b101, 0b111, 0b100, 0b100}; // P mayúscula.
            case 'R' -> new int[]{0b111, 0b101, 0b111, 0b110, 0b101}; // R mayúscula.
            case 'S' -> new int[]{0b011, 0b100, 0b010, 0b001, 0b110}; // S mayúscula.
            case 'T' -> new int[]{0b111, 0b010, 0b010, 0b010, 0b010}; // T mayúscula.
            case 'U' -> new int[]{0b101, 0b101, 0b101, 0b101, 0b111}; // U mayúscula.
            case 'X' -> new int[]{0b101, 0b101, 0b010, 0b101, 0b101}; // X mayúscula.
            case '0' -> new int[]{0b111, 0b101, 0b101, 0b101, 0b111}; // Dígito cero.
            case '1' -> new int[]{0b010, 0b110, 0b010, 0b010, 0b111}; // Dígito uno.
            case '2' -> new int[]{0b111, 0b001, 0b111, 0b100, 0b111}; // Dígito dos.
            case '3' -> new int[]{0b111, 0b001, 0b111, 0b001, 0b111}; // Dígito tres.
            case '4' -> new int[]{0b101, 0b101, 0b111, 0b001, 0b001}; // Dígito cuatro.
            case '5' -> new int[]{0b111, 0b100, 0b111, 0b001, 0b111}; // Dígito cinco.
            case '6' -> new int[]{0b111, 0b100, 0b111, 0b101, 0b111}; // Dígito seis.
            case '7' -> new int[]{0b111, 0b001, 0b010, 0b010, 0b010}; // Dígito siete.
            case '8' -> new int[]{0b111, 0b101, 0b111, 0b101, 0b111}; // Dígito ocho.
            case '9' -> new int[]{0b111, 0b101, 0b111, 0b001, 0b111}; // Dígito nueve.
            default -> new int[]{0, 0, 0, 0, 0}; // Espacio o carácter no dibujado.
        };
    }

    /** Punto de entrada del proyecto final. */
    public static void main(String[] args) {
        clase4 aplicacion = new clase4(); // Crea la versión con todas las etapas acumuladas.
        aplicacion.run(); // Inicia el ciclo de vida completo de la aplicación.
    }
}
