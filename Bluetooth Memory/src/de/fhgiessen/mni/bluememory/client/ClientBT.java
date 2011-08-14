package de.fhgiessen.mni.bluememory.client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import de.fhgiessen.mni.bluememory.R;
import de.fhgiessen.mni.bluememory.datentypen.GlobalValues;
import de.fhgiessen.mni.bluememory.datentypen.Spielfeld;
import de.fhgiessen.mni.bluememory.datentypen.StatusCodes;

/**
 * Kommunikations-Schnittstelle für einen Memory-Client über Bluetooth.
 * 
 * @author Timo Ebel
 */
public class ClientBT implements ClientStrategie {
	/** Logcat-Tag der Klasse */
	private static final String TAG = "BlueMemory.ClientBT";
	
	/** Singleton-Objekt der Kommunikations-Strategie */
	private static ClientStrategie singleton = null;
	
	/** BT-Adapter */
	private BluetoothAdapter btAdapter;
	
	/** Socket für die BT-Kommunikation mit dem Server */
	private BluetoothSocket btSocket;
	
	/** Task, der auf neue Nachrichten hört */
	private MessageListener listeningTask;
	
	/** Task, der Nachrichten versendet */
	private MessageOut sendingTask;
	
	/** Broadcast-Receiver für das Auffinden neuer BT-Geräte in Reichweite */
	private BroadcastReceiver btReceiver;
	
	/** Die aktuell aktive Activity, deren Callbacks bei eingehenden Nachrichten aufgerufen werden */
	private MemoryActivity aktuelleActivity;
	
	/** true, wenn der BT-Socket verbunden wurde, sonst false */
	private boolean connected;
	
	/** Zählt die fehlerhaften Übertragungen der Lobby. */
	private int errorCounterLobby;
	
	/** Zählt die fehlerhaften Übertragungen des Spielfelds. */
	private int errorCounterSpielfeld;
	
	/**
	 * Konstruktor unsichtbar wegen Singleton.
	 */
	private ClientBT() {
		errorCounterLobby = 0;
		errorCounterSpielfeld = 0;
	}
	
	/**
	 * Factory (Singleton) für ClientBT.
	 * 
	 * @return Instanz von ClientStrategie
	 */
	public static ClientStrategie getInstance() {
		if (singleton == null) singleton = new ClientBT();
		return singleton;
	}
		
	@Override
	public void getSpieleListe() {
		// BT-Adapter holen
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		
		// BT-Untersützung prüfen, bei Fehler Callback aufrufen und Ausführung abbrechen.
		if (btAdapter == null) {
			Log.e(TAG, "Kein Bluetooth-Adapter gefunden!");
			aktuelleActivity.onSpielelisteEmpfangenFehler();
			return;
		}
		
		// BT-Adapter ggf. aktivieren
		if (!btAdapter.isEnabled()) {
			Log.d(TAG, "Bluetooth-Adapter wird aktiviert.");
			new AdapterAktivieren().execute();
		} else {
			Log.d(TAG, "Bluetooth-Adapter ist bereits aktiviert.");
			onAdapterAktiviert();
		}
	}

	@Override
	public void onSpielGewaehlt() {
		// Beenden der Device-Discovery und des Broadcast-Receivers.
		btAdapter.cancelDiscovery();
		((Activity) aktuelleActivity).unregisterReceiver(btReceiver);
		Log.d(TAG, "Bluetooth Device Recovery beendet.");
	}

	@Override
	public void verbinde(String spiel) {
		// MAC-Adresse aus String extrahieren (2. Zeile)
		String[] geraet = spiel.split("\n");
		
		// Server-Device holen
		BluetoothDevice server = btAdapter.getRemoteDevice(geraet[1]);
		
		// Task starten
		new VerbindungHerstellen().execute(server);
		Log.d(TAG, "Verbinde mit Bluetooth-Gerät: " + geraet[0] + " / " + geraet[1]);
	}

