package de.fhgiessen.mni.bluememory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;
import de.fhgiessen.mni.bluememory.client.ClientBT;
import de.fhgiessen.mni.bluememory.client.ClientLokal;
import de.fhgiessen.mni.bluememory.client.ClientStrategie;
import de.fhgiessen.mni.bluememory.client.MemoryActivity;
import de.fhgiessen.mni.bluememory.datentypen.GlobalValues;
import de.fhgiessen.mni.bluememory.datentypen.ImageAdapter;
import de.fhgiessen.mni.bluememory.datentypen.Spielfeld;
import de.fhgiessen.mni.bluememory.datentypen.Statistik;
import de.fhgiessen.mni.bluememory.datentypen.StringTools;

/**
 * Die Activity mit dem Spielfeld und für den Spielverlauf.
 * 
 * Hier wird das Spielfeld vom Server angefordert und dargestellt. Bei fehlerhaftem
 * Spielfeld-Empfang bricht die Activity mit einem Fehler-Dialog ab.
 * 
 * Ist das Spielfeld erfolgreich empfangen worden, wird dies dem Server mitgeteilt. Wenn
 * alle Clients ihr "OK" gesendet haben, beginnt das Spiel.
 * 
 * Der Callback onRate(String) erhält den Spielernamen, der als nächstes einen Zug machen darf.
 * Dieser Name wird mit dem Namen des aktuellen Spielers an diesem Gerät verglichen. Stimmen
 * sie überein, ist man am Zug und das Spielfeld wird freigegeben. Ansonsten bleibt das
 * Spielfeld gesperrt und es wird auf den Zug eines Mitspielers gewartet.
 * 
 * Tätigt man einen Zug, wird dieser an den Server gesendet. Der Server wiederum teilt diesen
 * Zug allen Clients mit, die den Zug auf dem Spielfeld darstellen, einen Moment warten
 * (abhängig vom Schwierigkeitsgrad) und dann den Erhalt bestätigen. Liegt dem Server die
 * Empfangsbestätigung vor, sendet er den Namen des nächsten Spielers, der am Zug ist.
 * 
 * Erhält ein Client einen Zug, der auf der eigenen Kopie des Spielfelds ungültig ist, sind die
 * Spielfelder nicht mehr synchron. Die Activity wird dann beendet.
 * 
 * Für jeden Spieler wird während des Spiels eine Statistik mit der Anzahl der Züge und der Anzahl
 * der gefundenen Paare geführt.
 * 
 * Sind alle Karten aufgedeckt, ist das Spiel zuende. Der Server sendet das entsprechende Signal an
 * alle Clients. Die Spiel-Activity ermittelt dann den (bzw. bei einem Unentschieden: die) Gewinner,
 * aktualisiert die persönliche Statistik und zeigt einen Dialog mit Gewinner(n) und der Anzahl der
 * gefundenen Paare an. Ein Klick auf "OK" beendet die Activity und führt zurück zum Hauptmenü.
 * 
 * @author Timo Ebel
 *
 */
public class Spiel extends Activity implements MemoryActivity {
	/** Logcat-Tag der Klasse */
	private static final String TAG = "BlueMemory.Spiel";
	
	/** Das Objekt der eingesetzten Kommunikations-Strategie */
	private ClientStrategie komm;
	
	/** Das Spielfeld */
	private Spielfeld spielfeld;
	
	/** true, wenn ein Dialog im Vordergrund ist, ansonsten false. */
	private boolean dialogImVordergrund;
	
	/**
	 * HashMap mit den Spielern und ihrer Statistik.
	 * 
	 * Die Statistik ist ein 4 Werte langer Integer-Array der Form:
	 * [0] Spielzüge gesamt
	 * [1] gefundene Paare
	 * [2] der jeweils erste Zug in einer Runde
	 * [3] der jeweils zweite Zug in einer Runde
	 */
	private Map<String, Integer[]> spielerListe;
	
	/** Name des Spielers am Gerät. */
	private String spielerLokal;
	
	/** Der Spieler, der gerade an der Reihe ist. */
	private String spielerAktiv;
	
	/** Die View für das Spielfeld */
	private GridView gridView;
	
	/** Click-Listener für die GridView. */
	private OnItemClickListener clickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
			Log.d(TAG, "Klick auf Position: " + position);
			
