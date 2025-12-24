# Revision 21/12/25

Falta añadir el símbolo "Ticker". Entiendo que es el logo de la empresa, no solo las iniciales
Precio de apertura del dia? No se ven las fechas.
Historial de precios
Comprobar si un evento de un sector afecta a todas las empresas de ese sector
Al vender unas acciones, se realiza la accion pero se muestra igual un mensaje de error.
En la sección de Transacciones, falta la opcion de exportar a CSV y la comision.
Tablas de cotizaciones???
Debe salir una notificacion general en el movil cuando salte una alerta?
Implementar Funcionalidades Avanzadas








Proyecto 2: Simulador de Mercado Bursátil en Tiempo Real 📈
Plataformas: Kotlin Multiplatform (Escritorio Windows + Android)

Descripción
Desarrolla una aplicación multiplataforma que simule un mercado bursátil con múltiples acciones que cambian de precio en tiempo real, permitiendo a los usuarios comprar/vender acciones, gestionar un portfolio, ver gráficos de evolución y recibir alertas de precios. El simulador debe ejecutar operaciones de mercado en paralelo usando hilos y corrutinas.

Funcionalidades obligatorias
Motor de simulación de mercado
Generación de precios en tiempo real:

15-20 acciones de empresas ficticias con precios iniciales
Cada acción tiene un hilo/corrutina que actualiza su precio cada 1-3 segundos
Cambios de precio aleatorios pero realistas (-5% a +5%)
Factores que influyen en el precio:
Tendencia general del mercado (alcista/bajista)
Noticias aleatorias que afectan a sectores
Volatilidad por acción (algunas más estables, otras más volátiles)
Horario de mercado: abre/cierra en horarios configurables
Información de acciones:

Nombre de la empresa
Símbolo (ticker)
Sector (Tecnología, Energía, Banca, Comercio, Salud)
Precio actual
Precio de apertura del día
Precio máximo/mínimo del día
Variación en € y en %
Volumen de transacciones
Historial de precios (últimos 100 valores)
Eventos de mercado:

Generación aleatoria de noticias que afectan al mercado:
"Tecnología en auge: +3%"
"Crisis energética: Energía -4%"
"Subida de tipos de interés: Banca +2%"
Los eventos afectan a todas las empresas de un sector simultáneamente
Mostrar eventos en un panel de noticias en tiempo real
Gestión de portfolio
Cuenta de usuario:

Dinero disponible inicial: 10.000€
Saldo actual
Valor total del portfolio (cash + acciones)
Beneficio/pérdida total (€ y %)
Historial de transacciones
Compra/venta de acciones:

Comprar acciones al precio de mercado actual
Vender acciones que se poseen
Validaciones:
No comprar sin dinero suficiente
No vender más acciones de las que se poseen
Comisión por transacción (0.5%)
Confirmación antes de ejecutar
Portfolio de acciones:

Lista de acciones que se poseen con:
Cantidad de acciones
Precio de compra medio
Precio actual de mercado
Valor total actual
Beneficio/pérdida (€ y %)
Actualización en tiempo real del valor del portfolio
Ordenar por beneficio, pérdida, valor, etc.
Historial de transacciones:

Registro de todas las compras/ventas realizadas
Fecha y hora
Tipo (compra/venta)
Acción
Cantidad
Precio por acción
Total
Comisión
Exportar a CSV
Análisis y visualización
Gráficos:

Gráfico de líneas con evolución de precio de cualquier acción
Gráfico de tarta con distribución del portfolio por sector
Gráfico de barras con beneficios/pérdidas por acción
Gráfico de evolución del valor total del portfolio
Actualizaciones en tiempo real
Tabla de cotizaciones:

Lista de todas las acciones disponibles
Ordenar por: nombre, precio, variación %, sector
Filtrar por sector
Código de colores:
Verde: precio subió
Rojo: precio bajó
Gris: sin cambios
Animación al cambiar precio
Alertas de precios:

Configurar alertas para acciones específicas
Tipos de alerta:
Precio sube por encima de X€
Precio baja por debajo de Y€
Variación supera Z%
Notificación visual y sonora cuando salte alerta
Historial de alertas activadas
Funcionalidades avanzadas
Estrategias automáticas (opcional):

Compra automática si precio baja X%
Venta automática si beneficio supera Y%
Stop-loss: venta automática si pérdida supera Z%
Estadísticas:

Mejor/peor transacción
Acción más rentable
Tasa de éxito (% transacciones con beneficio)
Rentabilidad media
Persistencia:

Guardar estado del portfolio en archivos JSON
Cargar portfolio al iniciar
Guardar historial de precios
Exportar/importar portfolio
Requisitos técnicos de concurrencia
Hilos y corrutinas:

Cada acción tiene una corrutina que actualiza su precio periódicamente
Thread pool para gestionar múltiples actualizaciones
Corrutina para generar eventos de mercado aleatorios
Flow para emitir cambios de precios
StateFlow para estado global del mercado
Dispatchers.Default para cálculos
Dispatchers.Main para UI
Sincronización:

ConcurrentHashMap para precios actuales de las acciones
Acceso thread-safe al portfolio del usuario
Locks para operaciones de compra/venta (evitar race conditions)
Evitar deadlocks al actualizar múltiples acciones
AtomicInteger para IDs de transacciones
Manejo de estado:

Cancelación correcta de todas las corrutinas al cerrar
Pausar/reanudar simulación
Acelerar tiempo de simulación (modo rápido)
Interfaz gráfica
Versión Escritorio (Compose for Desktop):

Ventana principal dividida en secciones:
Tabla de cotizaciones (izquierda)
Gráfico de acción seleccionada (centro)
Portfolio y noticias (derecha)
Barra superior con:
Saldo actual
Valor portfolio
Beneficio/pérdida
Estado del mercado (ABIERTO/CERRADO)
Ventanas secundarias para:
Comprar/vender acciones
Configurar alertas
Ver historial de transacciones
Estadísticas
Tema claro/oscuro
Versión Android (Jetpack Compose):

Pestañas:
Mercado (lista de acciones con precios)
Portfolio (mis acciones)
Gráficos
Alertas
FloatingActionButton para comprar/vender
Notificaciones para alertas de precios
Swipe para ver detalles de acción
Entregables específicos
Versión Windows:

Archivo .exe ejecutable
O instalador .msi
Versión Android:

Archivo .apk
Vídeo demostrativo:

6-10 minutos
Mostrar mercado en funcionamiento (precios cambiando)
Realizar compras y ventas
Configurar alertas y que salten
Ver gráficos y estadísticas
Demostrar en ambas plataformas
Código fuente:

Proyecto Kotlin Multiplatform organizado
Módulos: shared, desktop, android
