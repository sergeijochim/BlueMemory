package de.fhgiessen.mni.bluememory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import de.fhgiessen.mni.bluememory.datentypen.GlobalValues;

/**
 * Zeigt die Liste der online verfügbaren Decks.
 * 
 * Die Liste wird aus dem Internet von GlobalValues.DECK_URL geladen. Per Klick auf ein
 * Deck kann das Deck dann installiert werden. Es wird von derselben Adresse heruntergeladen.
 * 
 * @author Timo Ebel
 *
 */
public class DeckHinzufuegen extends ListActivity {
	/** Logcat-Tag der Klasse */
	private static final String TAG = "BlueMemory.DeckHinzufuegen";
	
	/** Die Namen aller bereits installierten Decks. */
	private String[] deckNamenInstalliert;
	
	/** Die Liste der online verfügbaren Decks */
	private Map<String, String> deckNamenVerfuegbar;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.deck_hinzufuegen);
		setTitle(GlobalValues.TITEL + "Verfügbare Decks");
		
		// Liste der installierten Decks aus den SharedPrefs laden
		deckNamenInstalliert = getSharedPreferences(GlobalValues.SHARED_PREFS_DECKS, MODE_WORLD_READABLE).getAll().keySet().toArray(new String[0]);
		
		// Liste der verfügbaren Decks herunterladen
		new DeckListeLaden().execute(GlobalValues.DECK_URL + GlobalValues.DECK_LISTE);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// Angeklicktes Deck installieren
		String dateiname = deckNamenVerfuegbar.get(getListAdapter().getItem(position));
		String uri = GlobalValues.DECK_URL + dateiname;
		String name = (String) getListAdapter().getItem(position);
		String directory = dateiname.split("\\.")[0];
		new DeckInstallieren().execute(uri, name, directory);
		
	}
	
	/**
	 * DeckListeLaden
	 * 
	 * Blendet das "Laden"-Symbol ein, lädt die Liste der im Internet verfügbaren Decks
	 * und blendet das Symbol danach wieder aus.
	 * 
	 * Params: Die URL, unter der die Liste der Decks zu finden ist.
	 * Progress: Void
	 * Result: Map mit Deckname als Schlüssel und Dateiname als Wert. 
	 * 
	 * @see http://developer.android.com/reference/android/os/AsyncTask.html
	 * @author Timo Ebel
	 * 
	 * @todo Eingabe sperren.
	 */
	private class DeckListeLaden extends AsyncTask<String, Void, Map<String, String>> {
		@Override
		protected void onPreExecute() {
			// "Laden"-Symbol anzeigen
			findViewById(R.id.prb_deck_hinzufuegen).setVisibility(View.VISIBLE);
			
			// Benutzer informieren
			Toast.makeText(DeckHinzufuegen.this, R.string.toast_deckliste_laden, Toast.LENGTH_SHORT).show();
		}
	
		@Override
		protected Map<String, String> doInBackground(String... params) {
			Map<String, String> zeilen = new HashMap<String, String>();;
			
			try {
				// Request senden, Response in bufferedReader packen
				DefaultHttpClient client = new DefaultHttpClient();
				BufferedReader response = new BufferedReader(new InputStreamReader(
					client.execute(new HttpGet(params[0])).getEntity().getContent()
				));
				
				// Response auswerten
				String zeile = response.readLine();
				String[] wertePaar;
				while (zeile != null) {
					wertePaar = zeile.split(",");
					if (wertePaar.length == 2) zeilen.put(wertePaar[0], wertePaar[1]);
					zeile = response.readLine();
				}
				response.close();
			} 
			catch (IOException ioe) { zeilen = null; }
			
			return zeilen;
		}
		
		@Override
		protected void onPostExecute(Map<String, String> ergebnis) {
			// Falls der Download fehlgeschlagen ist
			if (ergebnis == null) {
				Toast.makeText(DeckHinzufuegen.this, R.string.toast_deckliste_laden_fehler, Toast.LENGTH_SHORT).show();
				finish();
				return;
			}
			
			// Zwischenspeichern und bereits installierte entfernen
			boolean save;
			deckNamenVerfuegbar = new HashMap<String, String>();
			Map.Entry<String, String> verfuegbar;
			Iterator<Entry<String, String>> it = ergebnis.entrySet().iterator();
			while (it.hasNext()) {
				save = true;
				verfuegbar = it.next();
				for (String nameInstalliert: deckNamenInstalliert) {
					if (verfuegbar.getKey().equals(nameInstalliert)) save = false;
				}
				
				if (save) deckNamenVerfuegbar.put(verfuegbar.getKey(), verfuegbar.getValue());
			}
						
			// ListAdapter erzeugen und setzen
			setListAdapter(
				new ArrayAdapter<String>(
					DeckHinzufuegen.this,
					android.R.layout.simple_list_item_1,
					deckNamenVerfuegbar.keySet().toArray(new String[0])
				)
			);
			
			// "Laden"-Symbol ausschalten
			findViewById(R.id.prb_deck_hinzufuegen).setVisibility(View.GONE);
		}
	}

	/**
	 * DeckInstallieren
	 * 
	 * Blendet das "Laden"-Symbol ein, lädt das Deck aus dem Internet,
	 * entpackt es, trägt es in die SharedPrefs ein und beendet danach die
	 * Activity.
	 * 
	 * Params: Die URL, unter der das Deck zu finden ist, der Deckname und das Verzeichnis, in
	 *         welches das Deck installiert werden soll.
	 * Progress: Void
	 * Result: Der Deckname und das Verzeichnis, in welches das Deck installiert werden soll. 
	 * 
	 * @see http://developer.android.com/reference/android/os/AsyncTask.html
	 * @author Timo Ebel
	 * 
	 * @todo Liste der installierten Decks aktualisieren.
	 * @todo Eingabe sperren.
	 */
	private class DeckInstallieren extends AsyncTask<String, Void, String[]> {
		@Override
		protected void onPreExecute() {
			// "Laden"-Symbol anzeigen
			findViewById(R.id.prb_deck_hinzufuegen).setVisibility(View.VISIBLE);
			
			// Benutzer informieren
			Toast.makeText(DeckHinzufuegen.this, R.string.toast_deck_laden, Toast.LENGTH_SHORT).show();
		}

		@Override
		protected String[] doInBackground(String... params) {
			String[] result;
			try {
				// Request senden, Response in bufferedReader packen
				HttpClient client = new DefaultHttpClient();
				HttpGet request = new HttpGet(params[0]);
				request.setHeader("Accept-Encoding", "identity");
				HttpResponse response = client.execute(request);
				BufferedInputStream content = new BufferedInputStream(
					response.getEntity().getContent()
				);

				// Datei herunterladen und temporär zwischenspeichern
				File decksDir = getDir(GlobalValues.DECKS_DIRECTORY, MODE_WORLD_READABLE);
				File file = new File(decksDir.getAbsolutePath() + File.separator + "temp.tmp");
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
				byte[] buffer = new byte[1024];
				int length;
				while ((length = content.read(buffer)) > 0) {
					bos.write(buffer, 0, length);
				}
				
				// Verbindung beenden, FileWriter schließen
				content.close(); content = null;
				bos.flush(); bos.close(); bos = null;
				
				// Deck-Verzeichnis erstellen
				File deckDir  = new File(decksDir, params[2]);
				deckDir.mkdir();
				
				// Datei entpacken
				FileOutputStream fos;
				ZipFile zFile = new ZipFile(file);
				Enumeration<? extends ZipEntry> entries = zFile.entries();
				while (entries.hasMoreElements()) {
					ZipEntry zEntry = entries.nextElement();
					if (!zEntry.isDirectory()) {
						InputStream is = zFile.getInputStream(zEntry);
						Log.d(TAG, "Datei " + zEntry.getName() + " wurde erstellt.");
						fos = new FileOutputStream(new File(deckDir + File.separator + zEntry.getName()));
						while ((length = is.read(buffer)) > 0) fos.write(buffer, 0, length);
						is.close(); fos.flush(); fos.close();
					}
				}
				
				// Temp-Datei löschen
				file.delete();
				
				// Ergebnis setzen
				result = new String[] {params[1], params[2]};
			} catch (IOException ioe) {
				result = null;
			}
			
			// Ende
			return result;
		}
		
		@Override
		protected void onPostExecute(String[] result) {
			// Falls der Download fehlgeschlagen ist
			if (result == null) {
				Toast.makeText(DeckHinzufuegen.this, R.string.toast_deck_laden_fehler, Toast.LENGTH_SHORT).show();
				
				// "Laden"-Symbol deaktivieren
				findViewById(R.id.prb_deck_hinzufuegen).setVisibility(View.INVISIBLE);
			}
			
			// Deck in den SharedPrefs hinterlegen (Name, Verzeichnis)
			Editor editor = getSharedPreferences(GlobalValues.SHARED_PREFS_DECKS, Context.MODE_WORLD_READABLE).edit();
			editor.putString(result[0], result[1]);
			editor.commit();
			
			// Ggf. zur Deckliste der "SpielErstellen"-Activity hinzufügen und sie benachrichtigen
			if ((SpielErstellen.decksListe != null) && (SpielErstellen.decksAdapter != null)) {
				SpielErstellen.decksListe.add(result[0]);
				SpielErstellen.decksAdapter.notifyDataSetChanged();
			}
			
			// Activity beenden
			Toast.makeText(DeckHinzufuegen.this, R.string.toast_deck_laden_ok, Toast.LENGTH_SHORT).show();
			setResult(RESULT_OK);
			finish();
		}
	}
}