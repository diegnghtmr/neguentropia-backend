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

`GET /api/health` representa la salud del proceso, no la disponibilidad de los datos. Sin un manifiesto configurado y validado, la respuesta contractual es HTTP 200 con:

```json
{"status":"up","corpusState":"unavailable"}
```

El contrato HTTP se define en una especificación OpenAPI mantenida fuera de este repositorio. Se implementan el catálogo `GET /api/papers`, el detalle `GET /api/papers/{id}` y el ranking `POST /api/ranking`. El ranking recibe un objeto JSON vacío, ordena la misma lista de frecuencias de los 20 abstracts con Counting, Radix y Bucket, comprueba que las tres listas completas coinciden y devuelve el top 15 y el tiempo de cada método. Las explicaciones están en [docs/ranking](docs/ranking). No se implementan similitud, clustering, búsqueda ni modelos: sus capacidades declaran `available: false`, `blockedReason: NOT_IMPLEMENTED`. No se habilitan proveedores externos ni se descarga el corpus.

## Corpus externo y preparación local

| Configuración | Comportamiento |
| --- | --- |
| `CORPUS_MANIFEST` | Ruta interna del único archivo de manifiesto privado; vacía por defecto. Montarlo externamente de solo lectura, nunca copiarlo al repositorio, recursos ni imagen. |
| `CORS_ALLOWED_ORIGINS` | Lista separada por comas de orígenes locales explícitos, por ejemplo `http://127.0.0.1:4174`. Vacía por defecto; no admite comodines, credenciales, rutas ni hosts externos. Una entrada inválida cierra la lista completa. |

El runtime local debe usar el UID/GID del propietario si el manifiesto conserva permisos `0600`; no ampliar permisos para resolver su lectura. Las rutas `sourceFile` del manifiesto son referencias de procedencia: **no se abren, resuelven ni interpretan como rutas de entrada**. Tampoco se lee ningún PDF desde la aplicación ni se aceptan rutas de corpus mediante HTTP.

La carga ocurre una vez al arrancar. Se exige el formato privado `schemaVersion: 1`, revisión `agent-reviewed`, `humanApproved: false` y exactamente veinte registros íntegros en orden `paper-01`…`paper-20`. Se comprueban campos/tipos, Unicode bien formado, utilidad del abstract, unicidad de fuentes/textos/IDs, SHA-256 del original UTF-8 y versión `abstracts-sha256-` calculada sobre las tuplas JSON ordenadas `[id,title,authors,originalAbstract,sourceSha256]`. El hash fuente se valida estructuralmente y por unicidad; comprobar sus bytes corresponde a la curación privada, no al runtime que no abre PDFs.

Una configuración ausente, ilegible, inválida o parcial deja el corpus completo no disponible: catálogo y detalle devuelven HTTP503 `CORPUS_UNAVAILABLE`, incluso para un ID desconocido. Con el corpus listo, health devuelve `up`/`ready`, el catálogo incluye veinte resúmenes y capacidades, el detalle conserva el original exacto y su hash, y un ID ajeno devuelve HTTP404 `PAPER_NOT_FOUND`. Las respuestas no incluyen referencias privadas, flags de revisión ni detalles de fallos de carga.

`TextPreparation` calcula en Java `baseText` NFC y tokens NFC → minúsculas `Locale.ROOT` → NFC; conserva secuencias de letras/marcas/dígitos Unicode solo si contienen alguna letra. No elimina stopwords ni aplica stemming, traducción o lematización. La preparación `unicode-nfc-root-v1`, los originales y las frecuencias permanecen en RAM: se recorren exclusivamente abstracts en orden del manifiesto, manteniendo primera aparición de términos mediante `LinkedHashMap`. No hay TF-IDF, ordenamiento de frecuencias ni nuevas rutas de análisis.

Las pruebas usan manifiestos de veinte registros **sintéticos** en estado temporal aislado; no acreditan la integración con los datos reales. Incluyen validación fail-closed, preparación Unicode, HTTP, CORS permitido/rechazado/cerrado por defecto y regresión del health sin corpus.

## Ranking measurement

The [full-corpus measurement report](docs/ranking/measurements.md) records the
three independent JVM runs, rotated method order, 20 warmups and 100 measured
runs per method/process, exact complete-output agreement, real top 15 and timing
limitations. `RankingBenchmark` is a standalone offline entry point; it does not
start the HTTP server. Its manifest must remain externally mounted read-only.

## Contenido del repositorio

Versionar fuentes, pruebas, wrapper, configuración de build, Dockerfile y documentación técnica. No versionar credenciales, cachés, resultados de build ni instrucciones/notas privadas. `.env.example`, si se incorpora, debe contener únicamente valores ficticios. Los directorios de datos `/corpus/` y `/models/` están excluidos: los permisos de uso y publicación se comprueban antes de cualquier transferencia. Ignorar un archivo no elimina versiones anteriores ni sustituye la revisión de los archivos preparados.

No hay CI ni despliegue configurados.
