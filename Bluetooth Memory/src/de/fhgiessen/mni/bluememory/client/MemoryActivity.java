package de.fhgiessen.mni.bluememory.client;

import java.util.List;

import de.fhgiessen.mni.bluememory.datentypen.Spielfeld;

/**
 * Callback-Interface für Activities des Memory-Spiels.
 * 
 * Activities des Memory-Spiels müssen einige Callback-Methoden bereithalten,
 * durch die die Client-Strategien ihre empfangenen Nachrichten zurückreichen
 * können. Diese werden hier definiert.
 * 
 * @author Timo Ebel
 */
public interface MemoryActivity {
	/**
	 * Initialisiert die Liste der verfügbaren Spiele.
	 * 
	 * @param spieleListe Liste der Spiele.
	 */
	public void onSpielelisteEmpfangen(List<String> spieleListe);
	
	/**
	 * Wird aufgerufen, wenn die Liste der verfügbaren Spiele nicht empfangen werden kann.
	 */
	public void onSpielelisteEmpfangenFehler();
	
	/**
	 * Aktualisiert die Liste der verfügbaren Spiele und fügt das Spiel hinzu.
	 * 
	 * @param spiel
	 */
	public void onNeuesSpielEmpfangen(String spiel);
	
	/**
	 * Wird aufgerufen, sobald die Bluetooth-Verbindung hergestellt wurde.
	 */
	public void onVerbunden();

	/**
	 * Wird aufgerufen, wenn ein Spieler die Lobby verlässt. (PLAYER_LEFT)
	 * 
	 * @param name Name des Spielers, der die Lobby verlässt.
	 */
	public void onSpielerWeg(String name);
	
	/**
	 * Wird aufgerufen, wenn das Betreten der Lobby erfolgreich war. (HELLO)
	 */
	public void onSpielerNameOk();
	
	/**
	 * Wird aufgerufen, wenn der Spielername, den der Client verwenden möchte, bereits belegt ist. (FEHLER_HELO)
	 */
	public void onSpielernameBelegt();

	/**
	 * Initialisiert das empfangene Spielfeld. (POST_SPIELFELD)
	 * 
	 * @param spielfeld Das Spielfeld.
	 */
	public void onSpielfeldEmpfangen(Spielfeld spielfeld);
	
	/**
	 * Wird aufgerufen, wenn das Empfangen des Spielfelds endgültig fehlgeschlagen ist.
	 * 
	 * Für diese Anwendung soll der Empfang 3 mal versucht werden, dann bricht das Spiel
	 * die Verbindung ab.
	 * 
	 * @param fehlerMeldung Die Fehlermeldung, die im Dialog-Feld ausgegeben werden soll oder null.
	 */
	public void onSpielfeldEmpfangenFehler(String fehlerMeldung);

	/**
	 * Wird aufgerufen, wenn die Liste der Spieler in der Lobby empfangen wurde. (POST_LOBBY)
	 * 
	 * @param spielerListe Die Liste der Spieler.
	 */
	public void onLobbyEmpfangen(List<String> spielerListe);

	/**
	 * Wird aufgerufen, wenn das Empfangen der Spielerliste endgültig fehlgeschlagen ist.
	 * 
	 * Für diese Anwendung soll der Empfang 3 mal versucht werden, dann bricht das Spiel
	 * die Verbindung ab.
	 */
	public void onLobbyEmpfangenFehler();
	
	/**
	 * Wird aufgerufen, wenn ein neuer Spieler die Lobby betritt. (PLAYER_JOINED)
	 * 
	 * @param name Name des neuen Spielers.
	 */
	public void onNeuerSpieler(String name);
	
	/**
	 * Wird aufgerufen, wenn der Server das Spiel startet. (STARTEN)
	 */
	public void onSpielStarten();
	
	/**
	 * Wird aufgerufen, wenn ein Spieler an der Reihe ist und seinen Zug machen soll. (RATE)
	 * 
	 * @param name Der Spieler, der an der Reihe ist. Die Activity prüft, ob sie selbst der Spieler ist.
	 */
	public void onRate(String name);
	
	/**
	 * Wird aufgerufen, wenn der Spielzug eines anderen Spielers empfangen wurde. (POST_ZUG)
	 * 
	 * @param zug Die Karte, die der Spieler angeklickt hat.
	 */
	public void onSpielzugEmpfangen(int zug);
	
	/**
	 * Wird aufgerufen, wenn alle Karten aufgedeckt wurden. (BEENDEN)
	 */
	public void onSpielZuende();
	
	/**
	 * Wird aufgerufen, wenn die Verbindung beendet wird. (BYE)
	 */
	public void onVerbindungBeendet();
	
	/**
	 * Wird aufgerufen, wenn der Client auf weitere Anweisungen warten soll. (WARTE)
	 */
	public void onWarten();
}