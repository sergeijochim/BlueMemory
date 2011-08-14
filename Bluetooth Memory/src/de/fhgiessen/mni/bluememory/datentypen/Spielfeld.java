package de.fhgiessen.mni.bluememory.datentypen;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

/**
 * Repräsentiert ein Memory-Spielfeld.
 * 
 * Ein Spielfeld hält eine Liste der Karten und erzeugt automatisch die Drawable-Objekte
 * zur Darstellung anhand der ID einer Karte und dem Namen des Decks, das gewählt wurde.
 * 
 * Es speichert, wenn ein Paar gefunden wurde und bietet eine Methode, alle aufgedeckten
 * Karten, die keine Paare bilden, wieder zu zu decken.
 * 
 * @author Timo Ebel
 *
 */
public class Spielfeld {
	/** Logcat-Tag der Klasse */
	private static final String TAG = "BlueMemory.Spielfeld";
	
	/** Kartenstatus: zugedeckt, anklickbar */
	private static final int ZUGEDECKT = 0;
	
	/** Kartenstatus: aufgedeckt, temporär */
	private static final int AUFGEDECKT_TEMPORARY = 1;
	
	/** Kartenstatus: aufgedeckt, permanent */
	private static final int AUFGEDECKT_PERMANENT = 2;
	
	/** Die Pause zwischen zwei Spielzügen (hängt vom gewählten Schwierigkeitsgrad ab) */
	public int pause;
	
	/** Breite * Höhe des Spielfeldes */
	private int dim;
	
	/** Name des Kartendecks */
	private String deck;
	
	/** Array mit den Nummern der Karten */
	private int[] feld;
	
	/** Array mit den Drawables der aufgedeckten Karten */
	private Drawable[] karten;
	
	/** Das Drawable für Karten-Rückseiten */
	private Drawable untouched;
	
	/** Array mit den Feldstati */
	private int[] feldStatus;
	
	/** True, wenn das Spielfeld gesperrt ist (keine Interaktionen möglich), sonst false */
	public boolean locked;
	
	/** Speichert das Ergebnis der letzten Überprüfung von zwei Zügen. */
	public boolean lastFoundPair;
	
	/**
	 * Initialisiert das Spielfeld-Objekt.
	 * 
	 * Im Anfangszustand sind alle Karten zugedeckt und nicht anklickbar.
	 * 
	 * @param ctx Application Context
	 * @param breite Die Breite des Spielfelds.
	 * @param hoehe Die Hoehe des Spielfelds.
	 * @param feld Ein Array mit den Karten-IDs.
	 * @param deck Das Deck, mit dem gespielt wird.
	 * @param pause Die Pause zwischen zwei Spielzügen.
	 * @throws FileNotFoundException Wenn das Deck nicht installiert ist.
	 */
	private Spielfeld(Context ctx, int breite, int hoehe, int[] feld, String deck, int pause) throws FileNotFoundException {
		this(ctx, breite * hoehe, feld, deck, pause);
	}
	
	/**
	 * Initialisiert das Spielfeld-Objekt.
	 * 
	 * Im Anfangszustand sind alle Karten zugedeckt und nicht anklickbar.
	 * 
	 * @param ctx Application Context
	 * @param dim Höhe * Breite des Spielfelds.
	 * @param feld Ein Array mit den Karten-IDs.
	 * @param deck Das Deck, mit dem gespielt wird.
	 * @param pause Die Pause zwischen zwei Spielzügen.
	 * @throws FileNotFoundException Wenn das Deck nicht installiert ist.
	 */
	private Spielfeld(Context ctx, int dim, int[] feld, String deck, int pause) throws FileNotFoundException {
		this.dim = dim;
		this.feld = feld;
		this.locked = true;
		this.deck = deck;
		this.pause = pause;
		this.lastFoundPair = false;
		
		// Feldstatus setzen und Drawables erzeugen
		feldStatus = new int[dim];
		karten = new Drawable[dim];
		File pfad = new File(
				ctx.getDir(GlobalValues.SHARED_PREFS_DECKS, Context.MODE_WORLD_READABLE).getAbsolutePath() + "/" +
				deck
		);
		
		// Prüfen, ob das Deck-Verzeichnis existiert
		if (!pfad.exists()) throw new FileNotFoundException();
		
		untouched = Drawable.createFromPath(new File(pfad, "untouched.png").getAbsolutePath());
		for (int i = 0; i < (dim); i++) {
			feldStatus[i] = ZUGEDECKT;
		
			// Pfad / gewünschten Dateinamen zusammenbauen
			String dateiname = feld[i] + ".png";
			File karte = new File(pfad, dateiname);
			
			// Drawable erzeugen und zurückgeben
			karten[i] = Drawable.createFromPath(karte.getAbsolutePath());
		}
	}
	
