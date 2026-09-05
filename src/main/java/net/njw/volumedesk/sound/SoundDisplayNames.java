package net.njw.volumedesk.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public final class SoundDisplayNames {
    private static final String TRANSLATION_PREFIX = "sound_tree.njw_volume_desk.";

    private SoundDisplayNames() {
    }

    public static String label(SoundTree.Node node, String nodeKey) {
        if (node.soundId().isPresent()) {
            String subtitle = subtitle(node.soundId().get());

            if (subtitle != null) {
                return subtitle;
            }
        }

        return structuralLabel(node, nodeKey);
    }

    public static String searchText(SoundTree.Node node, String nodeKey) {
        String structuralLabel = structuralLabel(node, nodeKey);

        if (node.soundId().isEmpty()) {
            return structuralLabel;
        }

        String subtitle = subtitle(node.soundId().get());
        return subtitle == null ? structuralLabel : subtitle + " " + structuralLabel;
    }

    private static String structuralLabel(SoundTree.Node node, String nodeKey) {
        int colon = nodeKey.indexOf(':');
        String namespace = colon < 0 ? nodeKey : nodeKey.substring(0, colon);
        String path = colon < 0 ? "" : nodeKey.substring(colon + 1);

        if (path.isEmpty()) {
            return translateOrFallback(TRANSLATION_PREFIX + "namespace." + namespace, humanize(node.segment()));
        }

        String exactKey = TRANSLATION_PREFIX + "path." + namespace + "." + path;

        if (I18n.exists(exactKey)) {
            return I18n.get(exactKey);
        }

        String[] segments = path.split("\\.");

        if (segments.length == 2 && isContentType(segments[0])) {
            String contentKey = segments[0] + "." + namespace + "." + segments[1];

            if (I18n.exists(contentKey)) {
                return I18n.get(contentKey);
            }
        }

        return translateOrFallback(
                TRANSLATION_PREFIX + "segment." + node.segment(),
                humanize(node.segment())
        );
    }

    private static String subtitle(Identifier soundId) {
        WeighedSoundEvents soundEvent = Minecraft.getInstance().getSoundManager().getSoundEvent(soundId);

        if (soundEvent == null) {
            return null;
        }

        Component subtitle = soundEvent.getSubtitle();

        if (subtitle == null) {
            return null;
        }

        String translated = subtitle.getString().trim();
        return translated.isEmpty() || translated.startsWith("subtitles.") ? null : translated;
    }

    private static boolean isContentType(String segment) {
        return segment.equals("block") || segment.equals("entity") || segment.equals("item");
    }

    private static String translateOrFallback(String key, String fallback) {
        return I18n.exists(key) ? I18n.get(key) : fallback;
    }

    private static String humanize(String segment) {
        return segment.replace('_', ' ').replace('-', ' ').toLowerCase(Locale.ROOT);
    }
}
