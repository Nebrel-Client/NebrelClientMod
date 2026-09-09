package de.nebrel.client.plus.server;

import de.nebrel.client.plus.profile.NebrelPlayerProfile;

import java.util.UUID;

/**
 * The seam where a future server-side integration would plug in.
 *
 * <h2>Why this interface exists empty</h2>
 *
 * <p>There is a limit worth being completely clear about, because it is easy to
 * promise past it:</p>
 *
 * <p><b>A client mod can only change what its own client draws.</b> Nebrel+
 * badges appear for players running Nebrel Client. A player on a vanilla client,
 * or on any other client, sees the ordinary name and always will — no client
 * mod can reach into another player's game and add a render element to it.</p>
 *
 * <p>Making a badge visible to everyone is therefore not a client feature at
 * all. It needs something server-side to put the prefix into the data the
 * server already sends every client: a scoreboard team prefix, a display name,
 * a tab list entry, a chat format. That is a separate project — a Paper,
 * Velocity or Fabric server plugin — and it is deliberately not part of this
 * one.</p>
 *
 * <p>What lives here is only the boundary that project would implement, so the
 * client is already shaped to hand off to it. No implementation ships, nothing
 * calls it yet, and nothing in Nebrel modifies any server.</p>
 */
public interface ServerIdentityBridge {

    /** Short name of the bridge, for diagnostics. */
    String name();

    /** Whether a compatible server integration has been detected. */
    boolean available();

    /**
     * Announces the local player's profile so the server can mirror it.
     *
     * <p>The server would then own the parts a vanilla client can see: the
     * scoreboard team prefix, the tab list name and the chat format.</p>
     */
    default void publishLocalProfile(NebrelPlayerProfile profile) {
    }

    /**
     * A profile the server supplied for another player.
     *
     * <p>The only way this client could learn that a <em>different</em> player
     * holds Nebrel+. Until a server integration exists, badges shown for other
     * players can only come from a Nebrel backend both clients trust.</p>
     *
     * @return null when the server has said nothing about this player
     */
    default NebrelPlayerProfile profileOf(UUID playerId) {
        return null;
    }

    /** A bridge that is never available; the default. */
    ServerIdentityBridge NONE = new ServerIdentityBridge() {
        @Override
        public String name() {
            return "None";
        }

        @Override
        public boolean available() {
            return false;
        }
    };
}
