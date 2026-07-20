package com.github.jbescos;

import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.github.jbescos.RatassGame;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		DisplayMode desktopMode = Lwjgl3ApplicationConfiguration.getDisplayMode(
				Lwjgl3ApplicationConfiguration.getPrimaryMonitor());
		config.setForegroundFPS(60);
		config.useVsync(true);
		config.setHdpiMode(HdpiMode.Pixels);
		config.setFullscreenMode(desktopMode);
		config.setResizable(true);
		config.setTitle("Rogue Circuit");
		new Lwjgl3Application(new RatassGame(), config);
	}
}
