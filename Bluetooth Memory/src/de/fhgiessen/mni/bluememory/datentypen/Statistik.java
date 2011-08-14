package de.fhgiessen.mni.bluememory.datentypen;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * 
 * @author Sergei Jochim
 * 
 * Statistik verwaltet die statistischen Daten des Spielers und synchronisiert diese mit
 * der Statistik in den SharedPreferences.
 */
public class Statistik {
	private static final String TAG = "BlueMemory.datentyp.Statistik";
	private static Statistik singleton;
	
	Context context;
	SharedPreferences sharedprefs;

	public static final int GEWONNEN = 0;
	public static final int UNENTSCHIEDEN = 1;
	public static final int VERLOREN = 2;
	
	public static final int RESET_VALUE = 0;

	public int klicks;
	public int paare;
	public int gesamt;
	public int gewonnen;
	public int unentschieden;
	public int verloren;
	public String spielername;

	/**
	 * Holt sich aus den SharedPreferences die aktuellen Werte.
	 * @param ctx
	 */
	private Statistik(Context ctx) {
		this.context = ctx;

		sharedprefs = context.getSharedPreferences(GlobalValues.STATS_SAVE_FILE, 0);

		klicks = sharedprefs.getInt("stat_klicks", 0);
		paare = sharedprefs.getInt("stat_paare", 0);
		gesamt = sharedprefs.getInt("stat_gesamt", 0);
		gewonnen = sharedprefs.getInt("stat_gewonnen", 0);
		unentschieden = sharedprefs.getInt("stat_unentschieden", 0);
		verloren = sharedprefs.getInt("stat_verloren", 0);
		spielername = sharedprefs.getString("stat_name", standardName());
	}
	
	/**
	 * 
	 * @param ctx
	 * @return
	 */
	public static Statistik getInstance(Context ctx) {
		if (singleton == null) singleton = new Statistik(ctx);
		return singleton;
	}
	/**
	 * updateStats aktualisiert die Statitik des Spielers.
	 * 
	 * @param klicks Anzahl gemachter Klicks.
	 * @param paare Anzahl richtiger Paare.
	 * @param platzierung Hat der Spieler GEWONNEN(=0) / VERLOREN(=2) / UNTENSCHIEDEN(=1) gespielt.
	 */
	public void updateStats(int klicks, int paare, int platzierung) {
		this.klicks += klicks;
		this.paare += paare;
		this.gesamt++;

		switch (platzierung) {
			case GEWONNEN: gewonnen++; break;
			case VERLOREN: verloren++; break;
			case UNENTSCHIEDEN: unentschieden++; break;
		}
		
		// Editor der SharedPreferences wird geladen, um die neuen Inhalte
		// abzuspeichern.
		SharedPreferences.Editor editor = sharedprefs.edit();

		// Alle Einträge auf 0 zurücksetzen
		editor.putInt("stat_klicks", this.klicks);
		editor.putInt("stat_paare", this.paare);
		editor.putInt("stat_gesamt", this.gesamt);
		editor.putInt("stat_gewonnen", this.gewonnen);
		editor.putInt("stat_unentschieden", this.unentschieden);
		editor.putInt("stat_verloren", this.verloren);
		
		editor.commit();
	}
	/**
	 * Setzt alle Statistik-Wert+Name auf Standard-Werte zurück. Speichert diese
	 * in den SharedPreferences.
	 */
	public void resetStats() {
		
		// Editor der SharedPreferences wird geladen, um die neuen Inhalte
		// abzuspeichern.
		SharedPreferences.Editor editor = sharedprefs.edit();

		// Alle Einträge auf 0 zurücksetzen
		editor.putInt("stat_klicks", RESET_VALUE);
		editor.putInt("stat_paare", RESET_VALUE);
		editor.putInt("stat_gesamt", RESET_VALUE);
		editor.putInt("stat_gewonnen", RESET_VALUE);
		editor.putInt("stat_unentschieden", RESET_VALUE);
		editor.putInt("stat_verloren", RESET_VALUE);
		editor.putString("stat_name", standardName());

		// Einträge speichern
		editor.commit();
		
		// Alles Varibalen von Singleton auch auf Standardwerte runter setzen.
		klicks = paare = gesamt = gewonnen = unentschieden = verloren = RESET_VALUE;
		spielername = standardName();
	}
	
	/**
	 * Speichert den aktuellen Namen.
	 */
	public void updateName(){
		SharedPreferences.Editor editor = sharedprefs.edit();
		editor.putString("stat_name", this.spielername);
		editor.commit();
	}
	
	/**
	 * Funktion zum ermitteln des Standard-Namen eines Spielers. Es wird in der
	 * nach einem Namen des Bluetooth-Geräts gesucht, ansonst wird ein "Kein Name"
	 * als String zurück gegeben.
	 * 
	 * @return String, der den Standard-Namen repräsentiert.
	 */
	private String standardName() {
		if ((BluetoothAdapter.getDefaultAdapter() != null)
		&& (BluetoothAdapter.getDefaultAdapter().getName() != null)) {
			Log.d(TAG, "Name aus BT-Adapter.");
			return BluetoothAdapter.getDefaultAdapter().getName();
		} else {
			Log.d(TAG, "Standard-Name.");
			return "Kein Name";
		}
	}
}