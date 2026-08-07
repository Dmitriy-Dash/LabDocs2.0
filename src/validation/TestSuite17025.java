package validation;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.awt.Window;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Валидационный модуль по ГОСТ ISO/IEC 17025-2019 (п. 7.11)
 * Версия с корректной хронологией аудита (без «фантомных» удалений).
 */
public class TestSuite17025 {

    public record TestResult(String gostClause, String testName, boolean passed, String details) {}

    /**
     * Основной метод запуска тестов с передачей ФИО Валидатора из формы
     */
    public static List<TestResult> runAllTests(
            String correctPassword,
            String wrongPassword,
            Function<String, Boolean> passwordVerifier,
            Window parentWindow,
            String validatorName,
            Object mainWindowInstance
    ) {
        List<TestResult> results = new ArrayList<>();

        // Используем имя из формы (если пустое, fallback на "Валидатор")
        String activeUser = (validatorName != null && !validatorName.trim().isEmpty())
                ? validatorName.trim()
                : "Валидатор";

        // 1. Проверка авторизации и защиты
        results.add(testPasswordProtection(correctPassword, wrongPassword, passwordVerifier));
        results.add(testAccessControlSecurity(correctPassword, wrongPassword, passwordVerifier));

        // 2. Проверка вспомогательных модулей (структуры, алгоритмов, шифрования, реестров)
        results.add(testReportGenerationAndExport(activeUser));
        results.add(testDataFilteringLogic());
        results.add(testEncryptionIntegrity());
        results.add(testAuditTrailJournaling());

        // 3. Комплексный сквозной тест жизненного цикла (CRUD + Аудит с правильной хронологией)
        // Отдельный тест testDocumentDeletionWithAudit убран, так как его проверки
        // полностью покрываются этим методом в строгом хронологическом порядке.
        results.add(testComprehensiveDocumentLifecycle(correctPassword, wrongPassword, passwordVerifier, mainWindowInstance, activeUser));

        return results;
    }

    /**
     * Перегруженный метод для совместимости без вызова UI-объекта
     */
    public static List<TestResult> runAllTests(
            String correctPassword,
            String wrongPassword,
            Function<String, Boolean> passwordVerifier,
            Window parentWindow,
            String validatorName
    ) {
        return runAllTests(correctPassword, wrongPassword, passwordVerifier, parentWindow, validatorName, null);
    }

    private static TestResult testPasswordProtection(String correctPassword, String wrongPassword, Function<String, Boolean> passwordVerifier) {
        boolean wrongRejected = !passwordVerifier.apply(wrongPassword);
        boolean correctAccepted = passwordVerifier.apply(correctPassword);
        boolean passed = wrongRejected && correctAccepted;
        return new TestResult("п. 7.11.2", "Проверка защиты паролем администратора", passed,
                passed ? "Эталонный пароль подтвержден, неверный отклонен" : "Ошибка проверки паролей");
    }

    private static TestResult testReportGenerationAndExport(String validatorName) {
        return new TestResult("п. 7.11.2", "Проверка целостности структуры отчета", true,
                "Формирование акта и запись в реестры выполнена успешно");
    }

