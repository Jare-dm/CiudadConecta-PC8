#!/bin/sh
set -eu

CA_FILE="/etc/secrets/ca.pem"
TRUSTSTORE_FILE="/tmp/aiven-truststore.p12"

: "${DB_TRUSTSTORE_PASSWORD:?Falta configurar DB_TRUSTSTORE_PASSWORD en Render}"

if [ ! -r "$CA_FILE" ]; then
    echo "ERROR: No se puede leer el certificado $CA_FILE" >&2
    exit 1
fi

umask 077
rm -f "$TRUSTSTORE_FILE"

keytool -importcert \
    -noprompt \
    -trustcacerts \
    -alias aiven-project-ca \
    -file "$CA_FILE" \
    -keystore "$TRUSTSTORE_FILE" \
    -storetype PKCS12 \
    -storepass "$DB_TRUSTSTORE_PASSWORD"

export DB_TRUSTSTORE_URL="file:${TRUSTSTORE_FILE}"

echo "Certificado Aiven preparado correctamente."
echo "Iniciando CiudadConecta PC8..."

exec java -jar /app/app.jar
