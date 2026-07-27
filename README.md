<div align="center">

<img src="https://user-images.githubusercontent.com/74448585/150906023-101cd383-da82-4a3c-9603-a3b5741c3994.png" alt="Reactive" width="200">

# Reactive

[![MIT License](https://img.shields.io/github/license/rizer001/Reactive?&logo=github)](LICENSE)
[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/rizer001/Reactive/build.yml?branch=main&event=push&logo=github)](https://github.com/rizer001/Reactive/actions)
[![GitHub release](https://img.shields.io/github/v/release/rizer001/Reactive?include_prereleases&logo=github)](https://github.com/rizer001/Reactive/releases)
[![Discord](https://img.shields.io/discord/685683385313919172?logo=discord&label=Discord)](https://purpurmc.org/discord)

**Reactive** — высокопроизводительный форк [Purpur](https://github.com/PurpurMC/Purpur) для Minecraft 26.2, основанный на [Paper](https://github.com/PaperMC/Paper).  
Проект наследует все возможности Purpur и добавляет собственные улучшения: расширенные границы мира, автоконфигурацию при первом запуске, интегрированную базу данных SQLite и многое другое.

---

**Скачать последнюю сборку:** [GitHub Releases](https://github.com/rizer001/Reactive/releases)

---

</div>

## ✨ Что такое Reactive?

Reactive — это drop-in замена для Purpur/Paper серверов. Вы просто заменяете JAR-файл — и получаете:

- ✅ **Всю функциональность Purpur** — более 400 настраиваемых опций, WASD-управление, конфигурация мобов, предметов, блоков, ИИ и многое другое
- ✅ **Производительность Paper** — оптимизированный чанковый движок, анти-лаг системы, Moonrise патчи
- ✅ **Ванильная совместимость** — все плагины для Paper/Spigot работают без изменений
- ✅ **Собственные улучшения Reactive** — уникальные фичи, описанные ниже

---

## 🚀 Уникальные возможности Reactive

### 📐 Расширенная высота мира (до 2048 блоков)

Reactive позволяет увеличить строительную высоту мира до **2048 блоков** (вместо стандартных 320). Настраивается в `config/reactive-config.yml`:

```yaml
reactive:
  world-height:
    enabled: true
    overworld-min-y: -64
    overworld-max-y: 2048
    nether-min-y: 0
    nether-max-y: 256
    end-min-y: 0
    end-max-y: 256
```

- Расширение реализовано через перехват CODEC `DimensionType` до заморозки реестра
- Работает на стороне сервера — **клиенту не нужны моды или ресурспаки**
- Все команды (`/tp`, `/setblock`, `/fill` и др.) используют обновлённые лимиты

### 🗺️ Расширение X/Z до 67 млн блоков

Горизонтальные координаты могут быть расширены до **67 000 000 блоков** по X/Z. Включение:

```yaml
reactive:
  world-border:
    enabled: false
    max-xz: 67000000
```

### ⚙️ Автоконфигурация при первом запуске

При первом запуске Reactive автоматически создаёт папку `config/` и генерирует все необходимые файлы:

| Файл | Назначение |
|------|-----------|
| `config/server.properties` | Основные настройки сервера |
| `config/bukkit.yml` | Настройки Bukkit |
| `config/spigot.yml` | Настройки Spigot |
| `config/purpur.yml` | Настройки Purpur |
| `config/reactive-config.yml` | **Настройки Reactive** |

**Важно:** папка `config/` является **единственным источником конфигурационных файлов**. Если файла нет при запуске — он будет создан автоматически.

**`reactive-config.yml`** — центральный конфиг для всех уникальных возможностей Reactive.

### 🔒 EULA — автосоглашение

Reactive автоматически принимает EULA от вашего имени при первом запуске. Это записывается в базу данных и не требует ручного редактирования `eula.txt`.

### 🗄️ SQLite база данных (опционально)

Встроенная поддержка SQLite для хранения данных сервера:

```yaml
reactive:
  database:
    enabled: false
    type: sqlite
    sqlite-file: reactive.db
```

### 🏷️ Брендинг

- **Название сервера:** `Reactive Server`
- **Идентификатор:** `rizer001:reactive`
- **Консоль:** отображает `Reactive` вместо `Purpur`
- **JAR-файл:** `reactive-server.jar` при сборке

---

## 📦 Установка

1. **Скачайте** последний JAR из [Releases](https://github.com/rizer001/Reactive/releases)
2. **Замените** ваш текущий серверный JAR на `reactive-server.jar`
3. **Запустите** сервер. Reactive сам создаст папку `config/` и все необходимые файлы
4. **Настройте** `config/reactive-config.yml` под свои нужды

> **Требования:** Java 25+ (рекомендуется Eclipse Adoptium Temurin-25+)

---

## 🛠️ Сборка из исходников

### Требования
- **Java 25+** (Eclipse Adoptium Temurin)
- **Git**

### Инструкция

```bash
# Клонируем репозиторий
git clone https://github.com/rizer001/Reactive.git
cd Reactive

# Применяем все патчи
./gradlew applyAllPatches

# Собираем сервер
./gradlew build

# Собираем server-ready JAR
./gradlew createReactiveJar
```

Собранный JAR будет находиться в `build/libs/reactive-server.jar` или `reactive-server/build/libs/reactive-server.jar`.

### Сборка в IDE
После `./gradlew applyAllPatches` проект готов к импорту в IntelliJ IDEA или Eclipse.

### Создание патча

1. Внесите изменения в `paper-server/` или `reactive-server/src/minecraft/`
2. Запустите `./gradlew rebuildPatches`
3. Патчи появятся в `reactive-server/paper-patches/` или `reactive-server/minecraft-patches/`

---

## 📋 Структура проекта

```
Reactive/
├── reactive-api/          # API (наследует Paper API)
│   └── paper-patches/     # Патчи для API
├── reactive-server/       # Серверная часть
│   ├── paper-patches/     # Патчи для Paper-сервера
│   │   ├── features/      # Функциональные патчи
│   │   └── files/         # Патчи для отдельных файлов
│   ├── minecraft-patches/ # Патчи для Minecraft кода
│   │   ├── features/      # Функциональные патчи (Purpur)
│   │   └── sources/       # Патчи для исходников
│   └── src/
│       ├── main/java/     # Reactive и Purpur код
│       └── minecraft/     # Minecraft код с изменениями
├── paper-api/             # Paper API (сгенерировано)
├── paper-server/          # Paper Server (сгенерировано)
├── patches/               # Дополнительные патчи
├── build-data/            # Данные для сборки
└── gradle.properties      # Версии и настройки
```

---

## 🔗 API

### Репозиторий Maven

```kotlin
repositories {
    maven("https://repo.purpurmc.org/snapshots")
}
```

```kotlin
dependencies {
    compileOnly("org.rizer001.reactive:reactive-api:26.2.build.+")
}
```

API включает все интерфейсы Paper, Spigot и Bukkit.

---

## 📜 Лицензия

Все патчи Reactive распространяются под лицензией **MIT**, если не указано иное в заголовках патчей.

[![MIT License](https://img.shields.io/github/license/rizer001/Reactive?&logo=github)](LICENSE)

См. [PaperMC/Paper](https://github.com/PaperMC/Paper) и [PurpurMC/Purpur](https://github.com/PurpurMC/Purpur) для информации о лицензиях использованных материалов.

---

## 🙏 Благодарности

- **[PurpurMC/Purpur](https://github.com/PurpurMC/Purpur)** — основа проекта
- **[PaperMC/Paper](https://github.com/PaperMC/Paper)** — производительный движок
- **[PaperMC/Paperweight](https://github.com/PaperMC/paperweight)** — система сборки
- **[YourKit](https://www.yourkit.com/)** — профилировщик Java
- **Всем контрибьюторам** Purpur, Paper и Reactive

---

<div align="center">

**[GitHub](https://github.com/rizer001/Reactive)** · **[Releases](https://github.com/rizer001/Reactive/releases)** · **[Issues](https://github.com/rizer001/Reactive/issues)**

</div>
