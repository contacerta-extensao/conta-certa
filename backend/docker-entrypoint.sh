#!/bin/sh
set -eu

private_key=/app/keys/jwt-private.pem
public_key=/app/keys/jwt-public.pem

if [ ! -s "$private_key" ] || [ ! -s "$public_key" ]; then
	openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$private_key"
	openssl pkey -in "$private_key" -pubout -out "$public_key"
	chmod 600 "$private_key"
	chmod 644 "$public_key"
fi

exec java -jar /app/app.jar