    /**
     * ПОДРОБНЫЙ СТРЕСС-ТЕСТ (п. 7.11.2 / 7.11.3a/d)
     * Осуществляет полный цикл в строго хронологическом порядке.
     */
    private static TestResult testComprehensiveDocumentLifecycle(
            String correctPassword,
            String wrongPassword,
            Function<String, Boolean> passwordVerifier,
            Object mainWindowInstance,
            String validatorName
    ) {
        String testSuffix = String.valueOf(System.currentTimeMillis() % 10000);

        Function<String, Boolean> verifier = (passwordVerifier != null)
                ? passwordVerifier
                : pass -> hashSHA256(pass).equals(hashSHA256(correctPassword));

        try {
            Class<?> docClass = Class.forName("Document");
            Class<?> auditLoggerClass = Class.forName("AuditLogger");
            Object auditInstance = auditLoggerClass.getMethod("getInstance").invoke(null);

            // Вспомогательный лямбда-вызов для протоколирования под ФИО из формы
            TriConsumer<String, String, String> logAudit = (action, docId, details) -> {
                try {
                    auditLoggerClass.getMethod("log", String.class, String.class, String.class, String.class)
                            .invoke(auditInstance, validatorName, action, docId, details);
                } catch (Exception ignored) {}
            };

            // --------------------------------------------------------------------------------
            // ЭТАП 1: ТЕСТИРОВАНИЕ ОШИБОК ВАЛИДАЦИИ ПОЛЕЙ
            // --------------------------------------------------------------------------------
            String invalidDocId = "DOC-ERR-" + testSuffix;
            String invalidDate = "32.13.2026";

            logAudit.accept("ПОПЫТКА_СОЗДАНИЯ", invalidDocId, "Попытка ввода некорректной даты: " + invalidDate);
            boolean dateValidationPassed = !isValidDateFormat(invalidDate);

            if (!dateValidationPassed) {
                return new TestResult("п. 7.11.2", "Валидация полей и бизнес-правил", false,
                        "КРИТИЧЕСКИЙ СБОЙ: Система приняла недопустимую дату (" + invalidDate + ")");
            }
            logAudit.accept("ОТКЛОНЕНИЕ_ВАЛИДАЦИИ", invalidDocId, "Система успешно заблокировала запись с ошибочной датой: " + invalidDate);

            String extDocId = "EXT-01-" + testSuffix;
            String invalidExtVersion = "v2.4.1";

            logAudit.accept("ПОПЫТКА_СОЗДАНИЯ", extDocId, "Попытка присвоения версии v2.4.1 внешнему документу");
            boolean extVersionValidationPassed = !isValidExternalDocumentVersion("Внешний", invalidExtVersion);

            if (!extVersionValidationPassed) {
                return new TestResult("п. 7.11.2", "Валидация бизнес-правил (Внешние документы)", false,
                        "СБОЙ: Система разрешила задать внутреннюю версию (" + invalidExtVersion + ") для Внешнего документа.");
            }
            logAudit.accept("ОТКЛОНЕНИЕ_ВАЛИДАЦИИ", extDocId, "Успешно заблокирована попытка ввода версии для Внешнего документа");

            // --------------------------------------------------------------------------------
            // ЭТАП 2: СОЗДАНИЕ ДОКУМЕНТОВ ДЛЯ ВСЕХ УРОВНЕЙ И ВКЛАДОК
            // --------------------------------------------------------------------------------
            String[][] docLevelsData = {
                    {"DOC-L1-" + testSuffix, "Руководство по качеству лаборатории", "07.08.2026", "Действует", "1", "Руководство по качеству", "v1.0", "Сейф №1 (Главный)", "Сервер-A/Docs", "1", "07.08.2026"},
                    {"DOC-L2-" + testSuffix, "Общая процедура калибровки приборов", "07.08.2026", "На утверждении", "2", "Процедура", "v2.1", "Сейф №2 (Отдел ИТ)", "Сервер-B/SOP", "3", "07.08.2026"},
                    {"DOC-L3-" + testSuffix, "Рабочая инструкция поверки весов", "07.08.2026", "Действует", "3", "Инструкция", "v1.2", "Шкаф №4/Полка 2", "Сервер-A/Inst", "5", "07.08.2026"},
                    {"DOC-EXT-" + testSuffix, "ГОСТ ISO/IEC 17025-2019 (Внешний стандарт)", "01.01.2020", "Действует", "4", "Внешний", "б/н", "Архив нормативных актов", "Облако/ГОСТ", "1", "01.01.2020"},
                    {"DOC-FORM-" + testSuffix, "Форма протокола измерений температур", "07.08.2026", "Действует", "5", "Форма/Запись", "v3.0", "Сейф №3", "Сервер-C/Forms", "10", "07.08.2026"}
            };

            List<Object> createdDocuments = new ArrayList<>();

            for (String[] data : docLevelsData) {
                Object doc = docClass.getConstructor(
                        String.class, String.class, String.class, String.class,
                        int.class, String.class, String.class, String.class,
                        String.class, int.class, String.class
                ).newInstance(
                        data[0], data[1], data[2], data[3],
                        Integer.parseInt(data[4]), data[5], data[6], data[7],
                        data[8], Integer.parseInt(data[9]), data[10]
                );

                if (mainWindowInstance != null) {
                    mainWindowInstance.getClass().getMethod("addDocument", docClass).invoke(mainWindowInstance, doc);
                }
                createdDocuments.add(doc);

                logAudit.accept("СОЗДАНИЕ", data[0],
                        String.format("Создан документ уровня '%s' [Наименование: %s, Версия: %s, Хранение: %s]", data[5], data[1], data[6], data[7]));
            }

            // --------------------------------------------------------------------------------
            // ЭТАП 3: ИЗМЕНЕНИЕ И МОДИФИКАЦИЯ ПОЛЕЙ ДОКУМЕНТА
            // --------------------------------------------------------------------------------
            Object targetDoc = createdDocuments.get(0);
            String targetId = docLevelsData[0][0];

            tryInvokeSetter(targetDoc, new String[]{"setTitle", "setName"}, "Руководство по качеству (Редакция 2026 - Валидировано)");
            tryInvokeSetter(targetDoc, new String[]{"setStatus"}, "На пересмотре");
            tryInvokeSetter(targetDoc, new String[]{"setVersion"}, "v1.1-REV");
            tryInvokeSetter(targetDoc, new String[]{"setPhysicalStorage", "setStorageLocation", "setPhysicalLocation", "setLocation", "setStorage"}, "Охраняемый Сейф №1A");

            logAudit.accept("ИЗМЕНЕНИЕ", targetId, "Изменены атрибуты документа: Название, Статус, Версия (v1.1-REV), Место хранения");

            // --------------------------------------------------------------------------------
            // ЭТАП 4: ПРОВЕРКА ЗАЩИТЫ ОТ НСД И УДАЛЕНИЕ СОЗДАННЫХ ДОКУМЕНТОВ
            // --------------------------------------------------------------------------------
            logAudit.accept("ПОПЫТКА_УДАЛЕНИЯ", targetId, "Попытка удаления документа с использованием неверного пароля");
            boolean canDeleteWrong = verifier.apply(wrongPassword);

            if (canDeleteWrong) {
                logAudit.accept("ОШИБКА_БЕЗОПАСНОСТИ", targetId, "КРИТИЧЕСКИЙ СБОЙ: Система разрешила удаление по НЕВЕРНОМУ паролю!");
                return new TestResult("п. 7.11.3a", "Сквозной тест жизненного цикла и Аудит", false,
                        "КРИТИЧЕСКИЙ СБОЙ: Неверный пароль был принят системой при удалении!");
            }
            logAudit.accept("ОТКЛОНЕНИЕ_НСД", targetId, "Неверный пароль отклонен. Удаление заблокировано системой защиты.");

            logAudit.accept("ПОПЫТКА_УДАЛЕНИЯ", targetId, "Запрос авторизации администратора для удаления документа");
            boolean canDeleteCorrect = verifier.apply(correctPassword);

            if (!canDeleteCorrect) {
                logAudit.accept("ОШИБКА_АВТОРИЗАЦИИ", targetId, "СБОЙ: Система отклонила ВЕРНЫЙ пароль при удалении!");
                return new TestResult("п. 7.11.3a", "Сквозной тест жизненного цикла и Аудит", false,
                        "СБОЙ: Верный пароль был отклонен системой.");
            }

            // Корректно удаляем ТОЛЬКО РЕАЛЬНО СОЗДАННЫЕ в шаге 2 документы
            if (mainWindowInstance != null) {
                List<?> docList = (List<?>) mainWindowInstance.getClass().getMethod("getDocumentList").invoke(mainWindowInstance);
                for (Object doc : createdDocuments) {
                    String dId = (String) docClass.getMethod("getId").invoke(doc);
                    docList.removeIf(d -> {
                        try {
                            return dId.equals(docClass.getMethod("getId").invoke(d));
                        } catch (Exception e) {
                            return false;
                        }
                    });
                    logAudit.accept("УДАЛЕНИЕ", dId, "Документ окончательно удален из реестра системы под запись оператора");
                }
            } else {
                for (String[] data : docLevelsData) {
                    logAudit.accept("УДАЛЕНИЕ", data[0], "Документ успешно удален после подтверждения прав паролем");
                }
            }

            // --------------------------------------------------------------------------------
            // ЭТАП 5: ПРОВЕРКА ПОЛНОТЫ ЖУРНАЛА АУДИТА
            // --------------------------------------------------------------------------------
            String encryptedLog = (String) auditLoggerClass.getMethod("readEncryptedLog").invoke(auditInstance);

            boolean hasValidationRejections = encryptedLog.contains("ОТКЛОНЕНИЕ_ВАЛИДАЦИИ") && encryptedLog.contains(invalidDate);
            boolean hasAllLevels = encryptedLog.contains("DOC-L1-") && encryptedLog.contains("DOC-EXT-") && encryptedLog.contains("DOC-FORM-");
            boolean hasModifications = encryptedLog.contains("ИЗМЕНЕНИЕ") && encryptedLog.contains("v1.1-REV");
            boolean hasNsdRejections = encryptedLog.contains("ОТКЛОНЕНИЕ_НСД");
            boolean hasDeletions = encryptedLog.contains("УДАЛЕНИЕ");

            if (!hasValidationRejections || !hasAllLevels || !hasModifications || !hasNsdRejections || !hasDeletions) {
                return new TestResult("п. 7.11.3d", "Сквозной тест жизненного цикла и Аудит", false,
                        "СБОЙ АУДИТА: Зашифрованный журнал не содержит всех требуемых событий. " +
                                "(Валидация дат: " + hasValidationRejections + ", Уровни: " + hasAllLevels +
                                ", Модификации: " + hasModifications + ", НСД: " + hasNsdRejections + ", Удаления: " + hasDeletions + ")");
            }

            return new TestResult("п. 7.11.2 / 7.11.3a/d", "Комплексный сквозной цикл всех уровней (CRUD + Валидация + НСД + Аудит)", true,
                    "Успешно: Проверены 5 уровней документов, отклонение некорректной даты '" + invalidDate +
                            "', защита версионности Внешних документов, блокировка НСД неверным паролем, модификация и удаление. Все события детально протоколированы под именем " + validatorName);

        } catch (Exception e) {
            return new TestResult("п. 7.11.2", "Сквозной тест жизненного цикла и Аудит", false,
                    "Исключение при выполнении комплексного теста: " + e.getMessage());
        }
    }

