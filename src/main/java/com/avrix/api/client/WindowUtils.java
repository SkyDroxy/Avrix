package com.avrix.api.client;

import org.lwjglx.opengl.Display;
import org.lwjglx.opengl.DisplayMode;

import zombie.core.Core;

/**
 * A set of tools for managing the game window
 */
public class WindowUtils {
    /**
     * Returns the height of the window.
     *
     * @return The height of the window.
     */
    public static int getWindowHeight() {
        return Core.getInstance().getScreenHeight();
    }

    /**
     * Returns the width of the window.
     *
     * @return The width of the window.
     */
    public static int getWindowWidth() {
        return Core.getInstance().getScreenWidth();
    }

    /**
     * Apply display settings provided via system properties set by the launcher.
     * Recognized properties:
     * - avrix.fullscreen: boolean (true/false)
     * - avrix.width: integer pixels
     * - avrix.height: integer pixels
     *
     * This method is safe to call multiple times; failures are caught and ignored.
     */
    public static void applyDisplaySettingsFromSystemProperties() {
        String fsProp = System.getProperty("avrix.fullscreen");
        String wProp = System.getProperty("avrix.width");
        String hProp = System.getProperty("avrix.height");

        // Nothing to do if no properties provided
        if (fsProp == null && wProp == null && hProp == null) return;

        boolean fullscreen = fsProp != null && (fsProp.equalsIgnoreCase("true") || fsProp.equals("1") || fsProp.equalsIgnoreCase("yes"));

        int width = -1;
        int height = -1;
        try {
            if (wProp != null) width = Integer.parseInt(wProp.trim());
        } catch (NumberFormatException ignored) {}
        try {
            if (hProp != null) height = Integer.parseInt(hProp.trim());
        } catch (NumberFormatException ignored) {}

        try {
            if (fullscreen) {
                // If a specific fullscreen resolution was provided, set it before toggling fullscreen
                if (width > 0 && height > 0) {
                    Display.setDisplayMode(new DisplayMode(width, height));
                }
                Display.setFullscreen(true);
            } else {
                // Ensure windowed mode first, then set size if provided
                Display.setFullscreen(false);
                if (width > 0 && height > 0) {
                    Display.setDisplayMode(new DisplayMode(width, height));
                }
            }
        } catch (Throwable t) {
            // Be defensive: if anything goes wrong, avoid crashing the game. Optionally print for debugging.
            t.printStackTrace();
        }
    }
}