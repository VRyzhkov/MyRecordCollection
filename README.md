# MyRecordCollection

Нативное Android-приложение для просмотра коллекции понравившихся альбомов из Яндекс Музыки.

Для пользования приложением у вас должен быть доступ к яндекс музыке

Альбомы отображаются в единой интерактивной карусели. Центральная обложка увеличивается, а в верхней части экрана показываются исполнитель и название выбранного альбома. Нажатие на центральный элемент открывает страницу альбома в официальном приложении Яндекс Музыки или в браузере.

> Интеграция использует неофициальный API Яндекс Музыки. Его формат и доступность могут измениться без предупреждения.

## Возможности

- вход через сайт Яндекса по OAuth Device Flow без ручного копирования токена;
- безопасное хранение access- и refresh-токенов с помощью Android Keystore;
- единая карусель всех альбомов с поддержкой жестов и инерции;
- загрузка обложек альбомов;
- локальное хранение коллекции в Room для работы без сети;
- обновление коллекции жестом вниз;
- автоматическое удаление локальных данных и обложек исключённых альбомов;
- системная, светлая и тёмная темы;
- переход к альбому в Яндекс Музыке;
- автоматическая сборка подписанного APK при публикации Git-тега.
- проверка последнего GitHub Release при запуске и предложение скачать новую версию.
- обучение кнопок автомобильного руля для навигации по альбомам и исполнителям.

## Технологии

- Kotlin;
- Jetpack Compose и Material 3;
- ViewModel, Coroutines и StateFlow;
- Room;
- Coil;
- Android Keystore;
- GitHub Actions.

Минимальная версия Android — 8.0 (API 26).

## Запуск проекта

1. Установите Android Studio и Android SDK.
2. Откройте корень репозитория в Android Studio.
3. Дождитесь завершения Gradle Sync.
4. Создайте или выберите Android-эмулятор либо подключите телефон с включённой отладкой по USB.
5. Запустите конфигурацию `app`.

Сборка из командной строки на Windows:

```powershell
.\gradlew.bat assembleDebug
```

Готовый debug APK находится в каталоге:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Тестирование

Unit-тесты:

```powershell
.\gradlew.bat testDebugUnitTest
```

Инструментальные тесты на подключённом устройстве или запущенном эмуляторе:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## Публикация APK

Workflow `.github/workflows/release-apk.yml` запускается при отправке тега формата `v0.0.1`. Он выполняет unit-тесты, собирает подписанный release APK и создаёт GitHub Release, из которого APK можно скачать на телефон.

Для подписи необходимо один раз создать постоянный файл `release.jks` и добавить в `Settings → Secrets and variables → Actions` четыре repository secret:

- `RELEASE_KEYSTORE_BASE64` — файл `release.jks`, преобразованный в Base64;
- `RELEASE_KEYSTORE_PASSWORD` — пароль хранилища;
- `RELEASE_KEY_ALIAS` — alias ключа, например `myrecordcollection`;
- `RELEASE_KEY_PASSWORD` — пароль закрытого ключа.

Пример создания ключа:

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" `
  -genkeypair `
  -v `
  -keystore .\release.jks `
  -alias myrecordcollection `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000
```

Преобразование ключа в Base64 и копирование результата в буфер обмена:

```powershell
[Convert]::ToBase64String(
    [IO.File]::ReadAllBytes((Resolve-Path .\release.jks))
) | Set-Clipboard
```

После настройки секретов создайте и отправьте тег:

```powershell
git tag v0.0.1
git push origin v0.0.1
```

Файл `release.jks` и пароли нельзя добавлять в Git. Необходимо хранить резервную копию ключа: без него Android не позволит установить будущую версию поверх ранее установленного приложения.

## Дополнительная документация

Текущая архитектура, принятые решения и план дальнейшей разработки описаны в [`PROJECT_NOTES.md`](PROJECT_NOTES.md).