	@Override
	public void trenneVerbindung() {
		if (btSocket != null) {
			try {
				Log.d(TAG, "Bluetooth-Verbindung wird beendet.");
				if (listeningTask != null) listeningTask.cancel(true);
				if (sendingTask != null) sendingTask.cancel(true);
				
				btSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "IOException beim Schließen des BT-Sockets.");
			} finally {
				connected = false;
				btSocket = null;
			}
		} else {
			Log.d(TAG, "Verbindung ist bereits beendet.");
		}
		
		// BT-Adapter deaktivieren
		Toast.makeText((Activity) aktuelleActivity, R.string.toast_bt_adapter_deaktivieren, Toast.LENGTH_SHORT).show();
		if ((btAdapter != null) && btAdapter.isEnabled()) btAdapter.disable();
		
		// Singleton-Instanz dereferenzieren
		if (singleton != null) singleton = null;
	}

	@Override
	public void sendeHelo(String name) {
		Log.d(TAG, ">>> HELO " + name);
		messageOut(StatusCodes.HELO, name);
	}

	@Override
	public void getLobby() {
		Log.d(TAG, ">>> GET_LOBBY");
		messageOut(StatusCodes.GET_LOBBY);
	}

	@Override
	public void sendeLobbyOK() {
		Log.d(TAG, ">>> OK_LOBBY");
		messageOut(StatusCodes.OK_LOBBY);
	}

	@Override
	public void sendeSpielStarten() {
		// BT-Clients starten keine Spiele, nur der Server kann das.
		return;
	}

	@Override
	public void getSpielfeld() {
		Log.d(TAG, ">>> GET_SPIELFELD");
		messageOut(StatusCodes.GET_SPIELFELD);	
	}

	@Override
	public void sendeSpielfeldOK() {
		Log.d(TAG, ">>> OK_SPIELFELD");
		messageOut(StatusCodes.OK_SPIELFELD);
	}

	@Override
	public void sendeZug(int zug) {
		Log.d(TAG, ">>> ZUG " + zug);
		messageOut(StatusCodes.ZUG, String.valueOf(zug));
	}

	@Override
	public void sendeZugOK() {
		Log.d(TAG, ">>> OK_ZUG");
		messageOut(StatusCodes.OK_ZUG);
	}

	@Override
	public void sendeBye() {
		Log.d(TAG, ">>> BYE");
		messageOut(StatusCodes.BYE);
	}

	@Override
	public void setActivity(MemoryActivity aktuelleActivity) {
		Log.d(TAG, "Aktuelle Activity: " + aktuelleActivity.getClass().getSimpleName());
		this.aktuelleActivity = aktuelleActivity;
	}
	
	/**
	 * Wird nach Aktivierung des BT-Adapters aufgerufen.
	 * 
	 * Sucht die bereits verbundenen BT-Geräte und gibt die Liste an die
	 * aktuell aktive Activity. Danach wird die Suche nach weiteren Geräten
	 * eingeschaltet.
	 */
	private void onAdapterAktiviert() {
		Log.d(TAG, "Bluetooth-Adapter wurde aktiviert.");
		
		// String-Liste mit den Device-Namen und MAC-Adressen erstellen
		List<String> spieleListe = new ArrayList<String>();
		for (BluetoothDevice device: btAdapter.getBondedDevices())
			spieleListe.add(device.getName() + "\n" + device.getAddress());
		
		// Liste an Activity übergeben
		aktuelleActivity.onSpielelisteEmpfangen(spieleListe);
		
		// BroadcastReceiver und Device-Discovery starten
		Log.d(TAG, "Starte Bluetooth Device Recovery.");
		btReceiver = new DeviceFoundBroadcastReceiver();
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		((Activity) aktuelleActivity).registerReceiver(btReceiver, filter);
		btAdapter.startDiscovery();
	}
	
	/**
	 * Startet den Thread zum Abhören von Nachrichten auf dem BT-Socket.
	 * 
	 * Die Methode wird aufgerufen, sobald die BT-Verbindung hergestellt wurde.
	 */
	private void startListening() {
		Log.d(TAG, "Message-Listener wird auf dem Bluetooth-Socket gestartet.");
		new MessageListener().execute();
	}
		
	/**
	 * Versendet einen Statuscode über den Bluetooth-Socket.
	 * 
	 * @param status Der Status-Code (s. StatusCodes)
	 */
	private void messageOut(byte status) {
		new MessageOut().execute(new byte[] {status});
	}
	
	/**
	 * Versendet eine Nachricht mit Statuscode und Parametern über den Bluetooth-Socket.
	 * 
	 * @param status Der Status-Code (s. StatusCodes)
	 * @param message Die Parameter als String.
	 */
	private void messageOut(byte status, String params) {
		ByteArrayBuffer message = new ByteArrayBuffer(params.getBytes().length + 1);
		message.append(status);
		message.append(params.getBytes(), 0, params.getBytes().length);
		
		new MessageOut().execute(message.toByteArray());
	}
	
	/**
	 * Verarbeitet eingehende Nachrichten des Bluetooth-Sockets.
	 * 
	 * Nach dem Empfang und der Auswertung einer Nachricht werden ggf. notwendige
	 * Aktionen durchgeführt, bspw. der Aufruf eines Callbacks der aktuellen Actitvity.
	 * 
	 * @param msg Die eingehende Nachricht inkl. Statuscode.
	 */
	private void onMessageIn(ByteArrayBuffer msg) {
		// Speichert, ob ein Fehler aufgetreten ist
		boolean error = false;
		
		// Statuscode und ggf. Parameter auslesen
		int status = msg.byteAt(0);
		String params = null;
		if (msg.length() > 1) params = new String(msg.toByteArray(), 1, msg.length() - 1);
		Log.d(TAG, "Nachricht empfangen. Status-Code: " + status + ((params != null) ? "; Parameter: " + params : "; keine Parameter"));
		
		switch (status) {
			// Anmeldung erfolgreich
			case StatusCodes.HELLO:
				aktuelleActivity.onSpielerNameOk();
			break;
			
			// Anmeldung fehlgeschlagen (Spielername belegt)
			case StatusCodes.FEHLER_HELO:
				aktuelleActivity.onSpielernameBelegt();
			break;
			
			// Verbindung beendet
			case StatusCodes.BYE:
				aktuelleActivity.onVerbindungBeendet();
			break;
			
			//  Lobby empfangen
			case StatusCodes.POST_LOBBY:
				List<String> spielerListe = new ArrayList<String>();
				if (params != null) {
					try {
						// Spielfeld auslesen
						JSONArray json = new JSONObject(params).getJSONArray("lobby");
						for (int i = 0; i < json.length(); i++) spielerListe.add(json.getString(i));
						
						// Spielfeld an Activity übermitteln
						aktuelleActivity.onLobbyEmpfangen(spielerListe);
					} catch (JSONException e) {
						error = true;
					}
				} else {
					error = true;
				}
				
				// Fehler:
				if (error) {
					// Fehler-Zähler inkrementieren
					errorCounterLobby++;
					
					// Bei 3 Fehlversuchen abbrechen, ansonsten Lobby erneut anfordern
					if (errorCounterLobby >= 3) {
						aktuelleActivity.onLobbyEmpfangenFehler();
					} else {
						Log.w(TAG, "Fehler beim Empfangen der Lobby! " + errorCounterLobby + ". Versuch");
						getLobby();
					}
				}
			break;
			
			// Ein Spieler hat die Lobby betreten
			case StatusCodes.PLAYER_JOINED: 
				aktuelleActivity.onNeuerSpieler(params);
			break;
			
			// Ein Spieler hat die Lobby verlassen
			case StatusCodes.PLAYER_LEFT: 
				aktuelleActivity.onSpielerWeg(params);
			break;
			
			// Das Spiel wird gestartet
			case StatusCodes.STARTEN: 
				aktuelleActivity.onSpielStarten();
			break;
			
			// Das Spielfeld wurde empfangen
			case StatusCodes.POST_SPIELFELD:
				if (params != null) {
					try {
						// Spielfeld
						aktuelleActivity.onSpielfeldEmpfangen(Spielfeld.createFromJSON((Activity) aktuelleActivity, params));
					} catch (JSONException e) {
						error = true;
					} catch (FileNotFoundException e) {
						aktuelleActivity.onSpielfeldEmpfangenFehler(((Activity) aktuelleActivity).getString(R.string.toast_deck_nicht_installiert));
					}
				} else {
					error = true;
				}
				
				// Fehler:
				if (error) {
					// Fehler-Zähler inkrementieren
					errorCounterSpielfeld++;
					
					// Bei 3 Fehlversuchen abbrechen, ansonsten Spielfeld erneut anfordern
					if (errorCounterSpielfeld >= 3) {
						aktuelleActivity.onSpielfeldEmpfangenFehler(null);
					} else {
						Log.w(TAG, "Fehler beim Empfangen des Spielfelds! " + errorCounterSpielfeld + ". Versuch");
						getSpielfeld();
					}
				}
			break;
			
			// Client am Zug
			case StatusCodes.RATE: 
				aktuelleActivity.onRate(params);
			break;
			
			// Client wartet
			case StatusCodes.WARTE:
				aktuelleActivity.onWarten();
			break;
			
			// Ein anderer Client hat einen Zug gemacht
			case StatusCodes.POST_ZUG:
				aktuelleActivity.onSpielzugEmpfangen(Integer.parseInt(params));
			break;
			
			// Das Spiel ist zuende
			case StatusCodes.BEENDEN:
				sendeBye();
				aktuelleActivity.onSpielZuende();
			break;
			
			// Unbekannter Statuscode
			default: Log.w(TAG, "Unbekannter Statuscode: " + String.valueOf(status));
		}
	}
	
	/**
	 * BroadcastReceiver zum Abfangen von Intents der "Bluetooth Device Recovery".
	 * 
	 * @see http://developer.android.com/reference/android/content/BroadcastReceiver.html
	 * @see http://developer.android.com/guide/topics/wireless/bluetooth.html#DiscoveringDevices
	 */
	private class DeviceFoundBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context ctx, Intent intent) {
			if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				
				// Wenn das Gerät noch nicht verbunden ist, zur Liste hinzufügen
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
					String spiel = device.getName() + "\n" + device.getAddress();
					aktuelleActivity.onNeuesSpielEmpfangen(spiel);
				}
			}
		}
	}
	
	/**
	 * Aktiviert den BT-Adapter.
	 * 
	 * Nach erfolgreicher Aktivierung wird die Methode onAdapterAktiviert aufgerufen.
	 * 
	 * Params: Void
	 * Progress: Void
	 * Result: Void
	 * 
	 * @see http://developer.android.com/reference/android/os/AsyncTask.html
	 * @author Timo Ebel
	 */
	private class AdapterAktivieren extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onPreExecute() {
			Toast.makeText((Activity) aktuelleActivity, R.string.toast_bt_adapter_aktivieren, Toast.LENGTH_SHORT).show();
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			// Adapter aktivieren
			btAdapter.enable();
			
			// Warten, bis der Adapter aktiviert ist
			while (!btAdapter.isEnabled());
			
			// Ende
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			// Adapter wurde aktiviert, weitermachen.
			onAdapterAktiviert();
		}
	}
	
	/**
	 * Stellt die Verbindung mit dem Spielserver her.
	 * 
	 * Nach erfolgreicher Herstellung der Verbindung wird der Kommunikation.btSocket auf den
	 * verbundenen Socket gesetzt, Kommunikation.connected auf "true" und per Aufruf von
	 * ClientBT.startListenung() das Abhören auf Nachrichten gestartet.
	 * 
	 * Params: Das BluetoothDevice-Objekt, von dem aus die Verbindung hergestellt werden soll
	 * Progress: Void
	 * Result: Ein BluetoothSocket-Objekt für die weitere Kommunikation
	 * 
	 * @see http://developer.android.com/reference/android/os/AsyncTask.html
	 * @author Timo Ebel
	 */
	private class VerbindungHerstellen extends AsyncTask<BluetoothDevice, Void, BluetoothSocket> {

		@Override
		protected BluetoothSocket doInBackground(BluetoothDevice... btDevice) {
			// Socket holen
			BluetoothSocket btSocket = null;
			try {
				btSocket = btDevice[0].createRfcommSocketToServiceRecord(GlobalValues.BT_UUID);
			} catch (IOException ioe) {
				Log.d(TAG, "Fehler beim Erstellen des BT-Sockets");
				aktuelleActivity.onVerbindungBeendet();
			}
			
			// Verbindungsherstellung
			try {
				// Verbindungsherstellung
				btSocket.connect();
			} catch (IOException ioe) {
				Log.d(TAG, "Fehler beim Verbinden des BT-Sockets");
				aktuelleActivity.onVerbindungBeendet();
			}
			
			return btSocket;
		}
		
		@Override
		protected void onPostExecute(BluetoothSocket result) {
			if (result != null) {
				btSocket = result;
				connected = true;
				startListening();
				aktuelleActivity.onVerbunden();
			}
		}
	}
	
	/**
	 * Hört auf eingehende Nachrichten vom BT-Server.
	 * 
	 * Der Task bricht ab, wenn keine Verbindung über den Socket besteht, es muss
	 * also sichergestellt werden, dass die Verbindung bereits hergestellt wurde.
	 * 
	 * Empfangene Nachrichten werden an den Callback "onMessageIn()" weitergegeben.
	 * 
	 * Params: Void
	 * Progress: Die empfangenen Nachrichten.
	 * Result: Void
	 * 
	 * @see http://developer.android.com/reference/android/os/AsyncTask.html
	 * @author Timo Ebel
	 */
	private class MessageListener extends AsyncTask<Void, ByteArrayBuffer, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			// InputStream holen
			InputStream is;
			try {
				is = btSocket.getInputStream();
			} catch (IOException e) {
				Log.d(TAG, "Fehler beim Initialisieren des Input-Streams");				
				is = null;
				e.printStackTrace();
			}

			// Listener-Schleife
			while (connected) {
				try {
					// Buffer usw. erstellen
					ByteArrayBuffer msg = new ByteArrayBuffer(1024);
					int length;
					byte[] buffer = new byte[1024];
					
					// Auf Nachrichten hören
					length = is.read(buffer);
					msg.append(buffer, 0, length);
					
					// Nachricht weiterleiten
					publishProgress(msg);
				} catch (IOException ioe) {
					// Verbindung wurde beendet
					Log.d(TAG, "Bluetooth-Verbindungsabbruch (IOException).");
					connected = false;
				}
			}
			
			// Ende
			return null;
		}
		
		@Override
		protected void onProgressUpdate(ByteArrayBuffer... msg) {
			// Message-Callback aufrufen
			onMessageIn(msg[0]);
		}
		
		@Override
		protected void onPostExecute(Void result) {
			Toast.makeText((Activity) aktuelleActivity, R.string.toast_verbindung_beendet, Toast.LENGTH_SHORT).show();
			aktuelleActivity.onVerbindungBeendet();
		}
		
		@Override
		protected void onCancelled() {
			try {
				if (btSocket != null) btSocket.getInputStream().close();
			} catch (IOException e) {
				Log.d(TAG, "Fehler beim Schließen des Input-Streams");
			}
			listeningTask = null;
		}
	}
	
	/**
	 * Sendet Nachrichten über den BT-Socket an den Server.
	 * 
	 * Params: Die zu sendende Nachricht.
	 * Progress: Void
	 * Result: Void
	 * 
	 * @see http://developer.android.com/reference/android/os/AsyncTask.html
	 * @author Timo Ebel
	 */
	private class MessageOut extends AsyncTask<byte[], Void, Void> {

		@Override
		protected Void doInBackground(byte[]... msg) {
			try {
				OutputStream out = btSocket.getOutputStream();
				out.write(msg[0]);
				out.flush();
			} catch (IOException ioe) {
				Log.d(TAG, "Fehler beim Schreiben auf den Output-Stream");
			}
			
			// Ende
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			sendingTask = null;
		}
		
		@Override
		protected void onCancelled() {
			try {
				if (btSocket != null) btSocket.getOutputStream().close();
			} catch (IOException e) {
				Log.d(TAG, "Fehler beim Schließen des Output-Streams");
			}
			sendingTask = null;
		}
	}
}