# FitRank: Récords y Competición en tus Rutinas de Entrenamiento
Una aplicación móvil desarrollada en Kotlin para Android, diseñada para transformar el seguimiento de tus rutinas de gimnasio en una experiencia competitiva y motivadora.

## 1. Introducción: Propósito y Usuarios Destinatarios

FitRank tiene como propósito central convertir el registro de entrenamiento en un sistema de puntuación dinámico y social. Buscamos motivar a los usuarios a la mejora constante, premiando el esfuerzo con un sistema de ligas y proporcionando seguimiento detallado de su progresión.

### 1.1. Usuarios Destinatarios

Usuarios que requieren un registro preciso del peso y repeticiones para evaluar su progreso.

Personas motivadas por la competencia que disfrutan de ver su rendimiento comparado con el de amigos.

## 2. Características Principales

La aplicación se organiza en tres pestañas principales para gestionar y visualizar el rendimiento del usuario:

### 2.1. Panel de Rendimiento Personal (Home)

Esta pestaña actúa como el centro de estadísticas.

Gráfico Radial de Rendimiento: Generado con la librería Koalaplot. Este gráfico desglosa las estadísticas de fuerza del usuario en cinco grupos musculares principales: Brazos, Pecho, Espalda, Piernas, y Core.

Clasificación por Grados: El rendimiento en cada grupo muscular se califica utilizando un sistema de grados (del más alto al más bajo): S, A, B, C, y F.

### 2.2. Gestión de Rutinas (Mi Rutina)

Permite el registro detallado de los entrenamientos realizados, utilizando CardViews dentro de un RecyclerView.

### 2.3. Sistema de Ranking Competitivo (Ranking)

Una tabla de clasificación dinámica que promueve la competencia.

Sistema de Ligas: La puntuación del entrenamiento define la liga a la que pertenece el usuario:


| Liga  | Puntos requeridos |
| ------------- | ------------- |
| Cobre  | 0 - 500  |
| Bronce  | 501 - 1000  |
| Plata  | 1001 - 1500  |
| Oro  | 1501 - 2000  |
| Esmeralda  | 2001 - 2500  |
| Diamante  | 2501 - 3000  |
| Campeón  | 3001 - 3500  |


### 2.4. Funcionalidades Transversales

Autenticación: Implementada mediante correo electrónico, gestionada por Supabase Auth.

Personalización: Preferencias de Tema (ej. modo oscuro) guardadas localmente con SharedPreferences.

Notificaciones: Alertas en tiempo real a través de Supabase Realtime si otro usuario supera la puntuación.

## 3. Tecnologías Utilizadas

FitRank está desarrollada en Kotlin para Android, utilizando Supabase como plataforma backend principal.

### 3.1. Stack Principal

| Categoría          | Tecnología            | Función específica                 |
|--------------------|------------------------|------------------------------------|
| Lenguaje           | Kotlin                 | Desarrollo nativo Android          |
| Backend            | Supabase               | Auth, BD, Storage, Realtime        |
| UI                 | RecyclerView, CardViews| Listas dinámicas                   |
| Gráficos           | Koalaplot              | Gráfico radial                     |
| Persistencia local | SharedPreferences      | Guardar preferencias               |


Almacenamiento de preferencias de tema (ej. modo oscuro).

### 3.2. Detalle de Implementación de Supabase

| Módulo Supabase | Función         | Uso en FitRank                     |
|-----------------|------------------|------------------------------------|
| Auth            | Autenticación    | Registros e inicios de sesión      |
| Postgres        | Base de datos    | Rutinas, puntos y usuarios         |
| Storage         | Archivos         | Foto de perfil                     |
| Realtime        | Eventos          | Notificaciones de ranking          |


## 4. Capturas de Pantalla (Opcional)

Home (Estadísticas Musculares)

Mi Rutina (Registro)

Ranking (Clasificación)





