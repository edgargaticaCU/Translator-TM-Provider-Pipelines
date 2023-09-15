package edu.cuanschutz.ccp.tm_provider.etl.util;

public enum PipelineKey {
	BIOC_TO_TEXT, DEPENDENCY_PARSE, ORIG, OGER, // BERT_IDS, BERT_SPANS,
	BIGQUERY_EXPORT, FILE_LOAD, PUBANNOTATION_EXPORT, DRY_RUN, SENTENCE_SEGMENTATION, SENTENCE_COOCCURRENCE_EXPORT,
	MEDLINE_XML_TO_TEXT, CRF, SENTENCE_EXTRACTION, CONCEPT_COOCCURRENCE_METRICS, BIORXIV_XML_TO_TEXT,
	CONCEPT_COOCCURRENCE_COUNTS, CONCEPT_POST_PROCESS, CONCEPT_ANNOTATION_EXPORT,
	WEBANNO_SENTENCE_EXTRACTION, CONCEPT_COUNT_DISTRIBUTION, CLASSIFIED_SENTENCE_STORAGE,
	UPDATE_MEDLINE_STATUS_ENTITIES, CONCEPT_IDF, ELASTICSEARCH_LOAD, ABBREVIATION, DOC_TEXT_AUGMENTATION,
	DEPENDENCY_PARSE_TO_SENTENCE, TEXT_EXPORT, DEPENDENCY_PARSE_IMPORT, DEPENDENCY_PARSE_TO_CONLL03,
	FILTER_UNACTIONABLE_TEXT, TEST, OGER_POST_PROCESS, COLLECTION_ASSIGNMENT
}
