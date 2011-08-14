package de.fhgiessen.mni.bluememory;

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import de.fhgiessen.mni.bluememory.client.ClientBT;
import de.fhgiessen.mni.bluememory.client.ClientStrategie;
import de.fhgiessen.mni.bluememory.client.MemoryActivity;
import de.fhgiessen.mni.bluememory.datentypen.GlobalValues;
import de.fhgiessen.mni.bluememory.datentypen.Spielfeld;

/**
 * Zeigt eine Liste der verfügbaren Spiele.
 * 
 * Ein Klick auf ein Spiel wechselt zur Lobby, welche versucht, eine Verbindung mit dem
 * Server herzustellen.
 * 
 * @author Timo Ebel
 *
 */
public class SpielTeilnehmen extends ListActivity implements MemoryActivity{
	/** Logcat-Tag der Klasse */
	private static final String TAG = "BlueMemory.SpielTeilnehmen";
	
	/** Kommunikations-Objekt */
	public ClientStrategie komm;
	
	/** Listen-Adapter für die ListActivity */
	public ArrayAdapter<String> spieleListe;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.spiele_liste);
		setTitle(GlobalValues.TITEL + "An einem Spiel teilnehmen");
		
		Log.d(TAG, "=== Activity erzeugt");
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		// Kommunikations-Objekt holen, Spieleliste abrufen
		komm = ClientBT.getInstance();
		komm.setActivity(this);
		komm.getSpieleListe();
		
		Log.d(TAG, "=== Activity gestartet");
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		
		Log.d(TAG, "=== Activity neu gestartet => Beenden");
		finish();
	}

	@Override
	protected void onDestroy() {
		if (isFinishing()) {
			Log.d(TAG, "Verbindung wird getrennt.");
			komm.trenneVerbindung();
			komm = null;
		}
		
		Log.d(TAG, "=== Activity beendet (onDestroy)");
		super.onDestroy();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// Kommunikations-Schnittstelle mitteilen, dass ein Spiel gewählt wurde.
		komm.onSpielGewaehlt();
		
		// Intent erstellen, gewähltes Spiel übermitteln, Lobby starten
		Intent intent = new Intent(this, Lobby.class);
		intent.setAction(GlobalValues.START_CLIENT_BT);
		intent.putExtra(ClientStrategie.EXTRA_SPIEL, spieleListe.getItem(position));
		startActivity(intent);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		switch (id) {
			case ClientStrategie.FEHLER_SPIELELISTE:
				builder.setMessage(R.string.fehler_spieleliste);
				builder.setCancelable(false);
				builder.setNeutralButton(R.string.beenden, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
						onVerbindungBeendet();
					}
				});
				dialog = builder.create();
			break;
		}
		
		return dialog;
	}
	
	@Override
	public void onSpielelisteEmpfangen(List<String> spieleListe) {
		// Listen-Adapter erstellen
		this.spieleListe = new ArrayAdapter<String>(
				this,
				android.R.layout.simple_list_item_1,
				spieleListe
		);
		
		// Adapter der Liste zuordnen
		setListAdapter(this.spieleListe);
		
		// Log
		Log.d(TAG, "Spieleliste empfangen, List-Adapter gesetzt.");
	}

	@Override
	public void onSpielelisteEmpfangenFehler() {
		showDialog(ClientStrategie.FEHLER_SPIELELISTE);
	}

	@Override
	public void onNeuesSpielEmpfangen(String spiel) {
		spieleListe.add(spiel);
		spieleListe.notifyDataSetChanged();
	}

	@Override
	public void onVerbunden() {
		// Hier wird noch nicht verbunden, daher tut diese Methode nichts.
		Log.w(TAG, "Nachricht über erfolgreichen Verbindungsaufbau erhalten, keine Verwendung.");
	}

	@Override
	public void onVerbindungBeendet() {
		finish();
	}

	@Override
	public void onSpielerNameOk() {
		// Hier kann keine Anmeldung stattfinden, daher tut diese Methode nichts.
		Log.w(TAG, "OK für Spielername empfangen, keine Verwendung.");
	}

	@Override
	public void onSpielernameBelegt() {
		// Hier kann keine Anmeldung stattfinden, daher tut diese Methode nichts.
		Log.w(TAG, "Fehler bei Spielername empfangen, keine Verwendung.");
	}

	@Override
	public void onLobbyEmpfangen(List<String> spielerListe) {
		// Spieler werden in dieser Activity nicht erfasst, daher tut diese Methode nichts.
		Log.w(TAG, "Lobby empfangen, keine Verwendung.");
	}
	
	@Override
	public void onLobbyEmpfangenFehler() {
		// Spieler werden in dieser Activity nicht erfasst, daher tut diese Methode nichts.
		Log.w(TAG, "Meldung über Fehler beim Empfang der Lobby erhalten, keine Verwendung.");
	}

	@Override
	public void onNeuerSpieler(String name) {
		// Neue Spieler werden in dieser Activity nicht erfasst, daher tut diese Methode nichts.
		Log.w(TAG, "Neuen Spieler empfangen, keine Verwendung.");
	}

	@Override
	public void onSpielerWeg(String name) {
		// Spieler werden in dieser Activity nicht erfasst, daher tut diese Methode nichts.
		Log.w(TAG, "Meldung erhalten, dass ein Spieler die Lobby verlassen hat, keine Verwendung.");
	}

	@Override
	public void onSpielStarten() {
		// Das Spiel kann hier noch nicht gestartet werden, daher tut diese Methode nichts.
		Log.w(TAG, "Aufforderung zum Spielstart, keine Verwendung.");
	}

	@Override
	public void onSpielfeldEmpfangen(Spielfeld spielfeld) {
		// Es wird hier noch kein Spielfeld benötigt, daher tut die Methode nichts.
		Log.w(TAG, "Spielfeld empfangen, keine Verwendung.");
	}

	@Override
	public void onSpielfeldEmpfangenFehler(String fehlerMeldung) {
		// Es wird hier noch kein Spielfeld benötigt, daher tut die Methode nichts.
		Log.w(TAG, "Meldung über Fehler beim Emfpangen des Spielfelds erhalten, keine Verwendung.");
	}

	@Override
	public void onSpielzugEmpfangen(int zug) {
		// Es werden hier noch nicht gespielt, daher tut die Methode nichts.
		Log.w(TAG, "Spielzug empfangen, keine Verwendung");
	}

	@Override
	public void onRate(String name) {
		// Es wird hier noch nicht gespielt, daher tut die Methode nichts.
		Log.w(TAG, "Aufforderung zum Raten empfangen, keine Verwendung");
	}

	@Override
	public void onWarten() {
		// Es wird hier noch nicht gespielt, daher tut die Methode nichts.
		Log.w(TAG, "Aufforderung zum Warten empfangen, keine Verwendung");
	}

	@Override
	public void onSpielZuende() {
		// Das Spiel hat noch nicht angefangen, daher tut die Methode nichts.
		Log.w(TAG, "Nachricht über Spielende empfangen, keine Verwendung");
	}	
}