	/**
	 * Generiert eine neues Spielfeld.
	 * 
	 * @param ctx Application Context
	 * @param breite Die Breite des Spielfelds.
	 * @param hoehe Die Hoehe des Spielfelds.
	 * @param deck Das Deck, mit dem gespielt wird.
	 * @param pause Die Pause zwischen zwei Spielzügen.
	 * @return Das neu generierte Spielfeld.
	 * @throws FileNotFoundException Wenn das Deck nicht installiert ist.
	 */
	public static Spielfeld generate(Context ctx, int breite, int hoehe, String deck, int pause) throws FileNotFoundException {
		// Feld generieren
		int[] feld = new int[breite * hoehe];

		// Karten auf das Spielfeld legen und dann durchmischen 
		for (int i = 0; i < (breite * hoehe); i += 2) {
			feld[i] = i / 2;
			feld[i + 1] = i / 2;
		}
		mische(feld);
			
		return new Spielfeld(ctx, breite, hoehe, feld, deck, pause);
	}
	
	/**
	 * Erzeugt das Spielfeld aus einem JSON-String.
	 * 
	 * @param ctx Application Context.
	 * @param jsonS Der String in JSON-Kodierung.
	 * @return Das Spielfeld-Objekt.
	 * @throws JSONException Wenn ein Fehler bei der Verarbeitung der JSON-Daten auftritt.
	 * @throws FileNotFoundException Wenn das Deck nicht installiert ist.
	 * @see http://json.org/
	 */
	public static Spielfeld createFromJSON(Context ctx, String jsonS) throws JSONException, FileNotFoundException {
		// Übergeordnetes Objekt auslesen
		JSONObject json = new JSONObject(jsonS);
		
		// Die Arrays für Kartenverteilung und Feld-Stati auslesen und zu "echten" Arrays umbauen
		JSONArray jsonFeld = json.getJSONArray("feld");
		JSONArray jsonFeldStatus = json.getJSONArray("feldStatus");
		
		int[] feld = new int[jsonFeld.length()];
		int[] feldStatus = new int[jsonFeld.length()];
		
		for (int i = 0; i < jsonFeld.length(); i++) {
			feld[i]       = jsonFeld.getInt(i);
			feldStatus[i] = jsonFeldStatus.getInt(i);
		}
		
		// Das Spielfeld generieren, Feld-Stati setzen und das neue Spielfeld zurückgeben
		Spielfeld spielfeld = new Spielfeld(ctx, json.getInt("dim"), feld, json.getString("deck"), json.getInt("pause"));
		spielfeld.feldStatus = feldStatus;
		
		return spielfeld;
	}
	
	/**
	 * Erzeugt ein JSON-Objekt mit den relevanten Spielfeld-Daten.
	 * 
	 * Aus der Stringrepräsentation des JSON-Objekts kann mit Spielfeld.createFromJSON() wieder
	 * das Spielfeld erstellt werden. Voraussetzung ist, dass das Kartendeck auf dem Gerät existiert.
	 * 
	 * @return Die String-Repräsentation des JSON-Objekts, welches das Spielfeld enthält.
	 */
	public String toJSON() {
		JSONObject json = new JSONObject();
		try {
			// Kartenverteilung in Liste speichern
			List<Integer> feld = new ArrayList<Integer>();
			for (int kartenID: this.feld) feld.add(kartenID);
			
			// Feldstati in Liste speichern
			List<Integer> feldStatus = new ArrayList<Integer>();
			for (int kartenStatus: this.feldStatus) feldStatus.add(kartenStatus);
			
			json.put("dim", this.dim);
			json.put("deck", this.deck);
			json.put("feld", new JSONArray(feld));
			json.put("feldStatus", new JSONArray(feldStatus));
			json.put("pause", pause);
		} catch (JSONException e) {
			json = new JSONObject();
		}
		
		return json.toString();
	}
	
	/**
	 * Erzeugt ein Drawable-Objekt mit der aktuellen Darstellung der Karte anhand
	 * des Decks.
	 * 
	 * @param zeile Zeile der Karte auf dem Spielfeld
	 * @param spalte Spalte der Karte auf dem Spielfeld
	 * @return Das Drawable-Objekt mit der entsprechenden Grafik
	 */
	public Drawable getKarte(int pos) {
		return ((feldStatus[pos] == AUFGEDECKT_TEMPORARY) || (feldStatus[pos] == AUFGEDECKT_PERMANENT))
			? karten[pos]
			: untouched;
	}
	
	/**
	 * Erzeugt einen Array mit den Drawables für das gesamte Spielfeld.
	 * 
	 * @return Das Spielfeld in Form eines Drawable-Arrays.
	 */
	public Drawable[] getKarten() {
		Drawable[] ergebnis = new Drawable[dim];
		
		for (int pos = 0; pos < dim; pos++)
			ergebnis[pos] = getKarte(pos);
		
		return ergebnis;
	}
	
