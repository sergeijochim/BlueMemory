package de.fhgiessen.mni.bluememory.server;


/**
 * Schnittstelle zur Kommunikation mit einem Client des Spiels.
 * 
 * @author Timo Ebel
 */
public interface ServerStrategie {
	/**
	 * Sendet dem Client das Signal, dass der Name OK ist.
	 */
	public void sendeHello();
	
	/**
	 * Sendet dem Client das Signal, dass der Name bereits belegt ist.
	 */
	public void sendeFehlerHelo();
	
	/**
	 * Sendet dem Client das Signal, dass ein neuer Spieler die Lobby betreten hat.
	 * 
	 * @param spieler Der Name des neuen Spielers.
	 */
	public void sendeNeuerSpieler(String spieler);
	
	/**
	 * Sendet dem Client das Signal, dass ein Spieler die Lobby verlassen hat.
	 * 
	 * @param spieler Der Name des Spielers, der die Lobby verlassen hat.
	 */
	public void sendeSpielerWeg(String spieler);
	
	/**
	 * Sendet dem Client die Lobby.
	 */
	public void sendeLobby();
	
	/**
	 * Sendet dem Client das Signal zum Starten des Spiels.
	 */
	public void sendeSpielStarten();
	
	/**
	 * Sendet dem Client das Spielfeld.
	 */
	public void sendeSpielfeld();
	
	/**
	 * Sendet dem Client den Namen des Spielers, der am Zug ist.
	 * 
	 * @param spieler Der Spieler, der am Zug ist.
	 */
	public void sendeRate(String spieler);
	
	/**
	 * Sendet dem Client einen Zug, den ein Spieler gemacht hat.
	 * 
	 * @param zug Die Position der Karte, die aufgedeckt wurde (als String)
	 */
	public void sendeZug(String zug);
	
	/**
	 * Sendet dem Client das Signal, dass das Spiel zuende ist.
	 */
	public void sendeBeenden();
	
	/**
	 * Setzt den Namen des Spielers, den diese Verbindung repräsentiert.
	 * 
	 * @param name Der Name, der gesetzt werden soll.
	 */
	public void setName(String name);
	
	/**
	 * Trennt die Verbindung zum Client.
	 */
	public void trenneVerbindung();
	
	/**
	 * Die String-Repräsentation einer Verbindung ist der Name des Spielers, den sie repräsentiert.
	 * 
	 * @return Der Name des Spielers.
	 */
	public String toString();
}