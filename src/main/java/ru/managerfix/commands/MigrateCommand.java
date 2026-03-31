package ru.managerfix.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.managerfix.ManagerFix;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.core.MigrationManager;
import ru.managerfix.utils.MessageUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Команда миграции данных: /migrate <yaml2sql|sql2yaml>
 */
public final class MigrateCommand implements org.bukkit.command.CommandExecutor, TabCompleter {

    private final ManagerFix plugin;

    public MigrateCommand(ManagerFix plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("managerfix.command.migrate")) {
            MessageUtil.send(plugin, sender, "no-permission");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.parse("<#FAA300>Использование: /migrate <yaml2sql|sql2yaml>"));
            sender.sendMessage(MessageUtil.parse("<#E0E0E0>  yaml2sql - Перенос данных из YAML в MySQL (автоматически включит MySQL)"));
            sender.sendMessage(MessageUtil.parse("<#E0E0E0>  sql2yaml - Перенос данных из MySQL в YAML"));
            return true;
        }

        String action = args[0].toLowerCase();
        MigrationManager migrationManager = plugin.getMigrationManager();

        if (migrationManager == null) {
            sender.sendMessage(MessageUtil.parse("<#C0280F>MigrationManager не инициализирован!"));
            return true;
        }

        if ("yaml2sql".equals(action)) {
            // Миграция из YAML в SQL
            sender.sendMessage(MessageUtil.parse("<#FAA300>Начинаю миграцию данных из YAML в MySQL..."));

            if (!migrationManager.hasDataInYaml()) {
                sender.sendMessage(MessageUtil.parse("<#C0280F>Данные YAML не найдены или пусты!"));
                return true;
            }

            // Проверяем, что MySQL настроен и подключён
            if (!plugin.getConfigManager().isMySqlStorage() ||
                plugin.getDatabaseManager() == null ||
                !plugin.getDatabaseManager().isInitialized()) {

                sender.sendMessage(MessageUtil.parse("<#FAA300>Хранилище YAML или БД не подключена"));

                // Если ещё не переключили на MySQL — переключаем
                if (!plugin.getConfigManager().isMySqlStorage()) {
                    if (!switchToMySQL(sender)) {
                        sender.sendMessage(MessageUtil.parse("<#C0280F>Не удалось переключиться на MySQL! Проверьте настройки БД в config.yml"));
                        return true;
                    }
                    sender.sendMessage(MessageUtil.parse("<green>✓ Хранилище переключено на MYSQL в config.yml"));
                }
                
                sender.sendMessage(MessageUtil.parse("<red>"));
                sender.sendMessage(MessageUtil.parse("<red>⚠ ТРЕБУЕТСЯ ПЕРЕЗАГРУЗКА СЕРВЕРА! ⚠"));
                sender.sendMessage(MessageUtil.parse("<red>"));
                sender.sendMessage(MessageUtil.parse("<yellow>Выполните команду:"));
                sender.sendMessage(MessageUtil.parse("<gold>  /reload</gold> или <gold>/restart"));
                sender.sendMessage(MessageUtil.parse("<yellow>После перезагрузки снова введите:"));
                sender.sendMessage(MessageUtil.parse("<gold>  /migrate yaml2sql"));
                sender.sendMessage(MessageUtil.parse("<red>"));
                return true;
            }

            // БД подключена — выполняем миграцию
            try {
                sender.sendMessage(MessageUtil.parse("<yellow>Очистка таблиц MySQL..."));
                migrationManager.clearSqlTables();
                
                sender.sendMessage(MessageUtil.parse("<yellow>Миграция данных..."));
                migrationManager.migrateYamlToSql();
                
                sender.sendMessage(MessageUtil.parse("<green>✓ Миграция в MySQL завершена успешно!"));
                sender.sendMessage(MessageUtil.parse("<green>✓ Все данные перенесены в базу данных"));
                sender.sendMessage(MessageUtil.parse("<gray>Сервер готов к работе с MySQL"));
                
            } catch (Exception e) {
                sender.sendMessage(MessageUtil.parse("<red>Ошибка при миграции: " + e.getMessage()));
                LoggerUtil.log(java.util.logging.Level.SEVERE, "Migration error", e);
                e.printStackTrace();
                return true;
            }

        } else if ("sql2yaml".equals(action)) {
            // Миграция из SQL в YAML
            sender.sendMessage(MessageUtil.parse("<green>Начинаю миграцию данных из MySQL в YAML..."));
            
            if (!migrationManager.hasDataInSql()) {
                sender.sendMessage(MessageUtil.parse("<red>Данные MySQL не найдены или пусты!"));
                return true;
            }

            try {
                migrationManager.migrateSqlToYaml();
                sender.sendMessage(MessageUtil.parse("<green>✓ Миграция в YAML завершена успешно!"));
                sender.sendMessage(MessageUtil.parse("<gray>Перезагрузите сервер для применения изменений."));
                
            } catch (Exception e) {
                sender.sendMessage(MessageUtil.parse("<red>Ошибка при миграции: " + e.getMessage()));
                LoggerUtil.log(java.util.logging.Level.SEVERE, "Migration error", e);
                e.printStackTrace();
                return true;
            }

        } else {
            sender.sendMessage(MessageUtil.parse("<red>Неизвестная команда! Используйте: yaml2sql или sql2yaml"));
            return true;
        }

        sender.sendMessage(MessageUtil.parse("<gray>Проверьте консоль для подробностей."));
        return true;
    }

    /**
     * Переключает storage.type на MYSQL в config.yml
     */
    private boolean switchToMySQL(CommandSender sender) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            sender.sendMessage(MessageUtil.parse("<red>config.yml не найден!"));
            return false;
        }

        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            
            // Проверяем, что настройки БД заполнены
            String host = config.getString("database.host", "");
            String database = config.getString("database.database", "");
            String username = config.getString("database.username", "");
            
            if (host.isEmpty() || database.isEmpty() || username.isEmpty()) {
                sender.sendMessage(MessageUtil.parse("<red>Настройки базы данных не заполнены в config.yml!"));
                sender.sendMessage(MessageUtil.parse("<red>Заполните: database.host, database.database, database.username"));
                return false;
            }
            
            // Меняем storage.type на MYSQL
            config.set("storage.type", "MYSQL");
            config.save(configFile);
            
            // Обновляем ConfigManager
            plugin.getConfigManager().setStorageType("MYSQL");
            
            return true;
            
        } catch (IOException e) {
            sender.sendMessage(MessageUtil.parse("<red>Ошибка записи config.yml: " + e.getMessage()));
            LoggerUtil.log(java.util.logging.Level.SEVERE, "Failed to update config.yml", e);
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            String partial = args[0].toLowerCase();
            
            if ("yaml2sql".startsWith(partial)) {
                suggestions.add("yaml2sql");
            }
            if ("sql2yaml".startsWith(partial)) {
                suggestions.add("sql2yaml");
            }
            
            return suggestions;
        }
        return Collections.emptyList();
    }
}
