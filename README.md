# Ruleta DEDsafío (Forge 1.20.1, 47.3.0)

Uso (necesita el mod en el servidor Y en los clientes):
1. `/give @s ruleta_dedsafio:ruleta_llave` (Dedita Celeste), `ruleta_altar` (altar visible) y/o `ruleta_trigger` (bloque activador invisible).
2. Clic derecho en el altar/activador con la Llave en la mano.
3. Comandos (OP): `/ruleta iniciar [x y z]` y `/ruleta detener`.

Compilar: sube todo a GitHub (incluida la carpeta `.github`), pestaña Actions > build > descarga el .jar de "Artifacts".
Local: `gradle wrapper --gradle-version 8.1.1` y luego `./gradlew build` (Java 17). El jar queda en `build/libs/`.

Evento para premios: `RuletaFinishedEvent` (sector 0 verde, 1 cian, 2 azul, 3 morado, 4 rojo, 5 naranja, 6 amarillo).
Ajustes de tiempos y tamaño: `Timeline.java`.
