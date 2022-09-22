package edu.isi.karma.rep;

import com.google.gson.reflect.TypeToken;
import edu.isi.karma.util.GsonUtil;
import eu.trentorise.opendata.semtext.Meaning;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

/**
 * Cell value for concepts. it extend {@link CellValue} and provide different methods to get different versions of
 * cell value while publishing and displaying the data.
 *
 * @author Danish danishasghar.cheema@studenti.unitn.it
 */
public class ConceptCellValue extends CellValue{

	private List<Concept> concepts = new LinkedList<>();

	public ConceptCellValue(List<Concept> concepts) {
		super();
		this.concepts = concepts;
	}

	public ConceptCellValue() {
		super();
	}

	public List<Concept> getConcepts() {
		return concepts;
	}

	public void setConcepts(List<Concept> concepts) {
		this.concepts = concepts;
	}

	@Override
	public String asString() {
		return getSelectedConceptAsString();
	}

	/**
	 * get only the selected concept.
	 * this method must be used when publishing or exporting the data,
	 * specially where irrelevant concepts are no longer required to be attached to row.
	 *
	 * @return serialized {@link Concept} object
	 */
	public String getSelectedConceptAsString() {
		for(Concept obj: concepts) {
			if (obj.getIsSelected()) {
				return GsonUtil.INSTANCE.getImmutableGsonObj().toJson(obj.getMeaning(), Meaning.class);
			}
		}
		return getEmptyValue().asString();
	}

	/**
	 * get all the concepts attached to a row. should only be used when we want to display
	 * all the possible {@link Concept} included selected one.
	 *
	 * @return serialized list of {@link Concept}.
	 */
	public String getConceptsAsString() {
		Type type = new TypeToken<List<Concept>>() {}.getType();
		return GsonUtil.INSTANCE.getImmutableGsonObj().toJson(this.concepts,type);
	}

}
