# 🥗 Recetas Seguras

Aplicación móvil Android para personas con condiciones alimentarias especiales (alergias, intolerancias, restricciones dietéticas). La app ayuda a los usuarios a descubrir alimentos y recetas seguras personalizadas según sus necesidades específicas.

## 📱 Características

- **Autenticación Segura**: Sistema completo de registro, login y gestión de sesión con tokens JWT
- **Gestión de Condiciones**: Los usuarios pueden seleccionar y guardar sus condiciones alimentarias
- **Sugerencias Personalizadas**: 
  - Lista de alimentos seguros según las condiciones del usuario
  - Recetas adaptadas y filtradas
- **Detalles de Recetas**: Visualización completa con ingredientes, cantidades y pasos de preparación
- **Perfil de Usuario**: Gestión de cuenta y cambio de contraseña

## 🛠️ Tecnologías

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose con Material 3
- **Arquitectura**: MVVM (Model-View-ViewModel)
- **Navegación**: Navigation Suite Scaffold (adaptable a tablets)
- **Red**: Retrofit + OkHttp + Moshi
- **Autenticación**: JWT con refresh token automático
- **Concurrencia**: Kotlin Coroutines + Flow
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 36

## 📋 Requisitos Previos

- Android Studio Hedgehog o superior
- JDK 17 o superior
- Dispositivo/emulador Android con API 29+
- Backend/API corriendo

## 🚀 Instalación

1. **Clonar el repositorio**
   ```powershell
   git clone https://github.com/xxx/recetas-seguras-app.git
   cd recetas-seguras-app
   ```

2. **Abrir en Android Studio**
   - File → Open → Seleccionar la carpeta del proyecto

3. **Configurar URL del backend** (opcional)
   
   Editar `app/build.gradle.kts`:
   ```kotlin
   buildConfigField("String", "API_BASE_URL", "\"http://tu-backend-url:puerto\"")
   ```

4. **Sincronizar dependencias**
   - Android Studio sincronizará automáticamente
   - O ejecutar: `.\gradlew build`

5. **Ejecutar la app**
   - Conectar dispositivo o iniciar emulador
   - Run → Run 'app' (Shift + F10)

## 📂 Estructura del Proyecto

```
app/src/main/java/com/example/recetasseguras/
├── MainActivity.kt              # Actividad principal y navegación
├── auth/
│   ├── ApiModels.kt            # DTOs para API
│   ├── AuthApiService.kt       # Definición de endpoints
│   ├── AuthManager.kt          # Gestión de tokens JWT
│   ├── AuthRepository.kt       # Capa de datos
│   └── AuthViewModel.kt        # Lógica de negocio y estado
├── network/
│   └── NetworkModule.kt        # Configuración Retrofit/OkHttp
└── ui/
    ├── auth/
    │   ├── LoginScreen.kt      # Pantalla de login
    │   └── RegisterScreen.kt   # Pantalla de registro
    ├── ConditionSelectionScreen.kt  # Selección de condiciones
    ├── HomeScreen.kt           # Sugerencias (alimentos/recetas)
    ├── ProfileScreen.kt        # Perfil de usuario
    ├── RecipeDetailScreen.kt   # Detalles de receta
    └── theme/
        └── Theme.kt            # Material 3 theming
```

## 🔑 Características Técnicas Destacadas

### Refresh Automático de Tokens
El `NetworkModule` implementa un sistema robusto de refresh de tokens:
- Detecta respuestas 401 (no autorizado)
- Refresca tokens automáticamente en segundo plano
- Sistema "single-flight" para evitar múltiples refreshes simultáneos
- Limpieza automática de sesión si el refresh falla

### Carga Paralela
Utilizamos `async/await` para optimizar rendimiento:
```kotlin
val foodsDeferred = async { repo.getSafeFoods(userId) }
val recipesDeferred = async { repo.getSafeRecipes(userId) }
val foods = foodsDeferred.await()
val recipes = recipesDeferred.await()
```

### UI Adaptable
- Navigation Suite Scaffold se adapta automáticamente a diferentes tamaños de pantalla
- Bottom navigation en móviles
- Rail navigation en tablets
- Drawer navigation en pantallas grandes

## 🎨 Diseño

La app utiliza Material 3 Design con:
- Cards con bordes outline para mejor contraste
- Iconos Material Design
- Tipografía y colores consistentes
- Modo claro

## 🔐 Seguridad

- Tokens JWT almacenados en SharedPreferences encriptadas
- Refresh token con rotación automática
- Validación de campos en cliente y servidor
- Timeout de sesión configurable

## 📡 API Backend

La app espera un backend REST con los siguientes endpoints:

```
POST   /api/auth/register        # Registro de usuario
POST   /api/auth/login           # Login
POST   /api/auth/refresh         # Refresh token
POST   /api/auth/logout          # Logout
GET    /api/users/me             # Obtener usuario actual
POST   /api/users/{id}/password  # Cambiar contraseña

GET    /api/conditions           # Listar todas las condiciones
GET    /api/users/{id}/conditions          # Condiciones del usuario
PUT    /api/users/{id}/conditions          # Actualizar condiciones

GET    /api/users/{id}/safe-foods          # Alimentos seguros
GET    /api/users/{id}/safe-recipes        # Recetas seguras
```

## 🐛 Debugging

Para ver logs de red y autenticación:
```powershell
adb logcat -s NetworkModule AuthViewModel MainActivity
```

## 📝 Licencia

Este proyecto es parte de un trabajo académico.

## 🙏 Agradecimientos

- Material 3 Design System
- Jetpack Compose community
- Square (Retrofit, OkHttp, Moshi)