	/**
	 * "Klickt" eine Karte auf dem Spielfeld an.
	 * 
	 * Der neue Status der Karte wird anhand ihres aktuellen Status ermittelt:
	 * 
	 * <table>
	 *   <tr><th>Karte ist</th><th>Karte wird sein</th></tr>
	 *   <tr><td>Aufgedeckt, anklickbar</td><td>Aufgedeckt, anklickbar</td></tr>
	 *   <tr><td>Aufgedeckt, nicht anklickbar</td><td>Aufgedeckt, nicht anklickbar</td></tr>
	 *   <tr><td>Zugedeckt</td><td>Aufgedeckt</td><tr>
	 * </table>
	 * 
	 * @param pos Die zu "klickende" Karte.
	 */
	public void touch(int pos) {
		int status;
		switch (feldStatus[pos]) {
			case AUFGEDECKT_TEMPORARY:
				status = AUFGEDECKT_TEMPORARY;
			break;
			
			case AUFGEDECKT_PERMANENT:
				status = AUFGEDECKT_PERMANENT;
			break;
			
			case ZUGEDECKT:
				status = AUFGEDECKT_TEMPORARY;
			break;
			
			default:
				status = ZUGEDECKT;
		}
		
		feldStatus[pos] = status;
	}
	
	/**
	 * Testet, ob ein Zug gültig ist.
	 * 
	 * Bei Zügen von "extern" muss das Spielfeld gesperrt bleiben, die Sperrung darf
	 * aber nicht die Gültigkeit des Zuges beeinflussen. Um diese Einstellung zu
	 * ignorieren, muss der zweite Parameter auf "true" gesetzt werden.
	 * 
	 * @param pos Die "geklickte" Karte.
	 * @param lockOverride true, um die Spielfeldsperrung zu ignorieren, sonst false
	 * @return true, wenn die Karte anklickbar ist, sonst false.
	 */
	public boolean check(int pos, boolean lockOverride) {
		Log.d(TAG, "Klick auf " + pos + " mit Status " + feldStatus[pos] + " und locked == " + locked + "; lockOverride = " + lockOverride);
		return ((!locked || lockOverride) && (feldStatus[pos] == ZUGEDECKT));
	}
	
	/**
	 * Überprüft, ob zwei Karten ein Paar sind.
	 * 
	 * @param pos1 Position der ersten Karte.
	 * @param pos2 Position der zweiten Karte.
	 * @return true, wenn die Karten ein Paar sind, sonst false.
	 */
	public boolean checkPair(int pos1, int pos2) {
		lastFoundPair = (feld[pos1] == feld[pos2]);
		
		// Paare permanent aufdecken
		if (lastFoundPair) {
			feldStatus[pos1] = AUFGEDECKT_PERMANENT;
			feldStatus[pos2] = AUFGEDECKT_PERMANENT;
		}
		
		return lastFoundPair;
	}
	
	/**
	 * Deckt die übergebenen Karten wieder zu, wenn sie kein Paar sind.
	 * 
	 * Die beiden Karten sollten immer die der letzten Runde sein. Die Methode
	 * überprüft das nicht!
	 */
	public void zudecken() {
		// Felder wieder zudecken oder permanent aufgedeckt lassen
		for (int i = 0; i < feldStatus.length; i++) {
			switch (feldStatus[i]) {
				case AUFGEDECKT_TEMPORARY: feldStatus[i] = ZUGEDECKT; break;
				case AUFGEDECKT_PERMANENT: feldStatus[i] = AUFGEDECKT_PERMANENT; break;
				case ZUGEDECKT: feldStatus[i] = ZUGEDECKT; break;
			}
		}
		lastFoundPair = false;
	}
	
	/**
	 * Prüft, ob alle Karten permanent aufgedeckt wurden (sprich: ob das Spiel zuende ist)
	 * 
	 * @return true, wenn das Spiel zuende ist, sonst false.
	 */
	public boolean isSpielZuende() {
		boolean zuende = true;
		String logMsg = "Spielfeld-Status: ";
		for (int status: feldStatus) {
			zuende = zuende && (status == AUFGEDECKT_PERMANENT);
			logMsg += status + ", ";
		}
		Log.d(TAG, logMsg);
		return zuende;
	}

	/**
	 * Fisher–Yates bzw. Knuth shuffle
	 * 
	 * @param array - indizes aus Bilderlisten Feld
	 * @see http://en.wikipedia.org/wiki/Fisher-Yates_shuffle
	 */
	private static void mische(int[] array) {
		Random rnd = new Random();
		for (int i = array.length - 1; i >= 0; i--) {
			int index = rnd.nextInt(i + 1);
			// umtauschen
			int a = array[index];
			array[index] = array[i];
			array[i] = a;
		}
	}
}