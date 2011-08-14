package de.fhgiessen.mni.bluememory.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import de.fhgiessen.mni.bluememory.R;
import de.fhgiessen.mni.bluememory.ServerService;
import de.fhgiessen.mni.bluememory.datentypen.GlobalValues;
import de.fhgiessen.mni.bluememory.datentypen.StatusCodes;

/**
 * Kommunikations-Strategie für den Memory-Server über Bluetooth.
 * 
 * @author Sergei Jochim
 *
 */
public class ServerBT implements ServerStrategie {
	/** Logcat-Tag der Klasse */
	private static final String TAG = "BlueMemory.ServerBT";
	
	/** Ein Async-Task, in dem nach neuen Spielern gesucht wird */
	private SucheSpieler sucheSpielerTask;
	
	/** Das globale ServerService-Objekt */
	private ServerService server;
	
	/** Der Name des Spielers, den diese Verbindung repräsentiert. */
	private String name;
	
	/** Der globale Bluetooth-Adapter */
	private static BluetoothAdapter btAdapter;
	
	/** Der Bluetooth-Socket dieser Verbindung */
	private BluetoothSocket btSocket;
	
	/** Hält fest, ob die Verbindung schon oder noch besteht */
	private boolean connected;
	
	/** Ein Async-Task, in dem auf neue Nachrichten gehört wird */
	private MessageListener listeningTask;
	
	/**
	 * Der Async-Task, mit dem gerade eine Nachricht gesendet wird oder null,
	 * falls gerade nicht gesendet wird.
	 */
	private MessageOut sendingTask;
	
	/**
	 * Konstruktor
	 * 
	 * Stellt die Verbindung zum ServerService her, erzeugt einen temporären
	 * Namen (zur Identifizierung) und wartet auf eingehende BT-Verbindungen.
	 */
	public ServerBT() {
		server = ServerService.getInstance();
		connected = false;
		name = "TEMPNAME_" + Math.random() * System.currentTimeMillis();
		
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (btAdapter == null) {
			Log.e(TAG, "Kein Bluetooth-Adapter gefunden!");
			server.onVerbindungFehlgeschlagen();
			return;
		}
		
		if (!btAdapter.isEnabled()) new AdapterAktivieren().execute();
	}
	
	/**
	 * Beendet die Bluetooth-Verbindung.
	 * 
	 * @param ctx Application Context
	 */
	public static void cleanUp(Context ctx) {	
		// BT-Adapter deaktivieren
		Toast.makeText(ctx, R.string.toast_bt_adapter_deaktivieren, Toast.LENGTH_SHORT).show();
		if ((btAdapter != null) && btAdapter.isEnabled()) btAdapter.disable();
	}
	
	@Override
	public void sendeBeenden() {
		messageOut(StatusCodes.BEENDEN);
	}

	@Override
	public void sendeFehlerHelo() {
		messageOut(StatusCodes.FEHLER_HELO);
	}

	@Override
	public void sendeHello() {
		messageOut(StatusCodes.HELLO);
	}

	@Override
	public void sendeLobby() {
		// Lobby-Liste erstellen
		List<String> lobby = new ArrayList<String>();
		for (ServerStrategie spieler: server.connections) lobby.add(spieler.toString());
		
		// JSON-Objekt erstellen
		JSONObject jo = null;
		try {
			 jo = new JSONObject().put("lobby", new JSONArray(lobby));
		} catch (JSONException e) {
			jo = new JSONObject();
		}

		// Lobby an Client senden
		messageOut(StatusCodes.POST_LOBBY, jo.toString());
	}

	@Override
	public void sendeNeuerSpieler(String spieler) {
		messageOut(StatusCodes.PLAYER_JOINED, spieler);
	}

	@Override
	public void sendeRate(String spieler) {
		messageOut(StatusCodes.RATE, spieler);
	}

	@Override
	public void sendeSpielerWeg(String spieler) {
		messageOut(StatusCodes.PLAYER_LEFT, spieler);
	}

	@Override
	public void sendeSpielfeld() {
		messageOut(StatusCodes.POST_SPIELFELD, server.spielfeld.toJSON());
	}

	@Override
	public void sendeSpielStarten() {
		messageOut(StatusCodes.STARTEN);
	}

