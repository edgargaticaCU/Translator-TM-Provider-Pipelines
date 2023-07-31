package edu.cuanschutz.ccp.tm_provider.oger.dict;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import edu.cuanschutz.ccp.tm_provider.oger.util.OgerDictFileFactory;

public class GoCcOgerDictFileFactory extends OgerDictFileFactory {

	private static final String MOLECULAR_FUNCTION = "http://purl.obolibrary.org/obo/GO_0003674";
	private static final String BIOLOGICAL_PROCESS = "http://purl.obolibrary.org/obo/GO_0008150";

	public GoCcOgerDictFileFactory() {
		super("cellular_component", "GO_CC", SynonymSelection.EXACT_ONLY,
				Arrays.asList(MOLECULAR_FUNCTION, BIOLOGICAL_PROCESS));
	}

	private static final Set<String> IRIS_TO_EXCLUDE = new HashSet<String>(Arrays.asList());

	@Override
	protected Set<String> augmentSynonyms(String iri, Set<String> syns) {
		Set<String> toReturn = removeStopWords(syns);
		toReturn = removeWordsLessThenLength(toReturn, 3);

		if (iri.contentEquals("http://purl.obolibrary.org/obo/GO_0005730")) { // nucleolus
			syns.add("nucleoli");
		}

		if (IRIS_TO_EXCLUDE.contains(iri)) {
			toReturn = Collections.emptySet();
		}

		return toReturn;
	}

}
