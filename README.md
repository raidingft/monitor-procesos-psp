# Monitor de Procesos

## 👨‍💻 Autor

**Saúl Fernández Torres**  
2º DAM - PSP  
Fecha de Entrega: 16/11/2025

## 📋 Índice

1. [Introducción](#introducción)
2. [Tecnologías Utilizadas](#tecnologías-utilizadas)
3. [Arquitectura](#arquitectura)
4. [Funcionalidades Principales](#funcionalidades-principales)
5. [Requisitos del Sistema](#requisitos-del-sistema)
6. [Instalación y Ejecución](#instalación-y-ejecución)
7. [Manual de Usuario](#manual-de-usuario)
8. [Pruebas Realizadas](#pruebas-realizadas)
9. [Conclusiones](#conclusiones)
10. [Bibliografía](#bibliografía)

---

## 🎯 Introducción

Monitor de Procesos es una aplicación multiplataforma desarrollada en Kotlin que permite visualizar, filtrar y gestionar los procesos en ejecución en sistemas operativos Windows, Linux y macOS. La aplicación utiliza Compose for Desktop para ofrecer una interfaz gráfica moderna e intuitiva.

### Requisitos Generales Implementados

- ✅ Visualización detallada de procesos (PID, nombre, usuario, CPU, memoria, estado, tipo)
- ✅ Uso total de CPU y memoria del sistema
- ✅ Filtrado por nombre, usuario y estado
- ✅ Eliminación de procesos con manejo de errores
- ✅ Actualización automática tras eliminación
- ✅ Botón de refresco manual
- ✅ Interfaz multiplataforma y redimensionable

---

## 💻 Tecnologías Utilizadas

### Lenguaje: Kotlin

Lenguaje moderno de JetBrains con las siguientes características:

- Soporte multiplataforma
- Compatibilidad con librerías Java
- Código seguro frente a errores null
- Manejo eficiente con corrutinas para tareas asíncronas

### Framework: Compose Multiplatform

Framework declarativo para construcción de interfaces reactivas:

- Creación de UI con código Kotlin sin XML
- Soporte multiplataforma con código compartido
- Recomposición inteligente de elementos visuales
- Reutilización de componentes

### APIs del Sistema

- **ProcessBuilder**: Ejecución de comandos del sistema (`tasklist`, `ps`)
- **ManagementFactory**: Acceso a información de bajo nivel del sistema
- **OperatingSystemMXBean**: Métricas del sistema operativo para cálculo de porcentajes

---

## 🏗️ Arquitectura
```
┌─────────────────────────────────────┐
│         Interfaz (Compose)          │
│   - Filtros                         │
│   - Tabla de procesos               │
│   - Controles                       │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      Lógica de Negocio (Kotlin)     │
│   - Gestión de estado               │
│   - Procesamiento de datos          │
│   - Corrutinas                      │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│        APIs del Sistema             │
│   - ProcessBuilder                  │
│   - ManagementFactory               │
│   - OperatingSystemMXBean           │
└─────────────────────────────────────┘
```

---

## ⚡ Funcionalidades Principales

### 1. Monitor de Procesos

- Pantalla de carga inicial mientras obtiene información del sistema
- Visualización completa de todos los procesos en ejecución
- Ventana redimensionable

### 2. Tarjetas de Totales

Muestra en la parte superior:

- **CPU Total**: Porcentaje de uso del procesador
- **Memoria Total**: Porcentaje de RAM utilizada

Valores actualizados al refrescar la lista.

### 3. Sistema de Filtros

#### Filtro por Nombre
Busca procesos que contengan el texto introducido en su nombre.

#### Filtro por Usuario
Muestra solo los procesos del usuario especificado.

#### Filtro por Estado
Adaptado dinámicamente según el sistema operativo:

**Windows:**
- Running
- Not Responding
- Unknown

**Linux/macOS:**
- Running
- Sleeping
- Zombie
- Stopped
- Idle
- Background

### 4. Tabla de Procesos

Columnas mostradas:

| PID | Nombre | Usuario | CPU (%) | MEM (MB) | Estado | Tipo |
|-----|--------|---------|---------|----------|--------|------|

- Selección múltiple mediante clic
- Contador de procesos mostrados/totales
- Resaltado visual de procesos seleccionados

### 5. Controles

#### Botón Refrescar
- Actualiza la lista completa de procesos
- Muestra estado "Actualizando..." durante la operación
- Mensaje de confirmación al finalizar

#### Botón Limpiar Filtros
- Restablece todos los filtros a valores predeterminados
- Actualización instantánea

#### Botón Finalizar
Termina los procesos seleccionados con manejo de errores:

- ✅ **Éxito**: Proceso finalizado correctamente
- ⚠️ **Sin permisos**: Error al intentar finalizar proceso protegido
- ⚠️ **No encontrado**: Proceso ya cerrado
- ⚠️ **Mixto**: Algunos finalizados, otros fallidos
- ⚠️ **Sin selección**: Aviso si no hay procesos seleccionados

---

## 📦 Requisitos del Sistema

### Software Necesario

- Kotlin 1.9+
- Compose Multiplatform 1.5
- JDK 17 o superior
- IntelliJ IDEA
- Sistema operativo: Windows, Linux o macOS

---

## 🚀 Instalación y Ejecución

### Pasos para Ejecutar

1. **Clonar el repositorio:**
```bash
   git clone https://github.com/raidingft/monitor-procesos-psp.git
   cd monitor-procesos-psp
```

2. **Abrir en IntelliJ IDEA:**
    - File → Open → Seleccionar carpeta del proyecto

3. **Configurar JDK:**
    - File → Project Structure → Project SDK
    - Verificar que sea JDK 17 o superior

4. **Esperar carga de dependencias:**
    - Gradle descargará automáticamente las dependencias necesarias

5. **Ejecutar:**
    - Abrir `Main.kt`
    - Clic en el botón "Run" (▶️)

---

## 📖 Manual de Usuario

### Pantalla Principal

Al iniciar verás:

1. **Zona Superior**: Tarjetas con uso total de CPU y Memoria
2. **Barra de Filtros**: Campos de texto y menú desplegable
3. **Tabla Central**: Lista de procesos con toda su información
4. **Botones de Control**: Refrescar, Limpiar Filtros, Finalizar

### Operaciones Disponibles

#### Filtrar Procesos

1. **Por nombre**: Escribe en el campo "Proceso"
2. **Por usuario**: Escribe en el campo "Usuario"
3. **Por estado**: Selecciona del menú desplegable
4. Los filtros se aplican instantáneamente y pueden combinarse

#### Limpiar Filtros

- Clic en "Limpiar Filtros" para resetear todos los campos
- El cambio es inmediato

#### Actualizar Lista

- Clic en "Refrescar"
- El botón se deshabilitará mostrando "Actualizando..."
- Aparecerá mensaje de confirmación al finalizar

#### Finalizar Procesos

1. Selecciona uno o varios procesos haciendo clic en ellos (se resaltarán en azul)
2. Clic en "Finalizar"
3. El sistema mostrará:
    - Mensaje de "Finalizando..."
    - Resultado de la operación (éxito/error)
    - Actualización automática de la tabla

### Casos de Uso Comunes

**Ver procesos de un usuario específico:**
- Escribe el nombre del usuario en el campo "Usuario"

**Ver solo procesos activos:**
- Selecciona "Running" en el filtro de estado

**Buscar un proceso específico:**
- Escribe parte del nombre en el campo "Proceso"

**Cerrar un programa:**
1. Busca el proceso (ej: "notepad")
2. Selecciónalo en la tabla
3. Clic en "Finalizar"

---

## 🧪 Pruebas Realizadas

| Nº | Función | Descripción | Resultado Esperado | Estado |
|----|---------|-------------|-------------------|--------|
| 1 | Carga de procesos | Iniciar aplicación | Se muestran todos los procesos tras 2-3 segundos | ✅ Correcto |
| 2 | Botón "Refrescar" | Actualizar lista | Se actualizan totales y procesos | ✅ Correcto |
| 3 | Botón "Limpiar Filtros" | Aplicar y limpiar filtros | Campos vuelven a estado inicial instantáneamente | ✅ Correcto |
| 4 | Botón "Finalizar" (éxito) | Finalizar proceso de usuario | Proceso cerrado y mensaje de éxito | ✅ Correcto |
| 5 | Botón "Finalizar" (sin permiso) | Intentar cerrar "System" | Mensaje de error por permisos | ✅ Correcto |

---

## 💡 Conclusiones

### Logros

- ✅ Implementación completa de un monitor de procesos funcional
- ✅ Interfaz intuitiva similar al Administrador de Tareas de Windows
- ✅ Soporte multiplataforma (Windows, Linux, macOS)
- ✅ Aprendizaje de conceptos como corrutinas y programación asíncrona
- ✅ Manejo robusto de errores y casos edge

### Dificultades Encontradas

1. **Cálculo de CPU Total**: Los valores varían según el SO y método de obtención
2. **Campo de Estado en UI**: Complejidad en posicionamiento y funcionalidad del dropdown
3. **Estados Multiplataforma**: Diferencias entre Windows (Running, Not Responding) y Linux (Sleeping, Zombie, Idle)
4. **Comandos del Sistema**: Adaptación de comandos nativos para cada SO

---

## 📚 Bibliografía

- **Innovamedia Consultores**. Gestión de procesos empresariales. [innovamediaconsultores.com](https://www.innovamediaconsultores.com/ayudas-kit-digital/gestion-de-procesos/)

- **Oracle**. Java SE Documentation - ProcessBuilder API. [docs.oracle.com/javase/8/docs/api/java/lang/ProcessBuilder.html](https://docs.oracle.com/javase/8/docs/api/java/lang/ProcessBuilder.html)

- **Oracle**. Java Management Extensions - OperatingSystemMXBean. [docs.oracle.com/javase/8/docs/api/java/lang/management/OperatingSystemMXBean.html](https://docs.oracle.com/javase/8/docs/api/java/lang/management/OperatingSystemMXBean.html)

- **Microsoft**. Tasklist command reference. [learn.microsoft.com/windows-commands/tasklist](https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/tasklist)

- **JetBrains**. Kotlin Multiplatform Wizard. [kmp.jetbrains.com](https://kmp.jetbrains.com)

---

## 📂 Repositorio

**Código fuente disponible en GitHub:**  
[https://github.com/raidingft/monitor-procesos-psp.git](https://github.com/raidingft/monitor-procesos-psp.git)

---
## 🤖 Herramientas de IA Utilizadas

### Claude (Anthropic)
Asistente principal utilizado para desarrollo de código y resolución de problemas técnicos.

**Ejemplos de uso:**
- Diferenciación conceptual entre "Estado" y "Tipo" de proceso
- Implementación de filtros dinámicos según SO

### ChatGPT (OpenAI)
Utilizado en fase inicial del proyecto.

**Ejemplos de uso:**
- Corrección de lectura de memoria en Windows (conversión KB a MB)
- Validación de selección antes de finalizar procesos

