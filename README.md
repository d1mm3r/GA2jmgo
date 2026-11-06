# GA2jmgo — Google Ассистент на кнопке микрофона JMGO N1 Ultra (китайская версия)

Кнопка микрофона на родном пульте китайского проектора **JMGO N1 Ultra** вызывает **Google Ассистента** (ТВ-версия, `com.google.android.katniss`) вместо китайского ассистента iFlytek. Root, перепрошивка и разборка не нужны — только `adb` по сети и маленькое приложение-«мостик» **Mic2Assistant** из этого репозитория.

> Проверено на одном проекторе: N1 Ultra, SN `H930…`, Android 9 (BonfireOS), прошивка `1.1.31.22`. На других прошивках может отличаться — см. [Проверка после сна](#шаг-7-проверка-после-сна).

## Как это работает

- Кнопку микрофона (keycode `609`) ловит системный сервис `com.jmgo.hippo` и рассылает **неявную** рассылку `com.jmgo.action.AI_VOICE` с extra `operate=NEAR_START` (нажатие) / `NEAR_STOP` (отпускание). Её может принять любое приложение.
- **Mic2Assistant** принимает `NEAR_START` и открывает Google Ассистента в режиме прослушивания (`ACTION_ASSIST` → `SearchActivityTrampoline`).
- Перед сном (STR) прошивка принудительно останавливает все сторонние приложения, кроме зашитого в `services.jar` белого списка (`PhoneWindowManager.clearBackagroundApplication`). Поэтому приложение называется **`com.bajintech.assistant`** — это имя из белого списка, такого пакета на проекторе нет. Так оно переживает сон.
- Остановка Ассистента перед сном заставляет Android обнулить настройки ассистента. Mic2Assistant следит за ними (`ContentObserver`, `SCREEN_ON`, `BOOT_COMPLETED`, а также при каждом нажатии) и восстанавливает. Для этого нужно право `WRITE_SECURE_SETTINGS`, выдаётся через adb.

## 1. Входные данные (исходное состояние)

- JMGO N1 Ultra, китайская версия, Android 9, adb по сети включён.
- Google-сервисы (GMS, Google Play) установлены, Google-аккаунт добавлен (по инструкциям из темы на 4PDA).
- Свой лаунчер (например, Projectivy), при желании русский язык через MoreLocale 2.
- Родной Bluetooth-пульт с кнопкой микрофона.
- Компьютер с [Android Platform Tools](https://developer.android.com/tools/releases/platform-tools) (`adb`) в той же сети.
- Скачать:
  - **Google для Android TV** (`com.google.android.katniss`), armeabi-v7a, Android 9+ (проверено с 7.55.87);
  - **Device ID** (Evozi) или **My Device IDs: GSF GAID viewer** — узнать GSF ID;
  - **Mic2Assistant.apk** — из [Releases](../../releases) или собрать самому (см. [Сборка](#сборка)).

**Не работает на этой прошивке** (не тратьте время):

| Что | Почему |
|---|---|
| Алиса («Яндекс с Алисой», «Алиса AI») | Интерфейс веб, системный WebView версии 66 (2018), подписан MediaTek и без root не обновляется — белый экран |
| Key Mapper, Button Mapper, переназначение кнопок в Projectivy | Прошивка блокирует включение сторонних служб специальных возможностей |
| `adb root` / `adb remount` | adbd не получает root |
| Телефонный Google app (`googlequicksearchbox`) | Упирается в Play Protect, на ТВ неудобен — нужен katniss |

## 2. Playbook

Все команды — на компьютере. IP подставьте свой.

### Шаг 0. Подключение
```bash
adb connect 192.168.1.XXX:5555
adb devices
```

### Шаг 1. Регистрация проектора в Google (снимает блок Play Protect)
1. В Device ID найдите строку **Google Service Framework (GSF)** — шестнадцатеричное число.
2. Переведите в десятичное, например в PowerShell: `[Convert]::ToUInt64("ВАШ_HEX_GSF", 16)`.
3. Зарегистрируйте его на <https://www.google.com/android/uncertified> под тем же аккаунтом, что на проекторе.
4. Подождите от нескольких минут до пары часов.

### Шаг 2. ТВ-ассистент Google
```bash
adb install <katniss>.apk
```

### Шаг 3. Назначить Google системным ассистентом
```bash
adb shell settings put secure voice_interaction_service com.google.android.katniss/com.google.android.katniss.search.serviceapi.KatnissVoiceInteractionService
adb shell settings put secure assistant com.google.android.katniss/com.google.android.katniss.search.serviceapi.KatnissVoiceInteractionService
adb shell settings put secure voice_recognition_service com.google.android.katniss/com.google.android.apps.tvsearch.voice.recognition.KatnissRecognitionService
```
Проверка — должна выехать панель «Задайте вопрос»:
```bash
adb shell am start -a android.intent.action.ASSIST -n com.google.android.katniss/com.google.android.apps.tvsearch.app.launch.trampoline.SearchActivityTrampoline
```

### Шаг 4. Отключить китайского ассистента
```bash
adb shell pm disable-user --user 0 com.jmgo.ai.voice
```

### Шаг 5. Mic2Assistant
```bash
adb install Mic2Assistant.apk
adb shell pm grant com.bajintech.assistant android.permission.WRITE_SECURE_SETTINGS
adb shell am broadcast -n com.bajintech.assistant/local.mic2assistant.RestoreReceiver -a local.mic2assistant.RESTORE
```
Последняя команда выводит свежеустановленное приложение из состояния «остановлено» (иначе Android не доставляет ему рассылки).

### Шаг 6 (по желанию). Русский язык
Ассистент берёт язык системы: в MoreLocale выберите «Русский (Россия)».

### Шаг 7. Проверка после сна
Выключите проектор кнопкой питания (это сон), включите:
```bash
adb shell "dumpsys package com.bajintech.assistant | grep stopped="
adb shell settings get secure assistant
```
Должно быть `stopped=false` и katniss в настройке. `stopped=true` значит, что на вашей прошивке белый список другой.

## 3. Выходные данные (итоговое состояние)

- Системный ассистент и распознавание речи — Google для Android TV; проектор зарегистрирован в Google.
- `com.jmgo.ai.voice` отключён.
- Установлен `com.bajintech.assistant` (Mic2Assistant): кнопка микрофона → Google Ассистент, переживает сон, сам восстанавливает настройки.
- Системный раздел, WebView, прошивка и обновления не тронуты, root нет.

## Использование

Пульт передаёт звук, **только пока кнопка зажата**:
1. Зажмите кнопку микрофона.
2. Дождитесь, пока «запрыгают» точки Ассистента — слушать он начинает через 0,4–1,3 с, сказанное раньше теряется.
3. Скажите фразу и отпустите кнопку.

## Проблемы

- **Звук обрывается сразу после нажатия.** К проектору по Bluetooth подключено ещё активное устройство (например, мышь) — голос с пульта идёт по тому же Bluetooth. Отключите его или подключите через USB-приёмник; Wi-Fi проектора лучше перевести на 5 ГГц.
- **«Google Assistant is starting. Please try again».** Ассистент не назначен — повторите шаг 3 или нажмите кнопку ещё раз.
- **После сна кнопка не работает.** Шаг 7; при `stopped=true` повторите последнюю команду шага 5.

## Откат
```bash
adb uninstall com.bajintech.assistant
adb shell pm enable com.jmgo.ai.voice
adb shell settings delete secure assistant
```

## Сборка

Без Gradle — нужны JDK и Android SDK (build-tools 37.0.0, platform android-36.1):
```bash
bash build.sh   # Windows: Git Bash
```
Пути переопределяются переменными `ANDROID_SDK` и `JDK_BIN`. При первой сборке создаётся `debug.keystore` для подписи — в репозиторий он не попадает. Исходники: [`AndroidManifest.xml`](AndroidManifest.xml), [`src/local/mic2assistant/`](src/local/mic2assistant). `targetSdk 25` нужен, чтобы манифестный приёмник получал неявную рассылку на Android 8+.

Пост для форума 4PDA (BBCode): [`docs/4pda_post.bbcode`](docs/4pda_post.bbcode).
