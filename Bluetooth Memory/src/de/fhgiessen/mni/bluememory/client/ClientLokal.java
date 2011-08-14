package de.fhgiessen.mni.bluememory.client;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import de.fhgiessen.mni.bluememory.R;
import de.fhgiessen.mni.bluememory.ServerService;
import de.fhgiessen.mni.bluememory.datentypen.GlobalValues;
import de.fhgiessen.mni.bluememory.server.ServerStrategie;

/**
 * Kommunikations-Schnittstelle für den lokalen Memory-Client.
 * 
 * @author Timo Ebel
 */
public class ClientLokal implements ClientStrategie {
	/** Logcat-Tag der Klasse */
	private static final String TAG = "BlueMemory.ClientLokal";
	
	/** Singleton-Objekt der Kommunikations-Strategie */
	private static ClientStrategie singleton = null;
	
	/** Die aktuell aktive Activity, deren Callbacks bei eingehenden Nachrichten aufgerufen werden */
	public MemoryActivity aktuelleActivity;
	
	/** Die Instanz des ServerService zur lokalen Kommunikation */
	private ServerService server = null;
	
	/** Der Name des Spielers an diesem Gerät */
	String name;
	
	/**
	 * Konstruktor unsichtbar wegen Singleton.
	 */
	private ClientLokal() { }
	
	/**
	 * Factory (Singleton) für ClientLokal.
	 * 
	 * @return Instanz von ClientStrategie
	 */
	public static ClientStrategie getInstance() {
		if (singleton == null) singleton = new ClientLokal();
		return singleton;
	}
	
	@Override
	public void onSpielGewaehlt() {
		// Bei lokaler Kommunikation gibt's hier nix zutun.
		return;
	}

	@Override
	public void getSpieleListe() {
		// Für lokale (= Server-) Kommunikation wird keine Spiele-Liste benötigt.
		return;
	}
	
	@Override
	public void verbinde(String spiel) {
		Log.d(TAG, "Lokale Verbindung wird hergestellt.");
		
		// Service starten
		Intent serviceIntent = new Intent((Activity) aktuelleActivity, ServerService.class);
		if (server == null) {
			serviceIntent.putExtra("breite", ((Activity) aktuelleActivity).getIntent().getStringExtra("breite"));
			serviceIntent.putExtra("hoehe", ((Activity) aktuelleActivity).getIntent().getStringExtra("hoehe"));
			serviceIntent.putExtra("deck", ((Activity) aktuelleActivity).getIntent().getStringExtra("deck"));
			serviceIntent.putExtra("pause", ((Activity) aktuelleActivity).getIntent().getIntExtra("pause", 1000));
		}
		((Activity) aktuelleActivity).startService(serviceIntent);
		
		// Warten, bis der Service gestartet wurde
		new Verbinde().execute();
	}

	@Override
	public void trenneVerbindung() {
		Log.d(TAG, "Service wird gestoppt . . .");
		((Activity) aktuelleActivity).stopService(new Intent((Activity) aktuelleActivity, ServerService.class));
		
		// Referenz zum Server entfernen
		if (server != null) server = null;
		
		// Singleton-Instanz dereferenzieren
		singleton = null;
	}

	@Override
	public void sendeHelo(String name) {
		server.onNeuerSpieler(null, name);
	}

	@Override
	public void getLobby() {
		List<String> lobby = new ArrayList<String>();
		for (ServerStrategie spieler: server.connections) lobby.add(spieler.toString());
		
		aktuelleActivity.onLobbyEmpfangen(lobby);
	}
	
	@Override
	public void sendeLobbyOK() {
		// Die Lobby wird direkt abgerufen, keine OK-Meldung nötig.
		return;
	}

	@Override
	public void sendeSpielStarten() {
		server.onSpielStarten();
	}

	@Override
	public void getSpielfeld() {
		if (server.spielfeld != null) {
			aktuelleActivity.onSpielfeldEmpfangen(server.spielfeld);
		} else {
			aktuelleActivity.onSpielfeldEmpfangenFehler(((Activity) aktuelleActivity).getString(R.string.toast_deck_nicht_installiert));
		}
	}

	@Override
	public void sendeSpielfeldOK() {
		server.onSpielfeldOk();
	}

	@Override
	public void sendeZug(int zug) {
		Log.d(TAG, "Klick auf " + zug);
		server.onZug(String.valueOf(zug));
	}

	@Override
	public void sendeZugOK() {
		server.onZugOk();
	}

	@Override
	public void sendeBye() {
		server.onVerbindungGetrennt(null);
	}

	@Override
	public void setActivity(MemoryActivity aktuelleActivity) {
		// Den Spielernamen auslesen, falls noch nicht gesetzt
		if (name == null) {
			name = ((Activity) aktuelleActivity).getSharedPreferences(GlobalValues.STATS_SAVE_FILE, 0).getString("stat_name", "ActivityName");
		}
		
		// Aktuelle Activity setzen
		this.aktuelleActivity = aktuelleActivity;
	}

	/**
	 * Wartet auf die Verbindungsherstellung mit dem ServerService und sendet dann die Spielerliste.
	 * 
	 * Params: Void
	 * Progress: Void
	 * Result: Void
	 * 
	 * @see http://developer.android.com/reference/android/os/AsyncTask.html
	 * @author Timo Ebel
	 */
	private class Verbinde extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... arg0) {
			// Warten, bis der Server-Service gestartet hat und verbunden ist.
			while (server == null) server = ServerService.getInstance();
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			// Spielerliste aus ServerService kopieren
			aktuelleActivity.onVerbunden();
		}
	}
}