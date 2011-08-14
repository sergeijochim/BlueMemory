package de.fhgiessen.mni.bluememory.datentypen;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

/**
 * Adapter für die GridView, die das Spielfeld darstellt.
 * 
 * @see http://developer.android.com/resources/tutorials/views/hello-gallery.html
 *
 */
public class ImageAdapter extends BaseAdapter {
	private Context mContext;

	// references to our images
	public Drawable[] feld;

	/** 
	 * Adapter wird dazu benutzt eine Liste von Fotos zu verbinden
	 * @param c - Aktueller Kontext
	 */
	public ImageAdapter(Context c) {
		mContext = c;
		this.feld = new Drawable[0];
	}

	/**
	 * @return Gesamtbildanzahl zurückgeben 
	 */
	public int getCount() {
		return feld.length;
	}

	/**
	 * @param position - Position des Bildobjektes im Array, das zurückgegeben werden soll
	 * @return Das Bild-Objekt
	 */
	public Object getItem(int position) {
		return feld[position];
	}

	/**
	 * @param position
	 * @return Indexwert des Bildobjektes im Array
	 */
	public long getItemId(int position) {
		return position;
	}

	/**
	 * Für jedes vom ImageAdapter referenzierte Objekt eine neue View erzeugen
	 * @param position
	 * @param convertView
	 * @param parent
	 * @return neue ImageView zurückgeben
	 */
	public View getView(int position, View convertView, ViewGroup parent) {
		/**
		 * if   - Wenn die View noch nicht erzeugt wurde, initialisiere attribute mit Standardwerten,
		 * else - ansonsten direkt die aktuelle View auf die neue View setzen.
		 */
		ImageView imageView;
		if (convertView == null) {  // if it's not recycled, initialize some attributes
			imageView = new ImageView(mContext);
			imageView.setLayoutParams(new GridView.LayoutParams(128, 128));
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
			imageView.setPadding(8, 8, 8, 8);
		} else {
			imageView = (ImageView) convertView;
		}

		imageView.setImageDrawable(feld[position]);
		return imageView;
	}
}