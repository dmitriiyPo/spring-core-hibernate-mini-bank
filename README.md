# MiniBank (Spring Core)

Консольное учебное банковское приложение на Java + Spring Core.

## Что умеет
- создавать пользователей;
- показывать всех пользователей и их счета;
- создавать дополнительные счета;
- пополнять и снимать деньги;
- переводить между счетами (с комиссией для разных пользователей);
- закрывать счет с переносом остатка;
- завершать работу по команде `EXIT`.

## Технологии
- Java 21
- Spring Core (`spring-context`)
- Конфигурация через `@Configuration`, `@PropertySource`, `@Component`
- Хранение данных в памяти (`Map`)

## Архитектура
- `User`, `Account` — POJO-модели.
- `UserService`, `AccountService` — бизнес-логика и хранение данных.
- `OperationCommand` + `ConsoleOperationType` — обработка команд (Command pattern).
- `OperationsConsoleListener` — главный цикл приложения.
- `ConsoleInput` — единая точка чтения/валидации консольного ввода.

## Команды
- `USER_CREATE`
- `SHOW_ALL_USERS`
- `ACCOUNT_CREATE`
- `ACCOUNT_DEPOSIT`
- `ACCOUNT_WITHDRAW`
- `ACCOUNT_TRANSFER`
- `ACCOUNT_CLOSE`
- `EXIT`

## Настройки
Файл: `src/main/resources/application.properties`

```properties
account.default-amount=500
account.transfer-commission=0.02
```

## Запуск
1. Собрать проект:
```bash
mvn clean package
```
2. Запустить:
```bash
mvn exec:java -Dexec.mainClass="sorokin.java.course.Main"
```

Если `exec-maven-plugin` не настроен, можно запускать из IDE через класс `Main`.

## Дополнительные материалы
- Подробная формулировка Hibernate-ДЗ: `docs/hibernate-homework.md`
- Подсказки: `docs/hibernate-hints.md`
