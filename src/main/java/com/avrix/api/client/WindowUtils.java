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
     * - avrix.windowMode: 'fullscreen' | 'windowed' | 'borderless'
     * - avrix.fullscreen: boolean (legacy; used to derive windowMode if missing)
     * - avrix.width: integer pixels (windowed only)
     * - avrix.height: integer pixels (windowed only)
     *
     * This method is safe to call multiple times; failures are caught and ignored.
     */
    public static void applyDisplaySettingsFromSystemProperties() {
        final String fsProp = System.getProperty("avrix.fullscreen");
        final String wmProp = System.getProperty("avrix.windowMode");
        final String wProp = System.getProperty("avrix.width");
        final String hProp = System.getProperty("avrix.height");

        // Nothing to do if no properties provided
        if (fsProp == null && wmProp == null && wProp == null && hProp == null) return;

        final boolean legacyFullscreen = fsProp != null && (fsProp.equalsIgnoreCase("true") || fsProp.equals("1") || fsProp.equalsIgnoreCase("yes"));
        final String windowMode = wmProp != null ? wmProp : (legacyFullscreen ? "fullscreen" : "windowed");

        int width = -1;
        int height = -1;
        try { if (wProp != null) width = Integer.parseInt(wProp.trim()); } catch (NumberFormatException ignored) {}
        try { if (hProp != null) height = Integer.parseInt(hProp.trim()); } catch (NumberFormatException ignored) {}

        try {
            if ("fullscreen".equalsIgnoreCase(windowMode)) {
                // Fullscreen uses the current system/display resolution.
                Display.setFullscreen(true);
            } else if ("borderless".equalsIgnoreCase(windowMode)) {
                // Borderless windowed: size to desktop resolution and place at (0,0), keep windowed mode.
                Display.setFullscreen(false);
                try {
                    DisplayMode dm = Display.getDesktopDisplayMode();
                    if (dm != null) {
                        Display.setDisplayMode(new DisplayMode(dm.getWidth(), dm.getHeight()));
                    }
                } catch (Exception ignored) {}
                try {
                    Display.setLocation(0, 0);
                    Display.setResizable(false);
                } catch (Exception ignored) {}
            } else { // windowed
                // Windowed mode: apply provided width/height if valid, then ensure windowed state.
                if (width > 0 && height > 0) {
                    try {
                        Display.setDisplayMode(new DisplayMode(width, height));
                    } catch (Exception ignored) {}
                }
                Display.setFullscreen(false);
            }
        } catch (Exception ignored) {
            // Avoid crashing the game if applying settings fails on this platform/runtime.
        }
    }
}