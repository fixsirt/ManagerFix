#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Скрипт для настройки таблицы kits в MySQL для ManagerFix
"""

import mysql.connector
from mysql.connector import Error
import sys

# Настройки подключения
DB_CONFIG = {
    'host': 'mysql2.joinserver.xyz',
    'port': 3306,
    'database': 's382208_manager_fix',
    'user': 'u382208_U9HFd7nG7f',
    'password': '70zp0blQM^TI6@jEcPxghpue'
}

def connect():
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        print("[OK] Подключение к MySQL успешно!")
        return conn
    except Error as e:
        print(f"[ERROR] Ошибка подключения: {e}")
        return None

def check_table_structure(cursor):
    print("\n[INFO] Проверка структуры таблицы kits...")
    
    cursor.execute("DESCRIBE kits")
    columns = {row[0]: row for row in cursor.fetchall()}
    
    print(f"   Найдено колонок: {len(columns)}")
    for col_name in columns:
        print(f"   - {col_name}")
    
    return columns

def add_missing_columns(cursor, columns):
    print("\n[INFO] Проверка колонок...")
    
    if 'priority' not in columns:
        print("   Добавляем колонку priority...")
        cursor.execute("ALTER TABLE kits ADD COLUMN priority INT DEFAULT 0")
        print("   [OK] priority добавлена")
    else:
        print("   [OK] priority существует")
    
    if 'one_time' not in columns:
        print("   Добавляем колонку one_time...")
        cursor.execute("ALTER TABLE kits ADD COLUMN one_time BOOLEAN DEFAULT FALSE")
        print("   [OK] one_time добавлена")
    else:
        print("   [OK] one_time существует")
    
    if 'icon_material' not in columns:
        print("   Добавляем колонку icon_material...")
        cursor.execute("ALTER TABLE kits ADD COLUMN icon_material VARCHAR(100)")
        print("   [OK] icon_material добавлена")
    else:
        print("   [OK] icon_material существует")

def check_kits(cursor):
    print("\n[INFO] Проверка китов...")
    
    cursor.execute("SELECT COUNT(*) FROM kits")
    count = cursor.fetchone()[0]
    print(f"   Найдено китов: {count}")
    
    if count > 0:
        cursor.execute("SELECT name, cooldown, permission, priority, one_time FROM kits")
        print("\n   Список китов:")
        for row in cursor.fetchall():
            name, cooldown, perm, priority, one_time = row
            cooldown_fmt = f"{cooldown // 3600}ч {cooldown % 3600 // 60}м" if cooldown > 0 else "Нет"
            one_time_str = "[OK] Одноразовый" if one_time else "[  ] Многократный"
            print(f"   - {name}: КД={cooldown_fmt}, Приоритет={priority}, {one_time_str}")
    else:
        print("\n   [WARN] Таблица пустая! Создаём тестовый кит...")
        create_test_kit(cursor)

def create_test_kit(cursor):
    try:
        cursor.execute("""
            INSERT INTO kits (name, cooldown, permission, items, priority, one_time)
            VALUES ('test', 3600, 'managerfix.kits.kit.test', '[]', 0, FALSE)
        """)
        print("   [OK] Тестовый кит 'test' создан!")
        print("   КД: 1 час, Приоритет: 0, Многократный")
    except Error as e:
        print(f"   [ERROR] Ошибка создания кита: {e}")

def main():
    print("=" * 50)
    print("ManagerFix - Настройка таблицы kits")
    print("=" * 50)
    
    conn = connect()
    if not conn:
        sys.exit(1)
    
    try:
        cursor = conn.cursor()
        
        columns = check_table_structure(cursor)
        add_missing_columns(cursor, columns)
        conn.commit()
        
        check_kits(cursor)
        conn.commit()
        
        print("\n" + "=" * 50)
        print("[OK] Настройка завершена успешно!")
        print("=" * 50)
        print("\nТеперь в игре:")
        print("  /kits - открыть GUI с китами")
        print("  /editkits - редактировать киты")
        print("  /kit create <name> - создать кит")
        
    except Error as e:
        print(f"\n[ERROR] Ошибка: {e}")
    finally:
        if conn.is_connected():
            cursor.close()
            conn.close()
            print("\n[OK] Соединение закрыто")

if __name__ == "__main__":
    try:
        import mysql.connector
        main()
    except ImportError:
        print("[ERROR] Установите mysql-connector-python:")
        print("  pip install mysql-connector-python")
