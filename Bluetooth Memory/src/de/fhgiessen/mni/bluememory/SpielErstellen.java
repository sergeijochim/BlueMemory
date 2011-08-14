package de.fhgiessen.mni.bluememory;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import de.fhgiessen.mni.bluememory.datentypen.GlobalValues;

/**
 * Einstellungen zum Erstellen eines neuen Spiels.
 * 
 * In dieser Activity kann ein neues Spiel (als Server) erstellt werden. Dazu können
 * Einstellungen vorgenommen werden:
 * 
 * 1) Feldgröße
 * Gibt die Größe des Spielfeldes in Breite mal Höhe an.
 * 
 * 2) Deck
 * Zeigt die Liste der installierten Kartendecks.
 * 
 * 3) Schwierigkeitsstufe
 * Bestimmt die Pause zwischen zwei Spielzügen. Die Pause wird allerdings auch durch die
 * Geschwindigkeit der beteiligten Geräte und die Verbindungsgeschwindigkeit beeinflusst.
 * Die Stufen sind:
 * - leicht (1500 ms)
 * - mittel (1000 ms)
 * - schwer (500 ms)
 * 
 * Ein Klick auf Spiel erstellen wechselt zur Lobby, wo auf weitere Mitspieler gewartet werden
 * kann.
 * 
 * @author Timo Ebel
 *
 */
public class SpielErstellen extends Activity {
	/** Logcat-Tag der Klasse */
	private static final String TAG = "BlueMemory.SpielErstellen";
	
	/** Die View für die Liste der Feldgrößen */
	private static Spinner feld;
	
	/** Die View für die Liste der Decks */
	private static Spinner deck;
	
	/** Die View für die Liste der Schwierigkeitsstufen */
	private static Spinner stufe;
	
	/**
	 * Die Liste der installierten Decks.
	 * Wird von "DeckHinzufügen" und "DecksVerwalten" manipuliert.
	 */
	public static List<String> decksListe;
	
	/** Die Listen-Adapter für die Feldgrößen */
	private static ArrayAdapter<String> felderAdapter;
	
	/** Der Adapter für die Deck-Auswahl */
	public static ArrayAdapter<String> decksAdapter;
	
	/** Der Listen-Adapter für die Schwierigkeitsstufen */
	private static ArrayAdapter<String> stufeAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.spiel_erstellen);
		setTitle(GlobalValues.TITEL + "Spiel erstellen");
		
		// Felder aus der View holen
		feld = (Spinner) findViewById(R.id.spin_feldgroesse);
		deck = (Spinner) findViewById(R.id.spin_deck);
		stufe = (Spinner) findViewById(R.id.spin_spielstufe);
		
		// Felderliste
		felderAdapter = new ArrayAdapter<String>(
				this,
				android.R.layout.simple_spinner_item,
				this.getResources().getStringArray(R.array.feldgroessen)
		);
		feld.setAdapter(felderAdapter);
		felderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		// Deck-Liste aus den Shared Prefs laden
		decksListe = new ArrayList<String>();
		for (String deck: getSharedPreferences(GlobalValues.SHARED_PREFS_DECKS, MODE_WORLD_READABLE).getAll().keySet().toArray(new String[0]))
			decksListe.add(deck);
		
		// Decklisten-Adapter erzeugen
		decksAdapter = new ArrayAdapter<String>(
				this,
				android.R.layout.simple_spinner_item,
				decksListe
		);
		deck.setAdapter(decksAdapter);
		decksAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		// Schwierigkeitsstufe
		stufeAdapter = new ArrayAdapter<String>(
				this,
				android.R.layout.simple_spinner_item,
				this.getResources().getStringArray(R.array.spielstufe)
		);
		stufe.setAdapter(stufeAdapter);
		stufeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		stufe.setSelection(1);
		
		Log.d(TAG, "=== Activity erzeugt");
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		
		Log.d(TAG, "=== Activity neu gestartet => Beenden");
		finish();
	}
	
	/**
	 * Erzeugt den Intent mit den gewählten Einstellungen und startet die Lobby.
	 * 
	 * @param target Der gedrückte Button.
	 */
	public void onClickSpielErstellen(View target) {
		// Spielfeldbreite und Höhe ermitteln
		String[] dimensions = ((String) feld.getSelectedItem()).split("x");
		
		// Ordner des gewählten Decks ermitteln
		String deckOrdner = getSharedPreferences(GlobalValues.SHARED_PREFS_DECKS, MODE_WORLD_READABLE).getString((String) deck.getSelectedItem(), "");
		
		// Pause zwischen zwei Spielzügen ermitteln
		int pause;
		switch (stufe.getSelectedItemPosition()) {
			case 0: pause = 1500; break;
			case 1: pause = 1000; break;
			case 2: pause = 500; break;
			default: pause = 500; break;
		}
		
		// Intent konfigurieren
		Intent intent = new Intent(this, Lobby.class);
		intent.putExtra("breite", dimensions[0]);
		intent.putExtra("hoehe", dimensions[1]);
		intent.putExtra("deck", deckOrdner);
		intent.putExtra("pause", pause);
		intent.setAction(GlobalValues.START_SERVER);
		
		// Intent absenden
		startActivity(intent);
	}
}