package com.example.recetasseguras.auth

import android.util.Log
import com.squareup.moshi.Moshi

object ApiErrorParser {
    private val moshi = Moshi.Builder().build()

    fun parse(json: String?): ApiError? {
        if (json.isNullOrBlank()) return null
        // Intento crear el adapter y parsear aquí dentro del método
        // para que cualquier ClassNotFound (adapter generado ausente) no ocurra en la
        // inicialización estática del objeto y pueda caer al fallback.
        try {
            val adapter = try {
                moshi.adapter(ApiError::class.java)
            } catch (t: Throwable) {
                Log.w("ApiErrorParser", "parse: cannot create ApiError adapter: ${t.message}")
                null
            }
            if (adapter != null) {
                try {
                    return adapter.fromJson(json)
                } catch (t: Throwable) {
                    Log.w("ApiErrorParser", "parse: adapter.fromJson failed, will try fallback. Cause= ${t.message}")
                }
            }
        } catch (t: Throwable) {
            Log.w("ApiErrorParser", "parse: unexpected error creating adapter: ${t.message}")
        }

        // Como fallback, intento parsear como Map y construir un ApiError parcial
        return try {
            val mapAdapter = moshi.adapter(Map::class.java)
            val map = mapAdapter.fromJson(json) as? Map<*, *> ?: return null

            val timestamp = map["timestamp"]?.toString()
            val status = when (val s = map["status"]) {
                is Double -> s.toInt()
                is Float -> s.toInt()
                is Int -> s
                is Long -> s.toInt()
                else -> null
            }
            val error = map["error"]?.toString()
            val code = map["code"]?.toString()
            val message = map["message"]?.toString()

            // errors puede ser una lista de mapas con field/message
            val rawErrors = map["errors"]
            val errors = if (rawErrors is List<*>) {
                rawErrors.mapNotNull { item ->
                    if (item is Map<*, *>) {
                        val field = item["field"]?.toString()
                        val msg = item["message"]?.toString()
                        ValidationError(field, msg)
                    } else null
                }
            } else null

            ApiError(timestamp, status, error, code, message, errors)
        } catch (t: Throwable) {
            Log.e("ApiErrorParser", "parse fallback failed: ${t.message}")
            null
        }
    }

    // Devuelvo un mensaje amigable para mostrar en el frontend según el código de error
    fun userFriendlyMessage(apiError: ApiError?): String? {
        if (apiError == null) return null
        // Uso la propiedad `code` como prioridad si está presente;
        // si no está, uso `error` (compatibilidad con respuestas antiguas).
        val code = apiError.code ?: apiError.error
        return when (code) {
            "USER_NOT_FOUND" -> "Usuario no encontrado. Verifica tu correo y vuelve a intentarlo."
            "USERNAME_ALREADY_EXISTS" -> "El nombre de usuario ya está en uso. Elige otro nombre."
            "INVALID_PASSWORD" -> "Contraseña incorrecta. Verifica e intenta de nuevo."
            "PASSWORD_ERROR" -> "Error con la contraseña. Verifica que tenga al menos 8 caracteres, incluyendo letras y números."
            "EMAIL_ALREADY_EXISTS" -> "El correo ya está registrado. Usa otro correo o inicia sesión."
            "VALIDATION_FAILED" -> "Hay errores en los datos enviados. Revisa los campos marcados y corrígelos."
            "MALFORMED_JSON" -> "El formato JSON de la petición es inválido. Revisa la estructura enviada."
            "ACCESS_DENIED" -> "Acceso denegado. No tienes permisos para esta operación."
            "AUTH_ERROR" -> "Error de autenticación. Verifica tus credenciales e intenta de nuevo."
            "CONSTRAINT_VIOLATION" -> "La solicitud viola una restricción del servidor. Revisa los datos y vuelve a intentarlo."
            "RUNTIME_ERROR" -> apiError.message ?: "Ocurrió un error. Intenta de nuevo más tarde."
            "INTERNAL_ERROR" -> "Error interno del servidor. Intenta más tarde."
            "OLD_PASSWORD_MISMATCH" -> "La contraseña actual no coincide. Verifica e intenta de nuevo."
            "PASSWORD_CONFIRMATION_MISMATCH" -> "Las contraseñas nuevas no coinciden. Verifica y vuelve a intentarlo."
            "NOT_AUTHENTICATED" -> "No estás autenticado. Por favor, inicia sesión."
            "UNAUTHENTICATED" -> "No estás autenticado. Por favor, inicia sesión."
            "MISSING_FIELDS" -> "Faltan campos obligatorios en la petición. Completa todos los campos requeridos."
            else -> apiError.message // fallback: usar mensaje del servidor si existe
        }
    }

    // Traduce mensajes de validación por campo comunes (ej. mensajes generados por validadores del servidor)
    // Devuelve null si no hay una traducción específica para el mensaje recibido.
    fun translateValidationMessage(field: String?, rawMessage: String?): String? {
        if (rawMessage.isNullOrBlank()) return null
        val msg = rawMessage.trim()
        // Casos comunes en inglés que queremos mostrar en español
        return when {
            // Mensaje típico de validación de correo (Hibernate/JSR-303)
            msg.contains("well-formed email address", ignoreCase = true) -> "El correo debe tener un formato válido."
            msg.contains("must be a well-formed email address", ignoreCase = true) -> "El correo debe tener un formato válido."
            // Mensaje común sobre longitud mínima de contraseña
            msg.contains("size must be between", ignoreCase = true) && field.equals("password", ignoreCase = true) -> "La contraseña no cumple los requisitos de longitud."
            // Mensaje genérico de campo requerido
            msg.contains("must not be null", ignoreCase = true) || msg.contains("may not be null", ignoreCase = true) -> "Este campo es obligatorio."
            else -> null
        }
    }
}
