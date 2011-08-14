package de.fhgiessen.mni.bluememory.datentypen;

/**
 * Die Status-Codes des Spielprotokolls für "BlueMemory".
 * 
 * @author Sergei Jochim
 */
public class StatusCodes {

	//Idee
	//	0-19:  Verbindungsaufbau / Verbindungstrennung
	//	20-39: Kommunikation Lobby
	//	40-69: Spielablauf
	
	/*
	 * Verbindungsaufbau / Verbindungstrennung
	 */
	/** Der Client möchte sich mit seinem Namen anmelden. Parameter: Spielername (Client). */
	public static final byte HELO = 0;
	
	/** Der Spielername wurde akzeptiert. Keine Parameter. */
	public static final byte HELLO = 1;
	
	/** Der Spielername wurde nicht akzeptiert. Keine Parameter. */
	public static final byte FEHLER_HELO = 2;
	
	/** Beenden der Verbindung. */
	public static final byte BYE = 10;
	
	
	/*
	 * Lobby
	 */
	/** Der Client fordert die Spielerliste aus der Lobby an. Keine Parameter. */
	public static final byte GET_LOBBY = 20;
	
	/** Die Spieler aus der Lobby werden gesendet. Parameter: Die Spielerliste (JSON-Objekt). */
	public static final byte POST_LOBBY = 21;
	
	/** Die Liste der Spieler wurde korrekt empfangen. Keine Parameter. */
	public static final byte OK_LOBBY = 22;
	
	/** Ein neuer Spieler hat die Lobby betreten. Parameter: Name des Spielers. */
	public static final byte PLAYER_JOINED = 25;
	
	/** Ein Spieler hat die Lobby verlassen. Parameter: Name des Spielers. */
	public static final byte PLAYER_LEFT = 26;
	
	/** Server hat Spiel gestartet. Keine Parameter. */
	public static final byte STARTEN = 27;
	
	
	/*
	 * Spielablauf
	 */
	// Spielfeldübermittlung
	/** Der Client fordert das Spielfeld an. Keine Parameter. */
	public static final byte GET_SPIELFELD = 40;
	
	/** Das Spielfeld wird gesendet. Parameter: Das Spielfeld (JSON-Objekt). */
	public static final byte POST_SPIELFELD = 41;
	
	/** Spielfeld empfangen. Keine Parameter. */
	public static final byte OK_SPIELFELD = 42;
	
	// Spielzüge
	/** Der Client ist am Zug. Parameter: Name des Spielers, der am Zug ist. */
	public static final byte RATE = 50;
	
	/** Der Client sperrt das Spielfeld und wartet auf weitere Anweisungen. */
	public static final byte WARTE = 51;
	
	/** Der Client hat einen Zug gemacht. Parameter: Die Nummer der Karte, die aufgedeckt wurde. */
	public static final byte ZUG = 52;
	
	/** Der Zug eines Clients wird an die anderen Clients übertragen. Parameter: Die Nummer der Karte, die aufgedeckt wurde. */
	public static final byte POST_ZUG = 53;
	
	/** Der Spielzug wurde vom Client empfangen und umgesetzt, das Spiel kann weitergehen. Keine Parameter. */
	public static final byte OK_ZUG = 54;
	
	// Beenden
	/** Das Spiel wird beendet. Keine Parameter */
	public static final byte BEENDEN = 60;
}