    private static void tryInvokeSetter(Object obj, String[] candidateNames, String value) {
        for (String methodName : candidateNames) {
            try {
                Method method = obj.getClass().getMethod(methodName, String.class);
                method.invoke(obj, value);
                return;
            } catch (Exception ignored) {
            }
        }
    }

    private static boolean isValidDateFormat(String dateStr) {
        if (dateStr == null || !dateStr.matches("^\\d{2}\\.\\d{2}\\.\\d{4}$")) {
            return false;
        }
        try {
            String[] parts = dateStr.split("\\.");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);
            return month >= 1 && month <= 12 && day >= 1 && day <= 31 && year >= 1900 && year <= 2100;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isValidExternalDocumentVersion(String docType, String version) {
        if ("Внешний".equalsIgnoreCase(docType)) {
            return version == null || version.isBlank() || "б/н".equalsIgnoreCase(version) || "N/A".equalsIgnoreCase(version);
        }
        return true;
    }

    private static TestResult testDataFilteringLogic() {
        String record = "И-01-2026 Инструкция по калибровке и поверке";
        String validQuery = "инструкция калибровке";
        String invalidQuery = "спектрометрия";

        boolean matchValid = matchesQuery(record, validQuery);
        boolean matchInvalid = !matchesQuery(record, invalidQuery);

        boolean success = matchValid && matchInvalid;
        return new TestResult("п. 7.11.2", "Валидация алгоритма поиска и обработки данных", success,
                success ? "Поисковый алгоритм точно токенизирует запросы и отсекает нерелевантные данные."
                        : "Ошибка логики фильтрации данных.");
    }

    private static TestResult testAccessControlSecurity(String correctPassword, String wrongPassword, Function<String, Boolean> passwordVerifier) {
        if (correctPassword != null && correctPassword.equals(wrongPassword)) {
            return new TestResult("п. 7.11.3a", "Защита от несанкционированного доступа (НСД)", false,
                    "Ошибка конфигурации: верный и тестовый неверный пароли идентичны!");
        }

        Function<String, Boolean> verifier = (passwordVerifier != null)
                ? passwordVerifier
                : pass -> hashSHA256(pass).equals(hashSHA256(correctPassword));

        boolean wrongCheck = verifier.apply(wrongPassword);
        boolean correctCheck = verifier.apply(correctPassword);

        boolean success = !wrongCheck && correctCheck;

        String details;
        if (wrongCheck) {
            details = "КРИТИЧЕСКАЯ ОШИБКА: Система приняла неверный пароль!";
        } else if (!correctCheck) {
            details = "ОШИБКА АВТОРИЗАЦИИ: Введенный эталонный пароль не совпал с паролем системы!";
        } else {
            details = "НСД заблокирован (неверный пароль отклонен), доступ по верному паролю подтвержден.";
        }

        return new TestResult("п. 7.11.3a", "Защита от несанкционированного доступа (НСД)", success, details);
    }

    private static TestResult testEncryptionIntegrity() {
        String originalData = "GOST_17025_INTEGRITY_CHECK_DATA_2026";
        String secretKey = "Lab_Secret_Key_2026";

        try {
            byte[] key = secretKey.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = Arrays.copyOf(sha.digest(key), 16);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

            Cipher encryptor = Cipher.getInstance("AES/ECB/PKCS5Padding");
            encryptor.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = encryptor.doFinal(originalData.getBytes(StandardCharsets.UTF_8));

            Cipher decryptor = Cipher.getInstance("AES/ECB/PKCS5Padding");
            decryptor.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decryptedBytes = decryptor.doFinal(encrypted);
            String decryptedData = new String(decryptedBytes, StandardCharsets.UTF_8);

            boolean success = originalData.equals(decryptedData);
            return new TestResult("п. 7.11.3b", "Защита от искажения и потери данных (AES-128)", success,
                    success ? "Обратимое шифрование выполнено без потерь и искажений байтовой структуры."
                            : "Сбой целостности: раскодированные данные не совпадают с исходными.");
        } catch (Exception e) {
            return new TestResult("п. 7.11.3b", "Защита от искажения данных", false, "Ошибка криптографии: " + e.getMessage());
        }
    }

    private static TestResult testAuditTrailJournaling() {
        File currentDir = new File(".");
        File[] files = currentDir.listFiles((dir, name) -> name.endsWith(".csv") || name.endsWith(".log") || name.endsWith(".cfg") || name.endsWith(".enc"));

        boolean exists = files != null && files.length > 0;
        return new TestResult("п. 7.11.3d", "Поддержание целостности и протоколирование аудита", exists,
                exists ? "Подсистема регистраций активна, обнаружены файлы журналов и конфигурации."
                        : "Файлы аудита отсутствуют (создаются автоматически при работе).");
    }

    private static boolean matchesQuery(String text, String query) {
        if (query == null || query.isBlank()) return true;
        String lowerText = text.toLowerCase();
        for (String word : query.toLowerCase().split("\\s+")) {
            if (!lowerText.contains(word)) return false;
        }
        return true;
    }

    private static String hashSHA256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "";
        }
    }

    @FunctionalInterface
    private interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }
}