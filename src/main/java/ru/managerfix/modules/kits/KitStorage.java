package ru.managerfix.modules.kits;

import java.util.List;
import java.util.Optional;

/**
 * Storage for kit definitions (YAML or SQL). Kits are cached by KitManager.
 */
public interface KitStorage {

    Optional<KitData> loadKit(String name);

    void saveKit(KitData kit);

    void deleteKit(String name);

    List<String> listKitNames();

    void init();

    void shutdown();
}
