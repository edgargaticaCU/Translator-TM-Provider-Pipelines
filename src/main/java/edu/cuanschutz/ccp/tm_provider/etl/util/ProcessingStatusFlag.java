package edu.cuanschutz.ccp.tm_provider.etl.util;

public enum ProcessingStatusFlag {
	/**
	 * 
	 */
	TEXT_DONE,
	/**
	 * true if dependency parsing is complete
	 */
	DP_DONE,

	////////////////////////////////////////////////////////////////
	/////////////////// OGER CONCEPT PROCESSING ////////////////////
	////////////////////////////////////////////////////////////////

	/**
	 * 
	 */
	OGER_CHEBI_DONE,
	/**
	 * 
	 */
	OGER_CL_DONE,
	/**
	 * 
	 */
	OGER_GO_BP_DONE,
	/**
	 * 
	 */
	OGER_GO_CC_DONE,
	/**
	 * 
	 */
	OGER_GO_MF_DONE,
	/**
	 * 
	 */
	OGER_MOP_DONE,
	/**
	 * 
	 */
	OGER_NCBITAXON_DONE,
	/**
	 * 
	 */
	OGER_SO_DONE,
	/**
	 * 
	 */
	OGER_PR_DONE,
	/**
	 * 
	 */
	OGER_UBERON_DONE,

	////////////////////////////////////////////////////////////////
	////////////////// BERT CONCEPT PROCESSING /////////////////////
	////////////////////////////////////////////////////////////////

	/**
	 * 
	 */
	BERT_CHEBI_DONE,
	/**
	 * 
	 */
	BERT_CL_DONE,
	/**
	 * 
	 */
	BERT_GO_BP_DONE,
	/**
	 * 
	 */
	BERT_GO_CC_DONE,
	/**
	 * 
	 */
	BERT_GO_MF_DONE,
	/**
	 * 
	 */
	BERT_MOP_DONE,
	/**
	 * 
	 */
	BERT_NCBITAXON_DONE,
	/**
	 * 
	 */
	BERT_SO_DONE,
	/**
	 * 
	 */
	BERT_PR_DONE,
	/**
	 * 
	 */
	BERT_UBERON_DONE,

	////////////////////////////////////////////////////////////////
	//////////////////// RELATION PROCESSING ///////////////////////
	////////////////////////////////////////////////////////////////

	/**
	 * 
	 */
//	ASSOC_CHEMICAL_PROTEIN_DONE

}
