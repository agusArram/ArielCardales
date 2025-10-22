#!/usr/bin/env python3
"""
Script para firmar el archivo licencias.json

Este script calcula el hash SHA-256 del JSON y lo agrega al campo "firma"
para verificar la integridad del archivo y evitar manipulaciones.

IMPORTANTE: La clave secreta debe coincidir con LicenciaConfig.SECRET_KEY
"""

import json
import hashlib

# ⚠️ CAMBIAR ESTO SI MODIFICASTE LicenciaConfig.SECRET_KEY
SECRET_KEY = "ArielCardales2025_SecretKey_ChangeThis!"

def calcular_firma(datos_json):
    """
    Calcula la firma SHA-256 del JSON sin el campo 'firma'
    """
    # Crear copia sin el campo firma
    datos = json.loads(datos_json)
    if "firma" in datos:
        del datos["firma"]

    # Convertir de vuelta a string JSON (sin espacios para consistencia)
    json_sin_firma = json.dumps(datos, separators=(',', ':'), sort_keys=True)

    # Concatenar con clave secreta
    contenido_firmado = json_sin_firma + SECRET_KEY

    # Calcular hash SHA-256
    hash_obj = hashlib.sha256(contenido_firmado.encode('utf-8'))
    firma = hash_obj.hexdigest()

    return firma

def firmar_archivo(ruta_archivo):
    """
    Lee el archivo JSON, calcula la firma y la actualiza
    """
    # Leer archivo
    with open(ruta_archivo, 'r', encoding='utf-8') as f:
        contenido = f.read()

    # Calcular firma
    firma = calcular_firma(contenido)

    # Parsear JSON
    datos = json.loads(contenido)

    # Agregar firma
    datos["firma"] = firma

    # Guardar archivo con firma
    with open(ruta_archivo, 'w', encoding='utf-8') as f:
        json.dump(datos, f, indent=2, ensure_ascii=False)

    print(f"[OK] Archivo firmado exitosamente")
    print(f"Firma: {firma}")
    print(f"Archivo: {ruta_archivo}")

    return firma

if __name__ == "__main__":
    import sys

    if len(sys.argv) > 1:
        archivo = sys.argv[1]
    else:
        archivo = "licencias.json"

    try:
        firma = firmar_archivo(archivo)
        print("\n[OK] Proceso completado")
        print("\nProximos pasos:")
        print("1. Revisar el archivo licencias.json")
        print("2. Hacer commit y push a GitHub")
        print("3. Copiar la URL RAW del archivo desde GitHub")
        print("4. Actualizar LicenciaConfig.LICENCIAS_JSON_URL con esa URL")

    except Exception as e:
        print(f"[ERROR] {e}")
        sys.exit(1)
