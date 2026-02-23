# Simulador de Ecosistema

Este proyecto implementa un simulador de ecosistema que modela las interacciones entre animales carnívoros y herbívoros
en un entorno dinámico. El objetivo principal es aplicar principios de diseño orientado a objetos, utilizando genéricos
y colecciones en Java para crear un sistema complejo y realista.

El simulador reproduce un ecosistema bidimensional donde coexisten lobos (carnívoros) y ovejas (herbívoros), cada uno
con comportamientos autónomos determinados por su estado interno y el entorno que perciben. Los animales pueden
encontrarse en diferentes estados: normal, hambriento, buscando pareja, huyendo de un peligro o muerto. Estas
transiciones de estado dependen de factores como la energía disponible, el deseo de reproducción, la edad y la presencia
de otros animales en su campo visual.

El mundo del simulador está organizado en una matriz de regiones que funcionan como fuentes de alimento para los
herbívoros. Los animales consumen energía al moverse y realizar actividades, y deben alimentarse para sobrevivir. Cuando
dos animales compatibles se encuentran, pueden reproducirse, generando descendencia que hereda características de sus
progenitores con pequeñas mutaciones, permitiendo así la evolución del ecosistema.

La simulación utiliza un sistema de tiempo discreto donde cada paso actualiza el estado de todos los animales y
regiones. El sistema permite configurar el estado inicial mediante archivos JSON.
Los resultados de la simulación se pueden visualizar en tiempo real y exportar en formato JSON para su análisis posterior.

Este proyecto constituye la práctica de la asignatura de Tecnología de la Programación II de la Universidad Complutense
de Madrid del curso 2025/2026.

## ¿Como compilar?
`mvn clean compile`

## ¿Como ejecutar (una vez compilado)?
`Pendiente de documentar`