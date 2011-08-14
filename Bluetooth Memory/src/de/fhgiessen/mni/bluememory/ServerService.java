package de.fhgiessen.mni.bluememory;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import de.fhgiessen.mni.bluememory.datentypen.Spielfeld;
import de.fhgiessen.mni.bluememory.server.ServerBT;
import de.fhgiessen.mni.bluememory.server.ServerLokal;
import de.fhgiessen.mni.bluememory.server.ServerStrategie;

/**
 * Agiert als Server für das Memory-Spiel
 * 
 * Der Server-Service hält eine Liste von Verbindungen zu Spielern (repräsentiert durch
 * ServerStrategie), koordiniert den Spielablauf und verteilt oder bearbeitet die Nachrichten
 * an bzw. von den Spieler-Geräten.
 * 
 * Es gibt genau einen lokalen Spieler, welcher immer der erste Spieler in der Liste ist und
 * durch ein ServerLokal-Objekt repräsentiert wird. Alle anderen Spieler sind über ein entferntes
 * Gerät verbunden und werden durch eine andere Server-Strategie repräsentiert (in diesem Fall
 * ServerBT).
 * 
 * Der ServerService ist als Singleton implementiert, alle ServerStrategien können direkt mit dem
 * ServerService-Objekt kommunizieren. Die Singleton-Instanz wird mit den Lifecycle-Methoden
 * onCreate() und onDestroy() initialisiert oder entfernt und kann per getInstance() abgerufen
 * werden.
 * 
 * @author Sergei Jochim
 *
 */
public class ServerService extends Service {
	/** Logcat-Tag der Klasse */
	private static final String TAG = "BlueMemory.ServerService";
	
	/** Das ServerService-Singleton-Objekt */
	private static ServerService singleton = null;
	
	/** Der Socket, auf dem aktuell auf neue Spielern gewartet wird. */
	private ServerBT serverSocket;
	
	/** Die Nummer des zur Zeit aktiven Spielers */
	private int spielerAktiv;
	
	/** Zähler für die erfolgreich empfangenen Spielfelder */
	private int spielfeldOk;
	
	/** Zähler für die erfolgreich empfangenen Spielzüge */
	private int zugOk;
	
	/** Alle bisher getätigten Spielzüge */
	private int zuege;
	
	/** Hält fest, ob das Spiel schon gestartet wurde oder noch nicht */
	private boolean spielGestartet;
	
	/** Die mit dem Server verbundenen Spieler */
	public List<ServerStrategie> connections;
	
	/** Das aktuelle Spielfeld */
	public Spielfeld spielfeld;
	
	/** Die Höhe des Spielfelds des letzten Spiels */
	private int spielfeldHoehe;
	
	/** Die Breite des Spielfelds des letzten Spiels */
	private int spielfeldBreite;
	
	/** Der Deckname des letzten Spiels */
	private String spielfeldDeck;
	
	/** Die Pause zwischen zwei Spielzügen */
	private int spielfeldPause;
	
	/**
	 * Holt das globale Server-Service-Objekt.
	 * 
	 * @return Das Objekt.
	 */
	public static ServerService getInstance() {
		return singleton;
	}
	
	@Override
	public void onCreate() {
		Log.d(TAG, "========== onCreate() ==========");
		
		singleton = this;
		
		super.onCreate();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "========== onStart() ==========");
		
		// Spielfeld-Daten auswerten (falls vorhanden)
		if ((intent != null) && intent.hasExtra("breite") && intent.hasExtra("hoehe") && intent.hasExtra("deck")) {
			// Spieldaten holen
			spielfeldBreite = Integer.parseInt(intent.getStringExtra("breite"));
			spielfeldHoehe = Integer.parseInt(intent.getStringExtra("hoehe"));
			spielfeldDeck = intent.getStringExtra("deck");
			spielfeldPause = intent.getIntExtra("pause", 1000);
			
			Log.d(TAG, "Start mit Intent und Extras.");
		} else if (intent == null) {
			Log.d(TAG, "Start mit leerem Intent.");
		} else {
			Log.d(TAG, "Start mit Intent, aber ohne Extras.");
		}
		
