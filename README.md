# Neguentropía — backend

Base local en Java 25 y Spring Boot. Todas las herramientas, pruebas y builds se ejecutan en Docker; no se necesita Java ni Gradle instalado en el anfitrión.

## Desarrollo y pruebas

Ejecutar desde esta carpeta. Los comandos siguientes usan un anfitrión Linux:

```sh
docker build --target development -t neguentropia-backend-dev .

docker run --rm --user "$(id -u):$(id -g)" \
  --mount "type=bind,src=$PWD,dst=/workspace" \
  --workdir /workspace neguentropia-backend-dev \
  ./gradlew --no-daemon --console=plain test
```

La caché Gradle permanece en `.gradle/`, excluida de Git y del contexto Docker. El wrapper fija Gradle; el BOM de Spring Boot gestiona las versiones de las dependencias de aplicación y pruebas. La primera ejecución descarga dependencias y necesita red.

## Imagen ejecutable

```sh
docker build -t neguentropia-backend .
docker run --rm --name neguentropia-backend \
  -p 127.0.0.1:8080:8080 -e PORT=8080 neguentropia-backend
```

El build ejecuta pruebas y genera el JAR dentro del contenedor. La etapa final contiene el JRE y el JAR, sin Gradle ni fuentes, y ejecuta Java con UID/GID no privilegiados `10001:10001`. Las dos imágenes base están fijadas por digest.

La aplicación escucha en `0.0.0.0` dentro del contenedor. `PORT` cambia el puerto interno y vale `8080` por defecto; el mapeo `-p` debe coincidir. La publicación local se limita a la interfaz loopback del anfitrión.

## Contrato y alcance

`GET /api/health` representa la salud del proceso, no la disponibilidad de los datos. Mientras no exista un corpus revisado, la respuesta contractual es HTTP 200 con:

```json
{"status":"up","corpusState":"unavailable"}
```

El contrato común se conserva únicamente en `docs/openapi.yaml` del workspace padre durante esta fase; no se duplica aquí. Esta base no implementa aún catálogo, ranking, similitud, clustering, búsqueda ni modelos. No se habilitan proveedores externos ni se descarga el corpus.

## Contenido del repositorio

Versionar fuentes, pruebas, wrapper, configuración de build, Dockerfile y documentación técnica. No versionar credenciales, cachés, resultados de build ni instrucciones/notas privadas. `.env.example`, si se incorpora, debe contener únicamente valores ficticios. Los directorios de datos `/corpus/` y `/models/` están excluidos: los permisos de uso y publicación se comprueban antes de cualquier transferencia. Ignorar un archivo no elimina versiones anteriores ni sustituye la revisión de los archivos preparados.

No hay CI ni despliegue configurados. La creación del repositorio no implica publicación de código, configuración de Render ni autorización de gastos.
