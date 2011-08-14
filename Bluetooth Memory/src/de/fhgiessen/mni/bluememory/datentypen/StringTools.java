package de.fhgiessen.mni.bluememory.datentypen;

import java.util.List;

/**
 * Kleine Tools, die das Leben mit Strings leichter machen.
 */
public abstract class StringTools {
	/**
	 * Implementierung der "implode()"-Funktion aus PHP
	 * 
	 * Setzt die Teile eines String-Arrays zu einem String zusammen.
	 * Zwischen den Stringteilen wird der "glue" eingesetzt.
	 * 
	 * @param stringArray Der String-Array.
	 * @param glue Das Verbindungsstück zwischen zwei String-Teilen
	 * @return implode(new String[] {"1", "2", "3"}, ","); ==> "1,2,3"
	 * @see http://imwill.com/implode-string-array-java/
	 */
	public static String implode(String[] stringArray, String glue) {
		// Bei leerem String-Array einen leeren String zurückgeben
		if (stringArray.length < 0) return "";
		
		// String zusammenbauen
		StringBuilder string = new StringBuilder();
		string.append(stringArray[0]);
		for (int i = 1; i < stringArray.length; i++) {
			string.append(glue);
			string.append(stringArray[i]);
		}
		
		return string.toString();
	}
	
	/**
	 * Implementierung der "implode()"-Funktion aus PHP
	 * 
	 * Setzt die Teile einer String-Liste zu einem String zusammen.
	 * Zwischen den Stringteilen wird der "glue" eingesetzt.
	 * 
	 * @param stringArray Der String-Array.
	 * @param glue Das Verbindungsstück zwischen zwei String-Teilen
	 * @return implode(new String[] {"1", "2", "3"}, ","); ==> "1,2,3"
	 */
	public static String implode(List<String> stringList, String glue) {
		return implode(stringList.toArray(new String[0]), glue);
	}
}
