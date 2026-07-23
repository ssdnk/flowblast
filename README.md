# FlowerBlast - Инструкция по запуску

## Системные требования

- **Android SDK**: 34+
- **JDK**: 17+
- **Gradle**: 8.7+ (включён в проект через gradlew)

## Запуск через Android Studio

1. Откройте Android Studio
2. File → Open → выберите папку проекта
3. Дождитесь синхронизации Gradle
4. Выберите конфигурацию запуска (например, app) и нажмите Run (Shift+F10)

## Запуск через командную строку

### Linux/macOS

```bash
./gradlew assembleDebug
```

### Windows

```cmd
gradlew.bat assembleDebug
```

 APK будет находиться в: `app/build/outputs/apk/debug/app-debug.apk`

## Дополнительные команды

| Команда | Описание |
|---------|----------|
| `./gradlew clean` | Очистка сборки |
| `./gradlew build` | Полная сборка |
| `./gradlew assembleDebug` | Сборка debug-версии |
| `./gradlew assembleRelease` | Сборка release-версии |
| `./gradlew lint` | Проверка кода |