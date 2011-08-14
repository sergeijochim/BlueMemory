package de.fhgiessen.mni.bluememory.datentypen;

import java.util.UUID;

/**
 * Globale Konstanten für das Spiel "BlueMemory".
 * 
 * @author Timo Ebel
 */
public abstract class GlobalValues {
	/** Titel-Zeilen-Prefix */
	public static final String TITEL = "BlueMemory - ";
	
	/** Name der SharedPrefs für die App */
	public static final String SHARED_PREFS_APP = "memory";

	/** Name der SharedPrefs mit den Decks */
	public static final String SHARED_PREFS_DECKS = "decks";
	
	/** Namde der SharedPrefs für die Statistik */
	public static final String STATS_SAVE_FILE = "GameStats";
	
	/** Name des Ordners, der die Decks enthält */
	public static final String DECKS_DIRECTORY = "decks";
	
	/** URL, von der neue Decks geladen werden können */
	public static final String DECK_URL = "http://www.weird-webdesign.de/memory/";
	
	/** Name der Listen-Datei, in der die herunterladbaren Decks stehen */
	public static final String DECK_LISTE = "list.txt";
	
	/** Intent-Action: Als Server starten */
	public static final String START_SERVER = "server";
	
	/** Intent-Action: Als Bluetooth-Client starten */
	public static final String START_CLIENT_BT = "client_bt";

	/** SDP-Name für die Bluetooth-Anwendung */
	public static final String SDP_NAME = "BlueMemory";
	
	/** UUID für die BT-Verbindung zwischen Server und Clients */
	public static final UUID BT_UUID = UUID.fromString("507e8240-bbba-49bc-830d-b1fef7008621");
	
	/** Minimale Anzahl von Spielern in einem Spiel */
	public static final int MIN_PLAYERS = 1;
	
	/** Maximale Anzahl von Spielern in einem Spiel */
	public static final int MAX_PLAYERS = 6;
}