			// Dem Server den Zug senden, wenn er darf
			if (spielfeld.check(position, false)) {
				// Spielfeld lokal sofort sperren, damit keine weiteren Klicks möglich sind
				spielfeld.locked = true;
				
				// Dem Server den Zug senden
				komm.sendeZug(position);
			} else {
				Log.d(TAG, "Ungültiger Zug. locked = " + spielfeld.locked);
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.spielfeld);
		setTitle(GlobalValues.TITEL + "Warten auf Spielstart . . .");
		
		Log.d(TAG, "=== Activity erzeugt");
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		dialogImVordergrund = false;
		
		// HashMap für Spieler-Stats zusammenbauen
		spielerListe = new HashMap<String, Integer[]>();
		for (String spieler: getIntent().getStringArrayListExtra("spielerListe"))
			spielerListe.put(spieler, new Integer[] {0, 0, 0, 0});
		
		// Eigenen Spielernamen setzen
		spielerLokal = getIntent().getStringExtra("spielerLokal");
		
		// Kommunikations-Objekt holen
		if (getIntent().getAction().equals(GlobalValues.START_SERVER)) {
			komm = ClientLokal.getInstance();
		} else {
			komm = ClientBT.getInstance();
		}

		// Spielfeld abrufen
		komm.setActivity(this);
		komm.getSpielfeld();
		
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
		if (!getIntent().getAction().equals(GlobalValues.START_SERVER)) return;
		else finish();
	}
	
	@Override
	protected Dialog onCreateDialog(int id, Bundle bundle) {
		dialogImVordergrund = true;
		Dialog dialog = null;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		switch (id) {
			case ClientStrategie.FEHLER_SPIELFELD:
				String fehler = (bundle.getString("fehler") == null)
					? getString(R.string.fehler_spielfeld)
					: bundle.getString("fehler");
				builder.setMessage(fehler);
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
			
			case ClientStrategie.SPIEL_ZUENDE:
				String msg =
					getString(R.string.gewinner) + ": " +
					StringTools.implode(bundle.getStringArrayList("gewinner"), ", ") +
					"\nmit " + bundle.getInt("paare") + " Paaren";
				
				builder.setMessage(msg);
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
	
	@Override
	public void onVerbindungBeendet() {
		if (!dialogImVordergrund) finish();
	}

	@Override
	public void onSpielfeldEmpfangen(Spielfeld spielfeld) {
		Log.d(TAG, "Spielfeld erfolgreich empfangen.");
		
		// Spielfeld setzen
		this.spielfeld = spielfeld;
		
		// Spielfeld darstellen
		gridView = (GridView) findViewById(R.id.gridview);
		ImageAdapter imaAd = new ImageAdapter(this);
		imaAd.feld = spielfeld.getKarten();
		gridView.setAdapter(imaAd);

		// Listener für die Spielkarten
		gridView.setOnItemClickListener(clickListener);
		
		// OK an Server senden
		komm.sendeSpielfeldOK();
	}

	@Override
	public void onSpielfeldEmpfangenFehler(String fehlerMeldung) {
		Log.e(TAG, "Fehler beim Empfangen des Spielfelds.");
		Bundle bundle = new Bundle();
		bundle.putString("fehler", fehlerMeldung);
		showDialog(ClientStrategie.FEHLER_SPIELFELD, bundle);
	}

	@Override
	public void onSpielzugEmpfangen(int zug) {
		setTitle(GlobalValues.TITEL + "Warten . . .");
		if (!spielfeld.check(zug, true)) {
			Log.e(TAG, "Ungültiger Zug empfangen. Spielfelder unterschiedlich?");
			finish();
		} else {
			// Aktiven Spieler holen - den brauchen wir noch öfter
			Integer[] spielerAktiv = spielerListe.get(this.spielerAktiv);
			
			// Anzahl der Züge des aktuellen Spielers inkrementieren
			spielerAktiv[0]++;
			
			// Den Zug ausführen
			spielfeld.touch(zug);
			
			// GridView aktualisieren
			((ImageAdapter) gridView.getAdapter()).feld[zug] = spielfeld.getKarte(zug);
			((ImageAdapter) gridView.getAdapter()).notifyDataSetChanged();
			
			// Den Zug speichern
			if ((spielerAktiv[0] % 2) != 0) {
				// Erster Zug
				spielerAktiv[2] = zug;
			} else {
				// Zweiter Zug
				spielerAktiv[3] = zug;
				
				// Testen, ob es ein Paar ist
				if (spielfeld.checkPair(spielerAktiv[2], spielerAktiv[3])) {
					Log.d(TAG, "Paar gefunden.");
					spielerAktiv[1]++;
				} else {
					Log.d(TAG, "Kein Paar gefunden.");
				}
			}
			
			// Kurz warten, dann okay an Server senden
			new Warten().execute();
		}
	}

	@Override
	public void onRate(String name) {
		/*
		 * Wenn es nicht der allererste Zug im gesamten Spiel ist (spielerAktiv wurde schonmal gesetzt)
		 * oder wenn der es sich um den ersten Zug einer neuen Runde handelt:
		 * 
		 * Letzten beiden Züge zudecken (falls es kein Paar ist) und die GridView aktualisieren.
		 */
		if ((spielerAktiv != null) && (spielerListe.get(spielerAktiv)[0] % 2) == 0) {
			Log.d(TAG, "Karten zudecken.");
			spielfeld.zudecken();
			
			// GridView aktualisieren
			((ImageAdapter) gridView.getAdapter()).feld = spielfeld.getKarten();
			((ImageAdapter) gridView.getAdapter()).notifyDataSetChanged();
		}
		
		// Nachsehen, ob man selbst oder ein anderer Spieler an der Reihe ist
		if (name.equals(spielerLokal)) {
			spielfeld.locked = false;
			setTitle(GlobalValues.TITEL + getString(R.string.toast_dein_zug));
			Log.d(TAG, "Juhu! Ich darf!");
		} else {
			spielfeld.locked = true;
			String titel = getString(R.string.toast_warten_auf) + " " + name;
			setTitle(GlobalValues.TITEL + titel);
			Log.d(TAG, name + " ist am Zug.");
		}
		
		// Aktiven Spieler setzen
		spielerAktiv = name;
	}

	@Override
	public void onWarten() {
		spielfeld.locked = true;
	}

	@Override
	public void onSpielZuende() {
		// Maximale Anzahl gefundener Paare ermitteln
		int paare = -1;
		for (String spieler: spielerListe.keySet())
			if (spielerListe.get(spieler)[1] > paare)
				paare = spielerListe.get(spieler)[1];
		
		// Die Gewinner ermitteln
		ArrayList<String> gewinner = new ArrayList<String>();
		for (String spieler: spielerListe.keySet())
			if (spielerListe.get(spieler)[1] == paare)
				gewinner.add(spieler);
		
		// Eigene Statistik aktualisieren
		Integer[] spielerStats = spielerListe.get(spielerLokal);
		if (gewinner.contains(spielerLokal) && (gewinner.size() == 1))
			Statistik.getInstance(this).updateStats(spielerStats[0], spielerStats[1], Statistik.GEWONNEN);
		else if (gewinner.contains(spielerLokal) && (gewinner.size() > 1))
			Statistik.getInstance(this).updateStats(spielerStats[0], spielerStats[1], Statistik.UNENTSCHIEDEN);
		else
			Statistik.getInstance(this).updateStats(spielerStats[0], spielerStats[1], Statistik.VERLOREN);
		
		// Dialogfeld mit Gewinner vorbereiten
		Bundle dialog = new Bundle();
		dialog.putStringArrayList("gewinner", gewinner);
		dialog.putInt("paare", paare);
		showDialog(ClientStrategie.SPIEL_ZUENDE, dialog);
	}

	@Override
	public void onSpielelisteEmpfangen(List<String> spieleListe) {
		// Es wird hier noch keine Spiele-Liste benötigt, daher tut die Methode nichts.
		Log.w(TAG, "Spiele-Liste empfangen, keine Verwendung");
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
	public void onVerbunden() {
		// Wir sind schon verbunden, daher tut diese Methode nichts.
		Log.w(TAG, "Nachricht über erfolgreichen Verbindungsaufbau erhalten, keine Verwendung.");
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
		// Das Spiel ist schon gestartet, daher tut die Methode hier nichts.
		Log.w(TAG, "Spiel starten empfangen, keine Verwendung");
	}
	
	/**
	 * Wartet, bevor dem Server SPIELZUG_OK gesendet wird.
	 * 
	 * Die Zeitspanne ist in GlobalValues.PAUSE definiert.
	 * 
	 * Params: Void
	 * Progress: Void
	 * Result: Void
	 * 
	 * @see http://developer.android.com/reference/android/os/AsyncTask.html
	 * @author Timo Ebel
	 */
	private class Warten extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			try {
				Thread.sleep(spielfeld.pause);
			} catch (InterruptedException e) {
				return null;
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			komm.sendeZugOK();
		}
	}
}