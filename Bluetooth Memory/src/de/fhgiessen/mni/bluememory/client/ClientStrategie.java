package de.fhgiessen.mni.bluememory.client;


/**
 * Schnittstelle zur Kommunikation mit dem Server-Service des Spiels.
 * 
 * Klassen, die diese Schnittstelle implementieren die Methoden, mit denen ein
 * Spiel-Client mit dem Server-Service kommuniziert. Sie sollten als Singleton
 * implementiert werden.
 * 
 * @author Timo Ebel
 */
public interface ClientStrategie {
	/** Request-Action-Code zum Aktivieren des Bluetooth-Adapter */
	public static final int REQUEST_ENABLE_BT = 1;
	
	/** Name des Intent-Extras, welches die Informationen über ein Spiel enthält */
	public static final String EXTRA_SPIEL = "spiel";
	
	/** Dialog-Fenster-Code für einen Fehler beim Abrufen der Liste der Spiele. */
	public static final int FEHLER_SPIELELISTE = 1;
	
	/** Dialog-Fenster-Code für einen Fehler bei der Anmeldung (Name bereits belegt). */
	public static final int FEHLER_HELO = 2;

	/** Dialog-Fenster-Code für einen Fehler bei der Übertragung der Lobby. */
	public static final int FEHLER_LOBBY = 3;

	/** Dialog-Fenster-Code für einen Fehler bei der Übertragung des Spielfelds. */
	public static final int FEHLER_SPIELFELD = 4;

	/** Dialog-Fenster-Code für das Spielende. */
	public static final int SPIEL_ZUENDE = 5;

	/**
	 * Callback-Methode, die gerufen wird, sobald ein Spiel in der SpielTeilnehmen-Activity gewählt wurde.
	 * 
	 * Hier können Aufräumarbeiten erledigt werden, bspw. das Auffinden von BT-Geräten beendet
	 * werden, bevor die Lobby-Activity gestartet wird.
	 */
	public void onSpielGewaehlt();

	/**
	 * Sucht die vorhandenen Spiele oder Geräte (bei BT) in der Umgebung.
	 * 
	 * Die Liste der gefundenen Spiele oder Geräte wird per onSpieleListeEmpfangen()
	 * an die aktive Activity übermittelt. Werden weitere Spiele oder Geräte gefunden,
	 * werden diese über onNeuesSpielEmpfangen() an die aktive Activity übermittelt.
	 */
	public void getSpieleListe();

	/**
	 * Initiiert die Verbindung mit einem Spiel-Server.
	 * 
	 * @param spiel Der Name des Spiels, mit dem verbunden werden soll.
	 */
	public void verbinde(String spiel);

	/**
	 * Trennt die Verbindung mit einem Spiel-Server.
	 */
	public void trenneVerbindung();

	/**
	 * Sendet den HELO-Statuscode und den Namen an den Server.
	 * 
	 * @param name Name des Spielers.
	 */
	public void sendeHelo(String name);

	/**
	 * Holt die Spielerliste vom Server.
	 */
	public void getLobby();
	
	/**
	 * Teilt dem Server mit, dass der Empfang der Lobby erfolgreich war.
	 */
	public void sendeLobbyOK();
	
	/**
	 * Teilt dem Server mit, dass das Spiel startet.
	 */
	public void sendeSpielStarten();

	/**
	 * Fordert das Spielfeld vom Server an.
	 */
	public void getSpielfeld();

	/**
	 * Teilt dem Server mit, dass der Empfang des Spielfelds erfolgreich war.
	 */
	public void sendeSpielfeldOK();
	
	/**
	 * Sendet einen Zug, den der Client gemacht hat, zum Server.
	 * 
	 * @param zug Die Nummer der angeklickten Karte.
	 */
	public void sendeZug(int zug);
	
	/**
	 * Teilt dem Server mit, dass der Spielzug empfangen und umgesetzt wurde.
	 */
	public void sendeZugOK();
	
	/**
	 * Sendes den BYE-Statuscode an den Server.
	 */
	public void sendeBye();
	
	/**
	 * Setzt die Activity an die Nachrichten zurückübermittelt werden sollen.
	 * 
	 * Es sollte immer die aktuell im Vordergrund befindliche Activity gesetzt sein.
	 * Deren Callback-Methoden werden zur Rückübermittlunh der Nachrichten verwendet.
	 * 
	 * @param aktuelleActivity Die Activity, die die Nachrichten erwartet.
	 */
	public void setActivity(MemoryActivity aktuelleActivity);
}