		// Spiel ist noch nicht gestartet
		spielGestartet = false;
		
		// Spielfeld-OK-Counter resetten
		spielfeldOk = 0;
		
		// Verbindungsliste initialisieren
		connections = new ArrayList<ServerStrategie>();
		
		// Lokalen Spieler erstellen und in die Liste einfügen
		connections.add(new ServerLokal());
		
		// Ggf. Spielfeld generieren
		try {
			Log.d(TAG, "Spielfeld wird generiert . . .");
			spielfeld = Spielfeld.generate(this, spielfeldBreite, spielfeldHoehe, spielfeldDeck, spielfeldPause);
			Log.d(TAG, spielfeld.toJSON());
		} catch (FileNotFoundException e) {
			spielfeld = null;
		}
		
		// Suche nach neuen Spielern starten.
		sucheSpieler();
		
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "========== onDestroy() ==========");
		
		singleton = null;
		connections = null;
		spielfeld = null;
		serverSocket = null;
		
		ServerBT.cleanUp(this);
		
		super.onDestroy();
	}
	
	/**
	 * Erstellt einen Server-Socket, auf dem nach neuen Spielern gehört wird.
	 */
	public void sucheSpieler() {
		serverSocket = new ServerBT();
	}
	
	/**
	 * Veranlasst den Start des Spiels.
	 */
	public void onSpielStarten() {
		spielGestartet = true;
		
		// Hören auf neue Spieler beenden
		serverSocket.trenneVerbindung();
		serverSocket = null;
		
		// Startsignal an alle senden
		for (ServerStrategie conn: connections) conn.sendeSpielStarten();
	}
	
	/**
	 * Wird aufgerufen, wenn der Server-Socket einen neuen Spieler empfangen hat.
	 */
	public void onNeueVerbindung() {
		connections.add(serverSocket);
		sucheSpieler();
	}
	
	/**
	 * Wird aufgerufen, wenn die Verbindung aus irgendeinem Grund fehlgeschlagen ist.
	 */
	public void onVerbindungFehlgeschlagen() {
		Log.e(TAG, "Verbindung fehlgeschlagen!");
		
		ServerBT.cleanUp(this);
	}
	
	/**
	 * Wird aufgerufen, wenn ein neuer Spieler zum Spiel hinzugekommen ist.
	 * 
	 * @param name Der Name des neuen Spielers.
	 */
	public void onNeuerSpieler(ServerStrategie connection, String name) {
		// Prüfen, ob der Spielername schon belegt ist
		boolean frei = true;
		for (ServerStrategie conn: connections) {
			if ((conn.toString() != null) && conn.toString().equals(name)) frei = false;
		}
		
		if (frei) { // Spielername ist noch frei
			if (connection == null) { // Lokaler Client
				connections.get(0).setName(name);
				connections.get(0).sendeHello();
			} else { // Remote Client
				connection.setName(name);
				connection.sendeHello();
				
				// Den anderen Clients den neuen Mitspieler zeigen
				for (ServerStrategie conn: connections)
					if (!conn.equals(connection)) conn.sendeNeuerSpieler(name);
			}
		} else { // Spielername nicht frei (geht nur bei Remote)
			connection.sendeFehlerHelo();
			return;
		}
	}
	
	/**
	 * Wird aufgerufen, wenn ein Spieler das Spielfeld erfolgreich empfangen hat.
	 */
	public void onSpielfeldOk() {
		spielfeldOk++;
		
		// Wenn noch nicht alle OKs vorliegen, hier abbrechen
		if (spielfeldOk < (connections.size())) return;
		
		// Allen Spielern den Namen des Spielers senden, der beginnen darf
		String ersterSpieler = connections.get(ersterSpieler()).toString();
		for (ServerStrategie conn: connections) conn.sendeRate(ersterSpieler);
	}
	
	/**
	 * Wird aufgerufen, wenn ein Spielzug empfangen wurde.
	 * 
	 * @param zug Der Spielzug als String.
	 */
	public void onZug(String zug) {
		zuege++;
		zugOk = 0;
		
		for (ServerStrategie conn: connections) conn.sendeZug(zug);
	}
	
	/**
	 * Wird aufgerufen, wenn ein Client den Spielzug erfolgreich umgesetzt hat.
	 */
	public void onZugOk() {
		// Wenn noch nicht alle OKs vorliegen, hier abbrechen
		zugOk++;
		if (zugOk < (connections.size())) return;
		
		// Testen, ob alle Karten aufgedeckt wurden (== Spiel ist zuende)
		if (spielfeld.isSpielZuende()) {
			Log.d(TAG, "Spiel zuende.");
			for (ServerStrategie conn: connections) conn.sendeBeenden();
			return;
		}
		
		// Nächsten Spieler ermitteln
		String name;
		if (((zuege % 2) != 0) || (((zuege % 2) == 0) && spielfeld.lastFoundPair)) {
			name = connections.get(spielerAktiv).toString();
		} else {
			spielerAktiv = (spielerAktiv + 1) % connections.size();
			name = connections.get(spielerAktiv).toString();
		}
		
		// Allen Spielern den Namen des Spielers senden, der als nächstes dran ist
		for (ServerStrategie conn: connections) conn.sendeRate(name);
	}
	
	/**
	 * Wird aufgerufen, wenn ein Client die Verbindung getrennt hat.
	 * 
	 * @param spieler Der Name des Spielers, der die Verbindung getrennt hat.
	 */
	public void onVerbindungGetrennt(ServerStrategie spieler) {
		Log.d(TAG, "Spieler " + spieler + " hat das Spiel verlassen.");
		
		if ((connections == null) || (connections.size() == 0)) return;
		
		// Wenn null übergeben wurde, handelt es sich um den lokalen Spieler
		if (spieler == null) spieler = connections.get(0);
		
		// Möglicherweise ist die Liste schon leer, dann abbrechen.
		if ((spieler == null) || (spieler.toString() == null)) return;
		
		Integer id = getPositionOf(spieler.toString());
		if (id == null) return;
		spieler.trenneVerbindung();
		connections.remove(id);
		
		connections.size();
		
		for (ServerStrategie conn: connections) conn.sendeSpielerWeg(spieler.toString());
		if (!spielGestartet) sucheSpieler();
	}
	
	/**
	 * Wird aufgerufen, wenn der Server die Verbindung zu allen Clients schließen soll.
	 */
	public void onKillAll() {
		Log.d(TAG, "Spielabbruch! Alle Sockets schließen.");
		
		for (ServerStrategie conn: connections) {
			conn.trenneVerbindung();
			connections.remove(conn);
		}
	}
	
	/**
	 * Liest die Position eines Spielers in der Verbindungsliste anhand des Spielernamens aus.
	 * 
	 * @param spieler Der Name des gesuchten Spielers.
	 * @return Die Position des Spielers, falls er in der Liste ist. Ansonsten null.
	 */
	private Integer getPositionOf(String spieler) {
		Integer pos = null;
		for (int i = 0; i < connections.size(); i++)
			if (connections.get(i).toString().equals(spieler)) pos = i;
		
		return pos;
	}
	
	/**
	 * Ermittelt per Zufall den Spieler, der das Spiel beginnen darf.
	 * 
	 * @return Die Position des ersten Spielers in der Liste.
	 * @post this.spielerAktiv == return
	 */
	private int ersterSpieler() {
		spielerAktiv = (int) (Math.round((connections.size()-1)*Math.random()));
		return spielerAktiv;
	}
}