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
            sender.sendMessage(MessageUtil.parse("<#00C8FF>Использование: /migrate <yaml2sql|sql2yaml|yaml2sqlite|sqlite2yaml>"));
            sender.sendMessage(MessageUtil.parse("<#F0F4F8>  yaml2sql - Перенос данных из YAML в SQL (автоматически включит SQLITE)"));
            sender.sendMessage(MessageUtil.parse("<#F0F4F8>  sql2yaml - Перенос данных из SQL в YAML"));
            sender.sendMessage(MessageUtil.parse("<#F0F4F8>  yaml2sqlite - Перенос данных из YAML в SQLite"));
            sender.sendMessage(MessageUtil.parse("<#F0F4F8>  sqlite2yaml - Перенос данных из SQLite в YAML"));
            return true;
        }

        String action = args[0].toLowerCase();
        MigrationManager migrationManager = plugin.getMigrationManager();

        if (migrationManager == null) {
            sender.sendMessage(MessageUtil.parse("<#FF3366>MigrationManager не инициализирован!"));
            return true;
        }

        if ("yaml2sql".equals(action) || "yaml2sqlite".equals(action)) {
            // Миграция из YAML в SQL (SQLite по умолчанию)
            sender.sendMessage(MessageUtil.parse("<#00C8FF>Начинаю миграцию данных из YAML в SQL..."));

            if (!migrationManager.hasDataInYaml()) {
                sender.sendMessage(MessageUtil.parse("<#FF3366>Данные YAML не найдены или пусты!"));
                return true;
            }

            // Переключаем на SQLITE если ещё не SQL
            String currentType = plugin.getConfigManager().getStorageType();
            if ("YAML".equals(currentType)) {
                plugin.getConfigManager().setStorageType("SQLITE");
                sender.sendMessage(MessageUtil.parse("<#00C8FF>✓ Хранилище переключено на SQLITE в config.yml"));
            }

            // Проверяем что БД подключена
            if (plugin.getDatabaseManager() == null || !plugin.getDatabaseManager().isInitialized()) {
                sender.sendMessage(MessageUtil.parse("<#FF3366>"));
                sender.sendMessage(MessageUtil.parse("<#FF3366>⚠ ТРЕБУЕТСЯ ПЕРЕЗАГРУЗКА СЕРВЕРА! ⚠"));
                sender.sendMessage(MessageUtil.parse("<#FF3366>"));
                sender.sendMessage(MessageUtil.parse("<#00C8FF>Выполните команду:"));
                sender.sendMessage(MessageUtil.parse("<#FF3366>  /reload</#FF3366> или <#FF3366>/restart"));
                sender.sendMessage(MessageUtil.parse("<#00C8FF>После перезагрузки снова введите:"));
                sender.sendMessage(MessageUtil.parse("<#FF3366>  /migrate yaml2sqlite"));
                sender.sendMessage(MessageUtil.parse("<#FF3366>"));
                return true;
            }

            // БД подключена — выполняем миграцию
            try {
                sender.sendMessage(MessageUtil.parse("<#00C8FF>Очистка SQL таблиц..."));
                migrationManager.clearSqlTables();

                sender.sendMessage(MessageUtil.parse("<#00C8FF>Миграция данных..."));
                migrationManager.migrateYamlToSql();

                sender.sendMessage(MessageUtil.parse("<#00C8FF>✓ Миграция в SQL завершена успешно!"));
                sender.sendMessage(MessageUtil.parse("<#00C8FF>✓ Все данные перенесены в базу данных"));
                sender.sendMessage(MessageUtil.parse("<#F0F4F8>Сервер готов к работе с SQL"));

            } catch (Exception e) {
                sender.sendMessage(MessageUtil.parse("<#FF3366>Ошибка при миграции: " + e.getMessage()));
                LoggerUtil.log(java.util.logging.Level.SEVERE, "Migration error", e);
                e.printStackTrace();
                return true;
            }

        } else if ("sql2yaml".equals(action) || "sqlite2yaml".equals(action)) {
            // Миграция из SQL в YAML
            sender.sendMessage(MessageUtil.parse("<#00C8FF>Начинаю миграцию данных из SQL в YAML..."));

            if (!migrationManager.hasDataInSql()) {
                sender.sendMessage(MessageUtil.parse("<#FF3366>Данные SQL не найдены или пусты!"));
                return true;
            }

            try {
                migrationManager.migrateSqlToYaml();
                plugin.getConfigManager().setStorageType("YAML");
                sender.sendMessage(MessageUtil.parse("<#00C8FF>✓ Миграция в YAML завершена успешно!"));
                sender.sendMessage(MessageUtil.parse("<#F0F4F8>Перезагрузите сервер для применения изменений."));

            } catch (Exception e) {
                sender.sendMessage(MessageUtil.parse("<#FF3366>Ошибка при миграции: " + e.getMessage()));
                LoggerUtil.log(java.util.logging.Level.SEVERE, "Migration error", e);
                e.printStackTrace();
                return true;
            }

        } else {
            sender.sendMessage(MessageUtil.parse("<#FF3366>Неизвестная команда! Используйте: yaml2sqlite, sqlite2yaml, yaml2sql, sql2yaml"));
            return true;
        }

        sender.sendMessage(MessageUtil.parse("<#F0F4F8>Проверьте консоль для подробностей."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            String partial = args[0].toLowerCase();
            
            if ("yaml2sql".startsWith(partial) || "yaml2sqlite".startsWith(partial)) {
                suggestions.add("yaml2sqlite");
            }
            if ("sql2yaml".startsWith(partial) || "sqlite2yaml".startsWith(partial)) {
                suggestions.add("sqlite2yaml");
            }
            
            return suggestions;
        }
        return Collections.emptyList();
    }
}
