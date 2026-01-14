# 📈 BolsaCotarelo – Simulador de Mercado Bursátil en Tiempo Real

**Proyecto 2 – Kotlin Multiplatform**
Plataformas: **Windows (Compose Desktop)** y **Android (APK)**

---

## 📌 Descripción del proyecto

**BolsaCotarelo** es una aplicación multiplataforma desarrollada en **Kotlin Multiplatform** que simula un **mercado bursátil en tiempo real**. El usuario puede interactuar con distintas acciones ficticias cuyos precios evolucionan dinámicamente, permitiendo realizar operaciones de compra y venta, gestionar un portfolio y analizar resultados mediante gráficos y estadísticas.

El objetivo principal del proyecto es aplicar conceptos de **concurrencia**, **programación reactiva** y **desarrollo multiplataforma**, utilizando corrutinas, Flows y una interfaz moderna basada en Compose.

---

## ⚙️ Funcionalidades principales

* 📊 **Mercado en tiempo real**

    * Acciones con precios que se actualizan automáticamente
    * Variaciones realistas de precio
    * Tendencias generales y eventos de mercado

* 💼 **Gestión de portfolio**

    * Dinero inicial configurable
    * Compra y venta de acciones con validaciones
    * Cálculo automático de beneficios y pérdidas
    * Historial de transacciones

* 📈 **Análisis y visualización**

    * Gráficos de evolución de precios
    * Gráficos de distribución del portfolio
    * Estadísticas generales del rendimiento

* 🚨 **Alertas**

    * Alertas configurables por subida, bajada o variación de precio
    * Notificaciones visuales dentro de la aplicación

* 💾 **Persistencia**

    * Guardado del estado del portfolio
    * Exportación de datos a CSV

---

## 🖥️ Ejecución en Windows (versión escritorio)

### Requisitos

* Sistema operativo **Windows 10 o superior**
* No es necesario tener Java instalado

### Pasos para ejecutar

1. Acceder a la carpeta:

   ```
   ejecutables/windows
   ```
2. Descomprimir el archivo:

   ```
   BolsaCotarelo-1.0-windows-portable.zip
   ```
3. Ejecutar el archivo `.exe` incluido en la carpeta
4. La aplicación se inicia directamente (no requiere instalación)

---

## 📱 Ejecución en Android

### Requisitos

* Dispositivo Android o emulador
* Permitir **instalar aplicaciones desde orígenes desconocidos**

### Pasos para ejecutar

1. Acceder a la carpeta:

   ```
   ejecutables/android
   ```
2. Copiar el archivo:

   ```
   BolsaCotarelo-1.0-release.apk
   ```

   al dispositivo Android
3. Abrir el APK e instalar la aplicación
4. Ejecutar la app desde el menú de aplicaciones

---

## 🛠️ Tecnologías utilizadas

* **Kotlin Multiplatform**
* **Jetpack Compose / Compose Desktop**
* **Corrutinas y Flow**
* **StateFlow para gestión de estado**
* **Gradle**
* **Persistencia en JSON**

---

## 📂 Estructura de entrega

```
ejecutables/
 ├─ android/
 │   └─ BolsaCotarelo-1.0-release.apk
 └─ windows/
     └─ BolsaCotarelo-1.0-windows-portable.zip
```

---

## 👨‍🎓 Contexto académico

Proyecto desarrollado como parte de la asignatura **Programación de Servicios y Procesos (PSP)**, aplicando conceptos de:

* Concurrencia
* Programación reactiva
* Multiplataforma
* Arquitectura limpia y mantenible
