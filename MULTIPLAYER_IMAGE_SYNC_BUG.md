# Bug: Multiplayer Mode - Camera/Gallery Images Not Syncing to Player B

## Problem Summary

En modo multijugador, cuando el Jugador A selecciona una imagen desde la **cámara o galería**, el Jugador B **NO recibe la imagen** y se queda esperando indefinidamente.

Solo funcionan las imágenes de Unsplash (URLs HTTP remotas).

## Root Cause

### El Problema Técnico

**Archivo afectado**: [android/app/src/main/java/com/example/drawmaster/presentation/viewmodels/ConfirmImageViewModel.kt](android/app/src/main/java/com/example/drawmaster/presentation/viewmodels/ConfirmImageViewModel.kt#L54)

En `ConfirmImageViewModel.onStartDrawingClicked()`:

```kotlin
fun onStartDrawingClicked(navController: NavHostController) {
    val finalUri = _uiState.value.imageUri?.toString()  // ← Puede ser content:// o file://
    
    if (finalUri != null && gameId != null) {
        val remoteToUse = when {
            !remoteUrlString.isNullOrBlank() -> remoteUrlString
            finalUri.startsWith("http://") || finalUri.startsWith("https://") -> finalUri
            else -> null  // ← Aquí entra para imágenes de cámara/galería
        }
        
        // Si no hay URL remota, actualiza RTDB con URI local
        val updates = mapOf<String, Any>(
            "state" to "started",
            "imageUri" to finalUri,  // ← AQUÍ ESTÁ EL PROBLEMA
            "startedAt" to System.currentTimeMillis()
        )
        database.reference.child("games").child(gameId).updateChildrenAwait(updates)
    }
}
```

### Por Qué Falla

1. **Imágenes de cámara/galería generan URIs locales**:
   - `content://com.android.providers.media.documents/document/image%3A123`
   - `file:///storage/emulated/0/DCIM/Camera/IMG_20240101_120000.jpg`

2. **Estas URIs son específicas del dispositivo**:
   - Solo son válidas en el teléfono del Jugador A
   - El Jugador B intenta acceder a esta URI desde su dispositivo y **NO FUNCIONA**
   - El ContentProvider/archivo no existe en el dispositivo de B

3. **En `MultiplayerWaitingScreen.kt`**, el Jugador B espera a que `imageUri` esté disponible:
   ```kotlin
   val imageUri = gameData?.get("imageUri") as? String
   if (state == "started" && !imageUri.isNullOrBlank()) {
       navController.navigate("game_screen/$encodedUri/$encodedGameId")  // Navega con URI inválida
   }
   ```

4. **En `GameScreen`**, cuando intenta cargar la imagen, falla silenciosamente**:
   - El `AsyncImagePainter` no puede resolver la URI local
   - El jugador ve una imagen en blanco o un placeholder

## Solution: Upload Local Images to Cloud Storage

### Enfoque Recomendado

**Usar Firebase Cloud Storage** para cargar imágenes de cámara/galería antes de sincronizar con Jugador B.

#### Diagrama del Flujo (Corregido)

```
Jugador A - SelectImage
    ↓
Selecciona de cámara/galería → URI local (content://)
    ↓
ConfirmImageScreen
    ↓
onStartDrawingClicked():
    ├─ Si es URL HTTP (Unsplash) → Usa directamente
    └─ Si es local (cámara/galería):
        ├─ Sube a Firebase Cloud Storage
        ├─ Obtiene URL descargable (HTTPS)
        ├─ Guarda HTTPS en RTDB
        └─ Jugador B accede vía HTTPS ✅
    ↓
Actualiza RTDB con imageUri = "https://storage.googleapis.com/..."
    ↓
Jugador B recibe URL HTTPS en MultiplayerWaitingScreen
    ↓
GameScreen carga imagen con AsyncImagePainter ✅
```

## Implementation Plan

### 1. Setup Firebase Cloud Storage

**En `app/build.gradle.kts`:**
```kotlin
dependencies {
    // Ya instalado:
    implementation("com.google.firebase:firebase-storage-ktx")
}
```

### 2. Create Storage Upload Service

**Archivo**: `android/app/src/main/java/com/example/drawmaster/data/storage/FirebaseStorageService.kt`

```kotlin
package com.example.drawmaster.data.storage

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseStorageService(private val context: Context) {
    
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Uploads a local image (from camera/gallery) to Cloud Storage
     * Returns the public HTTPS download URL
     */
    suspend fun uploadLocalImage(imageUri: Uri): String? {
        try {
            val userId = auth.currentUser?.uid ?: return null
            val fileName = "images/${userId}/${UUID.randomUUID()}.jpg"
            
            val ref = storage.reference.child(fileName)
            
            // Upload file
            context.contentResolver.openInputStream(imageUri)?.use { stream ->
                ref.putStream(stream).await()
            }
            
            // Get public download URL
            val downloadUrl = ref.downloadUrl.await()
            return downloadUrl.toString()
        } catch (e: Exception) {
            android.util.Log.w("StorageService", "Upload failed", e)
            return null
        }
    }
}
```

### 3. Update ConfirmImageViewModel

**Archivo**: [android/app/src/main/java/com/example/drawmaster/presentation/viewmodels/ConfirmImageViewModel.kt](android/app/src/main/java/com/example/drawmaster/presentation/viewmodels/ConfirmImageViewModel.kt)

```kotlin
import com.example.drawmaster.data.storage.FirebaseStorageService

class ConfirmImageViewModel(
    imageUriString: String?,
    private val remoteUrlString: String? = null,
    private val gameId: String? = null
) : ViewModel() {

    private val storageService = FirebaseStorageService(/* pass context */)

    fun onStartDrawingClicked(navController: NavHostController) {
        val finalUri = _uiState.value.imageUri?.toString()

        if (finalUri != null) {
            val encodedUri = Uri.encode(finalUri)
            
            if (gameId != null) {
                val imageUriToStore = when {
                    // Caso 1: Unsplash remoto
                    !remoteUrlString.isNullOrBlank() -> remoteUrlString
                    // Caso 2: URL HTTP (ya remota)
                    finalUri.startsWith("http://") || finalUri.startsWith("https://") -> finalUri
                    // Caso 3: Local (cámara/galería) → SUBIR A STORAGE
                    else -> {
                        viewModelScope.launch {
                            try {
                                val cloudUrl = storageService.uploadLocalImage(Uri.parse(finalUri))
                                if (cloudUrl != null) {
                                    updateGameWithImage(gameId, cloudUrl, navController, encodedUri)
                                } else {
                                    // Fallback si upload falla
                                    updateGameWithImage(gameId, finalUri, navController, encodedUri)
                                }
                            } catch (e: Exception) {
                                android.util.Log.w("ConfirmVM", "Upload error", e)
                                updateGameWithImage(gameId, finalUri, navController, encodedUri)
                            }
                        }
                        return  // Early exit, update ocurre en launch
                    }
                }
                
                // Imágenes remotas → actualizar inmediatamente
                if (imageUriToStore != null) {
                    updateGameWithImage(gameId, imageUriToStore, navController, encodedUri)
                }
            } else {
                navController.navigate("game_screen/$encodedUri")
            }
        } else {
            navController.popBackStack()
        }
    }
    
    private fun updateGameWithImage(
        gameId: String,
        imageUri: String,
        navController: NavHostController,
        encodedUri: String
    ) {
        viewModelScope.launch {
            try {
                val database = com.example.drawmaster.util.getFirebaseDatabase()
                val updates = mapOf<String, Any>(
                    "state" to "started",
                    "imageUri" to imageUri,
                    "startedAt" to System.currentTimeMillis()
                )
                withContext(Dispatchers.IO) {
                    database.reference.child("games").child(gameId).updateChildrenAwait(updates)
                }
            } catch (e: Exception) {
                android.util.Log.w("ConfirmVM", "DB update failed", e)
            }
            navController.navigate("game_screen/$encodedUri/$gameId")
        }
    }
}
```

### 4. Firebase Cloud Storage Rules

**Archivo**: `api-rest/firebase/storage.rules` (crear si no existe)

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /images/{userId}/{filename} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId && 
                      request.resource.size < 50 * 1024 * 1024; // 50MB max
      allow delete: if request.auth.uid == userId;
    }
  }
}
```

Publicar en Firebase:
```bash
firebase deploy --only storage
```

## Alternative Solutions (Si Cloud Storage No es Viable)

### Opción A: Backend Processing (Más Robusta)
- Player A sube imagen al backend
- Backend convierte a URL descargable  
- Backend escribe URL en RTDB
- Player B descarga de URL centralizada

### Opción B: Direct Base64 Encoding (No Recomendado)
- Convertir bitmap a Base64
- Escribir en RTDB
- **Problema**: RTDB tiene límite de 16MB por registro
- **Lento**: Codificación/decodificación en cada cambio

## Testing Checklist

- [ ] Player A selecciona imagen de **cámara**
  - [ ] Se sube a Cloud Storage
  - [ ] Se obtiene URL HTTPS
  - [ ] URL aparece en RTDB
  - [ ] Player B recibe URL
  - [ ] Player B ve imagen en GameScreen
  
- [ ] Player A selecciona imagen de **galería**
  - [ ] (mismo flujo que cámara)
  
- [ ] Player A selecciona imagen de **Unsplash** (debe seguir funcionando)
  - [ ] Se usa directamente, sin upload
  - [ ] URL aparece en RTDB
  - [ ] Player B ve imagen
  
- [ ] Player A selecciona **sample image** (debe seguir funcionando)
  - [ ] Si se descargó de Unsplash: usar URL remota
  - [ ] Si es recurso local: subir a Storage

## Migration Path

1. **Phase 1**: Implementar `FirebaseStorageService`
2. **Phase 2**: Actualizar `ConfirmImageViewModel`
3. **Phase 3**: Actualizar reglas de Firebase Storage
4. **Phase 4**: Testing multijugador completo
5. **Phase 5**: Monitoreo de errores en Crashlytics

## References

- Firebase Storage: https://firebase.google.com/docs/storage
- Content Providers: https://developer.android.com/guide/topics/providers/content-providers
- URI Types: https://developer.android.com/reference/android/net/Uri

---

**Status**: 🔴 Critical Bug - Multiplayer mode broken for camera/gallery images
**Priority**: High
**Assignee**: [Your Name]
**ETA**: 1-2 sprints (after implementation)
