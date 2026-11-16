# Inventario D-ACERO

Aplicación de escritorio para gestionar productos, existencias y movimientos de inventario por sede. Está desarrollada con Java 17, Swing, Maven y MySQL 8.

## Requisitos

- JDK 17 o compatible.
- Apache Maven 3.9 o compatible.
- MySQL 8 en ejecución.
- NetBeans 17 o compatible (opcional).

## Preparar MySQL

1. Ejecuta `src/main/resources/sql/01_modelo_fisico.sql` en DBeaver para crear la base de datos `dacero` y sus tablas, vistas, procedimientos y triggers. El script usa `$$` como delimitador.
2. Opcionalmente, ejecuta `src/main/resources/sql/02_datos_demo.sql` con el delimitador `;` para cargar datos de ejemplo.

## Configurar la conexión

La app lee `db.local.properties` desde la carpeta del proyecto. Copia `db.local.properties.example` con ese nombre y configura los valores locales. Ese archivo está excluido de Git para que la contraseña de MySQL no se publique. Las variables de entorno `DACERO_DB_URL`, `DACERO_DB_USER` y `DACERO_DB_PASSWORD`, cuando están definidas, tienen prioridad sobre el archivo.

No guardes contraseñas en `Db.java` ni subas `db.local.properties` al repositorio.

## Iniciar

Abre la carpeta del proyecto desde **File > Open Project** en NetBeans, o ejecútalo desde una terminal:

```bash
mvn clean package
mvn exec:java
```

En el primer inicio, si la tabla `usuario_app` está vacía, la aplicación solicitará crear la cuenta administradora. El usuario y la contraseña de la aplicación son distintos a las credenciales de MySQL. Las contraseñas de la aplicación se almacenan con PBKDF2 y deben tener al menos 8 caracteres.

## Funcionalidades

- Panel con indicadores de productos activos, existencias bajo mínimo y movimientos del mes.
- Gestión de productos, categorías, unidades, sedes y stock mínimo.
- Registro de entradas, salidas y ajustes de inventario.
- Consultas y exportación a PDF de existencias, stock bajo e historial de movimientos.

Los movimientos históricos se conservan; los productos se pueden desactivar en lugar de eliminarlos.
