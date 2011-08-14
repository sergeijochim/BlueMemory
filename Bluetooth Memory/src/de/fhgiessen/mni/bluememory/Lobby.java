package de.fhgiessen.mni.bluememory;

import java.util.ArrayList;
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
import android.widget.Button;
import de.fhgiessen.mni.bluememory.client.ClientBT;
import de.fhgiessen.mni.bluememory.client.ClientLokal;
import de.fhgiessen.mni.bluememory.client.ClientStrategie;
import de.fhgiessen.mni.bluememory.client.MemoryActivity;
import de.fhgiessen.mni.bluememory.datentypen.GlobalValues;
import de.fhgiessen.mni.bluememory.datentypen.Spielfeld;
import de.fhgiessen.mni.bluememory.datentypen.StatusCodes;

/**
 * Zeigt die Liste der Spieler, die an einem Spiel teilnehmen.
 * 
 * Es wird versucht, eine Verbindung mit dem Server-Gerät herzustellen. Ist dies
 * erfolgreich, wird der eigene Name gesendet. Der Server antwortet dann, ob der
 * Name OK oder schon belegt ist. Ist der Name OK, wird danach eine Liste aller
 * Spieler angefordert. Ist der Name belegt, wird ein entsprechender Dialog gezeigt,
 * ein Klick auf OK führt zum Beenden der Activity.
 * 
 * Der Spieler, der den Server stellt, kann das Spiel starten. Die Clients starten
 * das Spiel, sobald sie das Signal dazu vom Server erhalten.
 * 
 * @author Timo Ebel
 *
 */
public class Lobby extends ListActivity implements MemoryActivity {
	/** Logcat-Tag der Klasse */
	private static final String TAG = "BlueMemory.Lobby";
	
	/** Kommunikations-Objekt */
	public ClientStrategie komm;
	
	/** Liste der Spieler in der Lobby */
	private List<String> spielerListe;
	
	/** Name des Spieler an diesem Gerät */
	private String spielerName;
	
	/** Listen-Adapter der Spieler in der Lobby */
	private ArrayAdapter<String> spielerAdapter;
	
	/** Intent-Action, mit der die Activity aufgerufen wurde */
	private String intentAction;
	
	/**
	 * Sobald die Verbindung hergestellt wurde und der Spielername bestätigt, wird
	 * die Zurück-Taste gesperrt.
	 * true <==> Zurück-Taste gesperrt
	 * false <==> Zurück-Taste nicht gesperrt
	 */
	private boolean lockBack;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(GlobalValues.TITEL + "Lobby");
		
		// Wenn wir auf dem "Rückweg" sind, die Activity sofort beenden
		if ((savedInstanceState != null) && savedInstanceState.containsKey("BACK"));
		
		setContentView(R.layout.lobby);
		
		Log.d(TAG, "=== Activity erzeugt");
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		
		Log.d(TAG, "=== Activity neu gestartet => Beenden");
		finish();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		// Intent-Action auslesen
		intentAction = getIntent().getAction();
		
		// Kommunikations-Objekt holen
		if (intentAction.equals(GlobalValues.START_SERVER)) {
			// Lokale Verbindung zum Server-Service setzen
			komm = ClientLokal.getInstance();
		} else if (intentAction.equals(GlobalValues.START_CLIENT_BT)) {
			// Bluetooth-Verbindung zum Server-Service setzen
			komm = ClientBT.getInstance();
			
			// "Spiel starten"-Button umbenennen und deaktivieren
			Button button = (Button) findViewById(R.id.btn_spiel_starten);
			button.setText(R.string.auf_server_warten);
			button.setEnabled(false);
		} else {
			Log.e(TAG, "Unbekannte Intent-Action!");
			finish();
		}
		
		// Spielerliste initialiseren
		spielerListe = new ArrayList<String>();
		spielerAdapter = new ArrayAdapter<String>(
				this,
				android.R.layout.simple_list_item_1,
				this.spielerListe
		);
		
		// Name des Spielers an diesem Gerät aus den Shared Prefs holen
		spielerName = getSharedPreferences(GlobalValues.STATS_SAVE_FILE, 0).getString("stat_name", "ActivityName");
		
		// Zurück-Taste freischalten
		lockBack = false;
		
		// Verbindung herstellen
		komm.setActivity(this);
		komm.verbinde(getIntent().getStringExtra(ClientStrategie.EXTRA_SPIEL));
		
		Log.d(TAG, "=== Activity gestartet");
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
	public void onBackPressed() {
		// Gefangene des BlueMemory . . .
		if (lockBack) return;
		else finish();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		switch (id) {
			case ClientStrategie.FEHLER_HELO:
				builder.setMessage(R.string.spielername_belegt);
				builder.setCancelable(false);
				builder.setNeutralButton(R.string.beenden, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
						finish();
					}
				});
				dialog = builder.create();
			break;
			
