package edu.cuanschutz.ccp.tm_provider.etl.util;

public enum DocumentType {
	BIOC, TEXT, SECTIONS, DEPENDENCY_PARSE, CONCEPT_CHEBI, CONCEPT_CL, CONCEPT_DRUGBANK, CONCEPT_GO_BP, CONCEPT_GO_CC, CONCEPT_GO_MF,
	CONCEPT_HP, CONCEPT_MONDO, CONCEPT_MOP, CONCEPT_MP, CONCEPT_NCBITAXON, CONCEPT_PR, CONCEPT_SO, CONCEPT_UBERON, CRF_CHEBI, CRF_NLMCHEM,
	CRF_CL, CRF_GO_BP, CRF_GO_CC, CRF_GO_MF, CRF_HP, CRF_MONDO, CRF_MOP, CRF_NCBITAXON, CRF_PR, CRF_SO, CRF_UBERON,
	BIGQUERY, PUBANNOTATION, SENTENCE, SENTENCE_COOCCURRENCE, CONCEPT_ALL, CONCEPT_ALL_UNFILTERED, NGD_COUNTS, ELASTIC, ABBREVIATIONS
}
