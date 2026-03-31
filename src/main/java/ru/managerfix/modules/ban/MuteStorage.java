package ru.managerfix.modules.ban;

import java.util.Optional;
import java.util.UUID;

public interface MuteStorage {
    Optional<MuteRecord> getMute(UUID uuid);
    void addMuteAsync(MuteRecord record, Runnable onDone);
    void removeMuteAsync(UUID uuid, Runnable onDone);
    void init();
    void shutdown();
}
