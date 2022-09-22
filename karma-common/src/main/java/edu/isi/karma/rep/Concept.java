package edu.isi.karma.rep;

import eu.trentorise.opendata.semtext.Meaning;

/**
 * java Pojo with {@link Meaning} and a boolean variable.
 *
 * @author Danish danishasghar.cheema@studenti.unitn.it
 */
public class Concept {

	private Meaning meaning;
	private boolean isSelected;

	public Concept() { }

	public Concept(Meaning meaning, boolean isSelected) {
		this.meaning = meaning;
		this.isSelected=isSelected;
	}

	public boolean getIsSelected() { return isSelected; }
	public void setIsSelected(boolean isSelected) { this.isSelected = isSelected; }
	public Meaning getMeaning() { return meaning; }
	public void setMeaning(Meaning meaning) { this.meaning = meaning; }

}
