package de.fhgiessen.mni.bluememory;

import java.io.File;

import de.fhgiessen.mni.bluememory.datentypen.GlobalValues;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * Zeigt die Liste der installierten Decks.
 * 
 * Das Kontextmenü eines Eintrages bietet die Möglichkeit, diesen zu löschen.
 * Im Menü befindet sich die Option, weitere Decks aus dem Internet hinzuzufügen.
 * 
 * @author Timo Ebel
 *
 */
public class DecksVerwalten extends ListActivity {
	/** Logcat-Tag der Klasse */
	//private static final String TAG = "BlueMemory.DeckHinzufuegen";
	
	/** Enthält die Namen der Decks aus den SharedPrefs */
	private String[] deckNamen;
	
	/** Enthält die Ordnernamen der Decks aus den SharedPrefs */
	private String[] deckOrdner;
	
	/** Sperrt Menüs während eines laufenden Async-Tasks */
	private boolean locked = false;
	
	/** Listen-Adapter für die Activity */
	private ArrayAdapter<String> decksAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.decks);
		setTitle(GlobalValues.TITEL + "Installierte Decks");
		
		// Decknamen aus den SharedPrefs holen
		SharedPreferences sharedPrefs = getSharedPreferences(GlobalValues.SHARED_PREFS_DECKS, Context.MODE_WORLD_READABLE);
		deckNamen = sharedPrefs.getAll().keySet().toArray(new String[0]);
		deckOrdner = sharedPrefs.getAll().values().toArray(new String[0]);
		decksAdapter = new ArrayAdapter<String>(
				this,
				android.R.layout.simple_list_item_1,
				deckNamen
		);
		setListAdapter(decksAdapter);
		
		// Kontext-Menü registrieren
		registerForContextMenu(getListView());
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
		if (locked) return;
		
		super.onCreateContextMenu(menu, view, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.decks_context, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
		switch (item.getItemId()) {
			case R.id.item_deck_loeschen:
				// Async-Task starten
				new DeckLoeschen().execute(info.position);
			break;
		}
		
		return true;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (locked) return false;
		
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.decks_optionen, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.item_deck_hinzufuegen:
				Intent intent = new Intent(this, DeckHinzufuegen.class);
				startActivityForResult(intent, 0);
			break;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Decknamen aus den SharedPrefs holen
		SharedPreferences sharedPrefs = getSharedPreferences(GlobalValues.SHARED_PREFS_DECKS, Context.MODE_WORLD_READABLE);
		deckNamen = sharedPrefs.getAll().keySet().toArray(new String[0]);
		deckOrdner = sharedPrefs.getAll().values().toArray(new String[0]);
		decksAdapter = new ArrayAdapter<String>(
				this,
				android.R.layout.simple_list_item_1,
				deckNamen
		);
		setListAdapter(decksAdapter);
	}
	
	/**
	 * Löscht ein Deck aus dem Dateisystem und den Shared Preferences.
	 * 
	 * Params: Die Position des gewählten Eintrages in der List-Activity.
	 * Progress: Void
	 * Result: Der Name des zu gelöschten Decks.
	 * 
	 * @see http://developer.android.com/reference/android/os/AsyncTask.html
	 * @author Timo Ebel
	 */
	private class DeckLoeschen extends AsyncTask<Integer, Void, String> {
		@Override
		protected void onPreExecute() {
			// Menüs sperren
			locked = true;
			
			// "Laden"-Symbol anzeigen
			findViewById(R.id.prb_deck_loeschen).setVisibility(View.VISIBLE);
			
			// Benutzer informieren
			Toast.makeText(DecksVerwalten.this, R.string.toast_deck_loeschen, Toast.LENGTH_SHORT).show();
		}

		@Override
		protected String doInBackground(Integer... position) {
			// Ordnernamen zusammenbauen
			File deckLoeschenOrdner =
				new File(
					getDir(GlobalValues.DECKS_DIRECTORY, MODE_WORLD_WRITEABLE).getAbsolutePath() +
					File.separatorChar + deckOrdner[position[0]]
				);
			
			// Bilder löschen
			for (File bild: deckLoeschenOrdner.listFiles())
				bild.delete();
			
			// Deck-Ordner löschen
			deckLoeschenOrdner.delete();
			
			// Eintrag aus den SharedPrefs entfernen
			Editor editor = getSharedPreferences(GlobalValues.SHARED_PREFS_DECKS, MODE_WORLD_WRITEABLE).edit();
			editor.remove(deckNamen[position[0]]);
			editor.commit();
			
			// Ende
			return deckNamen[position[0]];
		}
		
		@Override
		protected void onPostExecute(String ergebnis) {
			// Listen aktualisieren
			SharedPreferences sharedPrefs = getSharedPreferences(GlobalValues.SHARED_PREFS_DECKS, MODE_WORLD_READABLE);
			deckNamen  = sharedPrefs.getAll().keySet().toArray(new String[0]);
			deckOrdner = sharedPrefs.getAll().values().toArray(new String[0]);
			decksAdapter = new ArrayAdapter<String>(
					DecksVerwalten.this,
					android.R.layout.simple_list_item_1,
					deckNamen
			);
			setListAdapter(decksAdapter);
			
			// Ggf. zur Deckliste der "SpielErstellen"-Activity hinzufügen und sie benachrichtigen
			if ((SpielErstellen.decksListe != null) && (SpielErstellen.decksAdapter != null)) {
				SpielErstellen.decksListe.remove(ergebnis);
				SpielErstellen.decksAdapter.notifyDataSetChanged();
			}			

			// Menüs freischalten
			locked = false;
			
			// "Laden"-Symbol ausblenden
			findViewById(R.id.prb_deck_loeschen).setVisibility(View.GONE);
			
			// Benutzer informieren
			Toast.makeText(DecksVerwalten.this, R.string.toast_deck_geloescht, Toast.LENGTH_SHORT).show();
		}
	}
}