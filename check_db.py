import mysql.connector

# Подключение к БД
conn = mysql.connector.connect(
    host="mysql2.joinserver.xyz",
    port=3306,
    user="u382208_U9HFd7nG7f",
    password="70zp0blQM^TI6@jEcPxghpue",
    database="s382208_manager_fix"
)

cursor = conn.cursor()

print("=== ТАБЛИЦЫ В БД ===")
cursor.execute("SHOW TABLES")
for table in cursor.fetchall():
    print(f"  - {table[0]}")

print("\n=== СТРУКТУРА TABLE `kits` ===")
cursor.execute("DESCRIBE kits")
for col in cursor.fetchall():
    print(f"  {col[0]}: {col[1]} {col[3]} {col[4]}")

print("\n=== ДАННЫЕ В `kits` ===")
cursor.execute("SELECT name, cooldown, permission, priority, one_time, icon_material FROM kits")
kits = cursor.fetchall()
if kits:
    for kit in kits:
        print(f"  - {kit[0]}: КД={kit[1]}, Приоритет={kit[3]}, OneTime={kit[4]}, Иконка={kit[5]}")
else:
    print("  (пусто)")

print("\n=== ПРОВЕРКА ПОДКЛЮЧЕНИЯ ===")
cursor.execute("SELECT VERSION()")
print(f"MySQL версия: {cursor.fetchone()[0]}")

cursor.close()
conn.close()

print("\n✓ Всё OK!")
