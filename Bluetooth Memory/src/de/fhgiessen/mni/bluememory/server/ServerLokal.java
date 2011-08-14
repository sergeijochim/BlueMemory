package de.fhgiessen.mni.bluememory.server;


import de.fhgiessen.mni.bluememory.client.ClientLokal;

/**
 * Kommunikations-Strategie für den Memory-Server zum lokalen Benutzer
 * 
 * @author Sergei Jochim
 *
 */
public class ServerLokal implements ServerStrategie {
	/** Der Name des Spielers, den diese Verbindung repräsentiert */
	private String name;
	
	/** Die Client-Kommunikations-Strategie des lokalen Spielers */
	private ClientLokal komm;
	
	/**
	 * Konstruktor
	 * 
	 * Holt sich das globale ClientLokal-Objekt.
	 */
	public ServerLokal() {
		komm = (ClientLokal) ClientLokal.getInstance();
	}
	
	@Override
	public void sendeBeenden() {
		komm.aktuelleActivity.onSpielZuende();
	}

	@Override
	public void sendeFehlerHelo() {
		komm.aktuelleActivity.onSpielernameBelegt();
	}

	@Override
	public void sendeHello() {
		komm.aktuelleActivity.onSpielerNameOk();
	}

	@Override
	public void sendeLobby() {
		// ClientLokal holt sich die Lobby selbst
	}

	@Override
	public void sendeNeuerSpieler(String spieler) {
		komm.aktuelleActivity.onNeuerSpieler(spieler);
	}

	@Override
	public void sendeRate(String spieler) {
		komm.aktuelleActivity.onRate(spieler);
	}

	@Override
	public void sendeSpielerWeg(String spieler) {
		komm.aktuelleActivity.onSpielerWeg(spieler);
	}

	@Override
	public void sendeSpielfeld() {
		// ClientLokal holt sich das Spielfeld selbst.
	}

	@Override
	public void sendeSpielStarten() {
		komm.aktuelleActivity.onSpielStarten();
	}

	@Override
	public void sendeZug(String zug) {
		komm.aktuelleActivity.onSpielzugEmpfangen(Integer.parseInt(zug));
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void trenneVerbindung() {
		// Bei lokaler Kommunikation gibt es nichts zutun.
	}

	@Override
	public String toString() {
		return name;
	}
}