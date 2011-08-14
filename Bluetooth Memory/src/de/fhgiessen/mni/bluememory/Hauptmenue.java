package de.fhgiessen.mni.bluememory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import de.fhgiessen.mni.bluememory.datentypen.GlobalValues;
import de.fhgiessen.mni.bluememory.datentypen.Statistik;

/**
 * Das Hauptmenü und der Einstiegspunkt des Spiels.
 * 
 * @author Timo Ebel
 *
 */
public class Hauptmenue extends Activity {
	/** Logcat-Tag dieser Klasse */
	public static final String TAG = "BlueMemory.Hauptmenue";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		setTitle(GlobalValues.TITEL + "Hauptmenü");
		
		Log.d(TAG, "=== Activity erzeugt");
	}

	@Override
	protected void onStart() {
		super.onStart();
		
		// Bei erstem Aufruf die Standard-Decks installieren
		if (getSharedPreferences(GlobalValues.SHARED_PREFS_APP, MODE_WORLD_READABLE).getBoolean("firstStart", true)) {
			// Statistik-Reset
			Statistik.getInstance(this).resetStats();
			
			// Kontrolle an den Async-Task übergeben
			new StandardDecksInstallieren().execute();
		} else {
			freischalten();
		}
		
		Log.d(TAG, "=== Activity gestartet");
	}
	
	/**
	 * Callback für die Buttons des Hauptmenüs.
	 * 
	 * Die Activity, die dem entsprechenden Menüpunkt zugeordnet wurde, wird ausgeführt.
	 * 
	 * @param target Der gedrückte Button.
	 */
	public void onButtonClick(View target) {
		Intent intent;

		// Intent setzen
		switch (target.getId()) {
			case R.id.btn_spiel_erstellen: intent = new Intent(this, SpielErstellen.class); break;

			case R.id.btn_spiel_teilnehmen: intent = new Intent(this, SpielTeilnehmen.class); break;

			case R.id.btn_statistik: intent = new Intent(this, Profil.class); break;

			case R.id.btn_decks_verwalten: intent = new Intent(this, DecksVerwalten.class); break;
			
			case R.id.btn_anleitung: intent = new Intent(this, Anleitung.class); break;

			default: intent = null;
		}

		// Intent ausführen
		if (intent != null) startActivity(intent);
	}
	
	/**
	 * Schaltet die Buttons des Hauptmenüs wieder frei.
	 */
	private void freischalten() {
		// Lade-Symbol ausschalten
		((ProgressBar) findViewById(R.id.prb_erster_aufruf)).setVisibility(View.GONE);
		
		// Buttons aktivieren
		findViewById(R.id.btn_spiel_erstellen).setEnabled(true);
		findViewById(R.id.btn_spiel_teilnehmen).setEnabled(true);
		findViewById(R.id.btn_statistik).setEnabled(true);
		findViewById(R.id.btn_decks_verwalten).setEnabled(true);
		findViewById(R.id.btn_anleitung).setEnabled(true);
	}
	
	/**
	 * Installiert das Standard-Deck. Aufruf nur beim ersten Start des Spiels.
	 * 
	 * Während der Installation werden die Buttons des Hauptmenüs gesperrt.
	 * 
	 * Params: Void
	 * Progress: Void
	 * Result: Void
	 * 
	 * @see http://developer.android.com/reference/android/os/AsyncTask.html
	 * @author Timo Ebel
	 */
	private class StandardDecksInstallieren extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onPreExecute() {
			// Dem Benutzer Bescheid sagen
			Toast.makeText(Hauptmenue.this, R.string.toast_konfiguriere, Toast.LENGTH_SHORT).show();
			
			// Buttons deaktivieren
			findViewById(R.id.btn_spiel_erstellen).setEnabled(false);
			findViewById(R.id.btn_spiel_teilnehmen).setEnabled(false);
			findViewById(R.id.btn_statistik).setEnabled(false);
			findViewById(R.id.btn_decks_verwalten).setEnabled(false);
			findViewById(R.id.btn_anleitung).setEnabled(false);
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			// Deck aus Assets kopieren
			try {
				// Verzeichnis erstellen
				File decksDir = getDir(GlobalValues.DECKS_DIRECTORY, MODE_WORLD_READABLE);
				File deckDir  = new File(decksDir, "androids");
				deckDir.mkdir();
				
				// Datei temporär zwischenspeichern
				File file = new File(decksDir.getAbsolutePath() + File.separator + "temp.tmp");
				InputStream fis = getAssets().open("deck1.zip");
				FileOutputStream fos = new FileOutputStream(file);
				
				byte[] bytes = new byte[1024];
				int length;
				while ((length = fis.read(bytes)) > 0) fos.write(bytes, 0, length);
				
				fis.close(); fos.flush(); fos.close();
				
				ZipFile zFile = new ZipFile(file);
				Enumeration<? extends ZipEntry> entries = zFile.entries();
				while (entries.hasMoreElements()) {
					ZipEntry zEntry = entries.nextElement();
					if (!zEntry.isDirectory()) {
						InputStream is = zFile.getInputStream(zEntry);
						Log.d("DeckHinzufuegen", "Datei " + zEntry.getName() + " wurde erstellt.");
						fos = new FileOutputStream(new File(deckDir + File.separator + zEntry.getName()));
						while ((length = is.read(bytes)) > 0) fos.write(bytes, 0, length);
						is.close(); fos.flush(); fos.close();
					}
				}
				
				// Temp-Datei löschen
				file.delete();
			} catch (IOException ioe) { ioe.printStackTrace(); }
			
			// Deck in den SharedPrefs hinterlegen
			Editor editor = getSharedPreferences(GlobalValues.SHARED_PREFS_DECKS, Context.MODE_WORLD_READABLE).edit();
			editor.putString("Androids", "androids");
			editor.commit();
			
			// firstStart in den SharedPrefs auf false setzen
			editor = getSharedPreferences(GlobalValues.SHARED_PREFS_APP, MODE_WORLD_READABLE).edit();
			editor.putBoolean("firstStart", false);
			editor.commit();
			
			// Ende
			return null;
		}
		
		@Override
		protected void onPostExecute(Void ergebnis) {
			// Menü wieder freischalten
			freischalten();
		}
	}
}