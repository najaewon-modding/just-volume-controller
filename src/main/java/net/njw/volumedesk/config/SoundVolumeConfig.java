package net.njw.volumedesk.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public final class SoundVolumeConfig {
    public static final int MIN_VOLUME_PERCENT = 0;
    public static final int MAX_VOLUME_PERCENT = 100;
    public static final int DEFAULT_VOLUME_PERCENT = 100;

    private static final int FORMAT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Path path;
    private volatile Map<Identifier, Integer> changedVolumes = Map.of();

    public SoundVolumeConfig(Path path) {
        this.path = Objects.requireNonNull(path).toAbsolutePath().normalize();
    }

    public synchronized void load() {
        if (Files.notExists(path)) {
            changedVolumes = Map.of();
            return;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement rootElement = JsonParser.parseReader(reader);

            if (!rootElement.isJsonObject()) {
                throw new JsonParseException("Expected a JSON object");
            }

            changedVolumes = Map.copyOf(readVolumes(rootElement.getAsJsonObject()));
        } catch (IOException | JsonParseException | IllegalStateException exception) {
            LOGGER.error("Failed to load Volume Desk sound volume config from {}", path, exception);
        }
    }

    public synchronized boolean save() {
        JsonObject root = new JsonObject();
        JsonObject volumes = new JsonObject();

        new TreeMap<>(changedVolumes).forEach((soundId, percent) ->
                volumes.addProperty(soundId.toString(), percent)
        );

        root.addProperty("formatVersion", FORMAT_VERSION);
        root.add("volumes", volumes);

        Path temporaryPath = path.resolveSibling(path.getFileName() + ".tmp");

        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Files.writeString(
                    temporaryPath,
                    GSON.toJson(root) + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            replaceFile(temporaryPath, path);
            return true;
        } catch (IOException exception) {
            LOGGER.error("Failed to save Volume Desk sound volume config to {}", path, exception);

            try {
                Files.deleteIfExists(temporaryPath);
            } catch (IOException cleanupException) {
                LOGGER.warn("Failed to remove temporary Volume Desk config file {}", temporaryPath, cleanupException);
            }

            return false;
        }
    }

    public int getVolumePercent(Identifier soundId) {
        return changedVolumes.getOrDefault(Objects.requireNonNull(soundId), DEFAULT_VOLUME_PERCENT);
    }

    public float getVolumeMultiplier(Identifier soundId) {
        return getVolumePercent(soundId) / 100.0F;
    }

    public synchronized void setVolumePercent(Identifier soundId, int percent) {
        Objects.requireNonNull(soundId);

        if (!isValidPercent(percent)) {
            throw new IllegalArgumentException("Volume percent must be between 0 and 100: " + percent);
        }

        Map<Identifier, Integer> updatedVolumes = new HashMap<>(changedVolumes);

        if (percent == DEFAULT_VOLUME_PERCENT) {
            updatedVolumes.remove(soundId);
        } else {
            updatedVolumes.put(soundId, percent);
        }

        changedVolumes = Map.copyOf(updatedVolumes);
    }

    public synchronized void resetAll() {
        changedVolumes = Map.of();
    }

    public Map<Identifier, Integer> getChangedVolumes() {
        return changedVolumes;
    }

    public Path getPath() {
        return path;
    }

    private static Map<Identifier, Integer> readVolumes(JsonObject root) {
        JsonElement volumesElement = root.get("volumes");

        if (volumesElement == null) {
            return Map.of();
        }

        if (!volumesElement.isJsonObject()) {
            throw new JsonParseException("Expected volumes to be a JSON object");
        }

        Map<Identifier, Integer> volumes = new HashMap<>();

        for (Map.Entry<String, JsonElement> entry : volumesElement.getAsJsonObject().entrySet()) {
            Identifier soundId = Identifier.tryParse(entry.getKey());
            Integer percent = readPercent(entry.getValue());

            if (soundId == null || percent == null || percent == DEFAULT_VOLUME_PERCENT) {
                LOGGER.warn("Ignoring invalid or default Volume Desk setting: {}", entry.getKey());
                continue;
            }

            volumes.put(soundId, percent);
        }

        return volumes;
    }

    private static Integer readPercent(JsonElement element) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return null;
        }

        try {
            int percent = Integer.parseInt(element.getAsString());
            return isValidPercent(percent) ? percent : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static boolean isValidPercent(int percent) {
        return percent >= MIN_VOLUME_PERCENT && percent <= MAX_VOLUME_PERCENT;
    }

    private static void replaceFile(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
