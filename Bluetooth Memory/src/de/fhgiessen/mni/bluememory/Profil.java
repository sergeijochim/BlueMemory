package de.fhgiessen.mni.bluememory;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import de.fhgiessen.mni.bluememory.R.layout;
import de.fhgiessen.mni.bluememory.datentypen.GlobalValues;
import de.fhgiessen.mni.bluememory.datentypen.Statistik;

/**
 * Statistik
 * 
 * In der Statistik werden Werte der gespielten Runden festegehalten. Diese werden
 * mit Hilfe von SharedPreferences gespeichert.
 *
 * @author Sergei Jochim
 */
public class Profil extends Activity {
	/** Singleton Objekt von Stats, welches die gesamte Statistik verwaltet*/
	Statistik stats;
	
	// TextViews aus dem Layout, die mit den Statistikwerten belegt werden sollen.
	TextView view_klicks;
	TextView view_paare;
	TextView view_gesamt;
	TextView view_gewonnen;
	TextView view_unentschieden;
	TextView view_verloren;
	EditText edit_name;

	/**
	 * onCreate
	 * 
	 * Wird beim erstellen der Activity gestartet und das dazugehörige Layout
	 * geladen.
	 * 
	 * Diese Methode initialisiert alle TextView-Elemente aus dem Statistik
	 * Layout und belegt diese mit den aktuellen Werten, die in den
	 * SharedPreferences gespeichert sind.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(layout.statistik);
		setTitle(GlobalValues.TITEL + "Profil");
		
		// Instanz von den Statistiken holen
		stats = Statistik.getInstance(this);

		//config = getSharedPreferences(GlobalValues.STATS_SAVE_FILE, 0);

		view_klicks = (TextView) findViewById(R.id.view_anzahl_klicks);
		view_paare = (TextView) findViewById(R.id.view_anzahl_richtiger_paare);
		view_gesamt = (TextView) findViewById(R.id.view_spiele_gesamt);
		view_gewonnen = (TextView) findViewById(R.id.view_spiele_gewonnen);
		view_unentschieden = (TextView) findViewById(R.id.view_spiele_unentschieden);
		view_verloren = (TextView) findViewById(R.id.view_spiele_verloren);
		
		edit_name = (EditText) findViewById(R.id.edit_spielername);

		view_klicks.setText(""+stats.klicks);
		view_paare.setText(""+stats.paare);
		view_gesamt.setText(""+stats.gesamt);
		view_gewonnen.setText(""+stats.gewonnen);
		view_unentschieden.setText(""+stats.unentschieden);
		view_verloren.setText(""+stats.verloren);
		
		edit_name.setText(stats.spielername);

		super.onCreate(savedInstanceState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.statistik, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_statistik_loeschen:
			reset();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * resetStatus
	 * 
	 * Diese Methode hat einzig und allein den den Zweck alle Werte in der Statistik
	 * auf 0 zu setzen.
	 */
	private void reset() {
		// Der Wert, mit dem alle Einträge zurückgesetzt worden ist.
		stats.resetStats();
		
		// Die Einträge der aktuellen Views aktualisieren mit dem neuen Wert.
		view_klicks.setText("" + stats.klicks);
		view_paare.setText("" + stats.paare);
		view_gesamt.setText("" + stats.gesamt);
		view_gewonnen.setText("" + stats.gewonnen);
		view_unentschieden.setText("" + stats.unentschieden);
		view_verloren.setText("" + stats.verloren);
		edit_name.setText(""+stats.spielername);
	}
	
	public void onButtonSaveClick(View target) {
		switch(target.getId()) {
		case R.id.btn_save: 
			//Neuen Name setzen
			stats.spielername = edit_name.getText().toString(); 
			//Neuen Namen speichern
			stats.updateName();
			Toast.makeText(this, "Neuer Name gespeichert", Toast.LENGTH_SHORT).show();
			break;
		}
	}
}