	@Override
	public void sendeZug(String zug) {
		messageOut(StatusCodes.POST_ZUG, zug);
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public void trenneVerbindung() {
		Log.d(TAG, "Bluetooth-Socket wird geschlossen: " + name);
		try {
			if (btSocket != null) {
				// Ggf. Listening-Task abbrechen
				if (listeningTask != null) listeningTask.cancel(true);				
				if (sendingTask != null) sendingTask.cancel(true);
				
				// Socket schließen
				btSocket.close();
				connected = false;
			} else {
				if (sucheSpielerTask != null) sucheSpielerTask.cancel(true);
			}
		} catch (IOException e) {
			Log.e(TAG, "Fehler beim Schlie0en des BT-Sockets");
		}
	}

	@Override
	public String toString() {
		return name;
	}
	
	/**
	 * Wird nach Aktivierung des BT-Adapters aufgerufen.
	 * 
	 * Sucht die bereits verbundenen BT-Geräte und gibt die Liste an die
	 * aktuell aktive Activity. Danach wird die Suche nach weiteren Geräten
	 * eingeschaltet.
	 */
	private void onAdapterAktiviert() {
		sucheSpielerTask = new SucheSpieler();
		sucheSpielerTask.execute();
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
		//boolean error = false;
		
		// Statuscode und ggf. Parameter auslesen
		int status = msg.byteAt(0);
		String params = null;
		if (msg.length() > 1) params = new String(msg.toByteArray(), 1, msg.length() - 1);
		Log.d(TAG, "Nachricht empfangen. Status-Code: " + status + ((params != null) ? "; Parameter: " + params : "; keine Parameter"));
		
		switch (status) {
			case StatusCodes.HELO:
				server.onNeuerSpieler(this, params);
			break;
			
			case StatusCodes.GET_LOBBY:
				sendeLobby();
			break;
			
			case StatusCodes.GET_SPIELFELD:
				sendeSpielfeld();
			break;
			
			case StatusCodes.OK_SPIELFELD:
				server.onSpielfeldOk();
			break;
			
			case StatusCodes.ZUG:
				server.onZug(params);
			break;
			
			case StatusCodes.OK_ZUG:
				server.onZugOk();
			break;
			
			case StatusCodes.BYE:
				server.onVerbindungGetrennt(this);
			break;
			default:
		}
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
		
		sendingTask = new MessageOut();
		sendingTask.execute(message.toByteArray());
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
			Toast.makeText(server, R.string.toast_bt_adapter_aktivieren, Toast.LENGTH_SHORT).show();
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
	
	private class SucheSpieler extends AsyncTask<Void, Void, BluetoothSocket> {
		BluetoothServerSocket btServer;
		BluetoothSocket tmpSocket;
		
		@Override
		protected BluetoothSocket doInBackground(Void... params) {
			try {
				btServer = btAdapter.listenUsingRfcommWithServiceRecord(GlobalValues.SDP_NAME, GlobalValues.BT_UUID);
				tmpSocket = btServer.accept();
				btServer.close();
				btServer = null;
				
				return (tmpSocket);
			} catch (IOException e) {
				Log.e(TAG, "Fehler beim Warten auf neue Clients.");
			}
		
			return null;
		}
		
		@Override
		protected void onPostExecute(BluetoothSocket result) {
			if (result == null) {
				Log.e(TAG, "Fehler beim Verbindungsaufbau mit Remote-Device.");
				server.onVerbindungFehlgeschlagen();
			} else {
				btSocket = result;
				connected = true;
				listeningTask = new MessageListener();
				listeningTask.execute();
				server.onNeueVerbindung();
			}
		}
		
		@Override
		protected void onCancelled() {
			try {
				if (btServer != null) btServer.close();
				if (tmpSocket != null) tmpSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "Fehler beim Schließen der BT-Sockets");
			}
		}
	}
	
	/**
	 * Hört auf eingehende Nachrichten vom BT-Client.
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
	private class MessageListener extends AsyncTask<Void, ByteArrayBuffer, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {
			// InputStream holen
			InputStream is;
			try {
				is = btSocket.getInputStream();
			} catch (IOException e) {
				Log.e(TAG, "Fehler beim Initialisieren des Input-Streams");
				is = null;
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
			return connected;
		}
		
		@Override
		protected void onProgressUpdate(ByteArrayBuffer... msg) {
			// Message-Callback aufrufen
			onMessageIn(msg[0]);
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			server.onVerbindungGetrennt(ServerBT.this);
		}
		
		@Override
		protected void onCancelled() {
			try {
				if (btSocket != null) btSocket.getInputStream().close();
			} catch (IOException e) {
				Log.e(TAG, "Fehler beim Schließen des Input-Streams");
			}
			listeningTask = null;
		}
	}

	/**
	 * Sendet Nachrichten über den BT-Socket an den Client.
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
				Log.e(TAG, "Fehler beim Senden auf dem Output-Stream.");
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
				Log.e(TAG, "Fehler beim Schließen des Output-Streams");
			}
			sendingTask = null;
		}
	}
}