			case ClientStrategie.FEHLER_LOBBY:
				builder.setMessage(R.string.fehler_lobby);
				builder.setCancelable(false);
				builder.setNeutralButton(R.string.beenden, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
						finish();
					}
				});
				dialog = builder.create();
			break;
		}
		
		return dialog;
	}
	
	/**
	 * Sended das Signal zum Starten des Spiels an den Server.
	 * 
	 * @param target Der gedrückte Button.
	 */
	public void onClickSpielStarten(View target) {
		komm.sendeSpielStarten();
	}
	
	@Override
	public void onVerbunden() {
		komm.sendeHelo(spielerName);
	}

	@Override
	public void onSpielerNameOk() {
		lockBack = true;
		komm.getLobby();
	}

	@Override
	public void onSpielernameBelegt() {
		komm.sendeBye();
		showDialog(StatusCodes.FEHLER_HELO);
	}

	@Override
	public void onLobbyEmpfangen(List<String> spielerListe) {
		// Listen-Adapter erstellen
		this.spielerListe = spielerListe;
		spielerAdapter = new ArrayAdapter<String>(
				this,
				android.R.layout.simple_list_item_1,
				this.spielerListe
		);
				
		// Adapter der Liste zuordnen
		setListAdapter(spielerAdapter);
		
		// Bei Server-Gerät Button abhängig von Anzahl der Spieler (de)aktivieren
		setButton();
		
		// Log
		Log.d(TAG, "Spielerliste empfangen, List-Adapter gesetzt.");
	}

	@Override
	public void onLobbyEmpfangenFehler() {
		komm.sendeBye();
		showDialog(ClientStrategie.FEHLER_LOBBY);
	}

	@Override
	public void onNeuerSpieler(String name) {
		// Spieler hinzufügen
		Log.d(TAG, "<<< Neuer Spieler: " + name);
		spielerListe.add(name);
		spielerAdapter.notifyDataSetChanged();
		
		// Bei Server-Gerät Button abhängig von Anzahl der Spieler (de)aktivieren
		setButton();
	}

	@Override
	public void onSpielerWeg(String name) {
		spielerListe.remove(name);
		spielerAdapter.notifyDataSetChanged();
		
		// Bei Server-Gerät Button abhängig von Anzahl der Spieler (de)aktivieren
		setButton();
	}

	@Override
	public void onSpielStarten() {
		Intent intent = new Intent(this, Spiel.class);
		intent.setAction(getIntent().getAction());
		intent.putExtra("spielerListe", (ArrayList<String>) spielerListe);
		// TODO: Spielername aus SharedPrefs
		intent.putExtra("spielerLokal", spielerName);
		
		// Spiel aufrufen
		startActivity(intent);
	}

	@Override
	public void onVerbindungBeendet() {
		finish();
	}

	@Override
	public void onSpielelisteEmpfangen(List<String> spieleListe) {
		// Es wird hier keine Spiele-Liste mehr benötigt, daher tut die Methode nichts.
		Log.w(TAG, "Spiele-Liste empfangen, keine Verwendung.");
	}

	@Override
	public void onSpielelisteEmpfangenFehler() {
		// Es wird hier keine Spiele-Liste mehr benötigt, daher tut die Methode nichts.
		Log.w(TAG, "Meldung über Fehler beim Empfangen der Spiele-Liste erhalten, keine Verwendung.");
	}

	@Override
	public void onNeuesSpielEmpfangen(String spielName) {
		// Es wird hier keine Spiele-Liste mehr benötigt, daher tut die Methode nichts.
		Log.w(TAG, "Neues Spiel empfangen, keine Verwendung.");
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
	
	/**
	 * Aktiviert / Deaktiviert den Button zum Starten des Spiels.
	 * 
	 * Damit der Button aktiviert wird, muss das Gerät den Spiel-Server darstellen und die
	 * minimale Anzahl der Spieler (GlobalValues.MIN_PLAYERS) erreicht sein und die maximale
	 * Anzahl der Spieler (GlobalValues.MAX_PLAYERS) darf nicht überschritten sein.
	 */
	private void setButton() {
		if (intentAction.equals(GlobalValues.START_SERVER)) {
			Button button = (Button) findViewById(R.id.btn_spiel_starten);
			button.setEnabled(
				((this.spielerListe.size() >= GlobalValues.MIN_PLAYERS)
				&& (this.spielerListe.size() <= GlobalValues.MAX_PLAYERS))
				? true : false
			);
		}
	}
}