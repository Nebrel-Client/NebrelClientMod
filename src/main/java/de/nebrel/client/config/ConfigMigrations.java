package de.nebrel.client.config;

import com.google.gson.JsonObject;

/**
 * Upgrades config objects written by older versions of the client.
 *
 * <p>The rules are deliberately simple and additive:</p>
 * <ul>
 *   <li>A key that is missing is not an error. The setting keeps its default.</li>
 *   <li>A key that is no longer known is left in the object and ignored on
 *       read. It is dropped the next time the file is written, but only after a
 *       successful load, so a downgrade-then-upgrade round trip does not lose
 *       anything the user configured.</li>
 *   <li>A renamed key gets an explicit step here.</li>
 * </ul>
 *
 * <p>Version 0 means "written before versioning existed"; it is treated as
 * version 1 shaped, since 1 is the first released format.</p>
 */
public final class ConfigMigrations {

    private ConfigMigrations() {
    }

    /**
     * Runs every step between {@code from} and {@code to}.
     *
     * @param fileName the config file, so a step can be file specific
     * @param root     the parsed object, mutated in place
     * @return the migrated object
     */
    public static JsonObject migrate(String fileName, JsonObject root, int from, int to) {
        if (from > to) {
            // Written by a newer client. Read it as best we can rather than
            // wiping the user's settings; unknown keys are ignored anyway.
            ConfigLog.warn(fileName + " was written by a newer version of Nebrel Client"
                    + " (format " + from + " > " + to + "); unknown entries will be ignored");
            return root;
        }

        int version = from;
        while (version < to) {
            root = step(fileName, root, version);
            version++;
        }
        return root;
    }

    /** Applies the single step that takes {@code version} to {@code version + 1}. */
    private static JsonObject step(String fileName, JsonObject root, int version) {
        // No released format below 1 yet, so 0 to 1 is a straight adoption.
        // Future steps go here, each one small and named:
        //
        //   if (version == 1) { renameKey(root, "modules.oldId", "modules.newId"); }
        //
        return root;
    }

    /** Helper for future steps: moves a value from one key to another. */
    @SuppressWarnings("unused")
    private static void renameKey(JsonObject object, String oldKey, String newKey) {
        if (object.has(oldKey) && !object.has(newKey)) {
            object.add(newKey, object.get(oldKey));
        }
        object.remove(oldKey);
    }
}
