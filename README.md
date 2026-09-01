# Atlas Semáforo Lite v0.26

Versión personal: motor económico compensado + memoria temporal de servicio.

## Novedad principal v0.26
- Sustituye el semáforo rígido por un score económico ponderado: 45% COP/km + 45% COP/h + 10% rating.
- Un indicador fuerte puede compensar parcialmente otro débil; ya no se rechaza automáticamente una oferta porque una sola métrica quede amarilla/roja.
- Umbrales del score: verde >=80, amarillo 60-79, rojo <60.
- Protección: si COP/km o COP/h cae bajo su piso duro, el resultado no puede ser verde; si ambas métricas quedan bajo sus mínimos, el resultado es rojo.
- Pisos configurables por el usuario: 1.200 COP/km y 22.000 COP/h por defecto.
- Overlay muestra score 0-100, subscore Km/Hora/Cliente y una explicación breve (p. ej. “hora compensa km”).
- Mantiene configuración local de v0.23 y memoria temporal de servicio de v0.24.

### Identidad visual Android
- Icono oficial: un semáforo literal rojo/amarillo/verde.
- Incluye icono launcher clásico y adaptive icon para Android 8+.
- El manifiesto referencia `@mipmap/ic_launcher` y `@mipmap/ic_launcher_round`.
- El activo se mantiene local dentro del APK; no agrega permisos ni red.


## Privacidad y límites
- Sin AccessibilityService.
- Sin clics, gestos ni aceptación/rechazo automático en Uber.
- Capturas, Bitmap y OCR efímeros.
- Solo campos estructurados del servicio activo permanecen temporalmente en RAM.
- Sin permiso INTERNET.
- Overlays informativos y no táctiles.

## Estado de verificación
Pruebas puras Kotlin y contratos locales v0.26: pruebas de icono y contratos locales ejecutadas. Compilación Android/APK/prueba física v0.26: NO ejecutadas en este runtime.
