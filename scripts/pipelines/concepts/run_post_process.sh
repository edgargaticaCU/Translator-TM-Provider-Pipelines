#!/bin/sh

PROJECT=$1
COLLECTION=$2
STAGE_LOCATION=$3
TMP_LOCATION=$4
BUCKET=$5
TEXT_PIPELINE_KEY=$6
FILTER_FLAG=$7


JOB_NAME=$(echo "CONCEPT-POST-PROCESS-${COLLECTION}" | tr '_' '-')

# if filter flag is BY_CRF then we need to collect the CRF annotations as well as the concept annotations and the target processing status flag = CONCEPT_POST_PROCESSING_DONE
# if the filter flag is NONE then we don't need the CRF annotations and the target processing status flag = CONCEPT_POST_PROCESSING_UNFILTERED_DONE
if [ $FILTER_FLAG == 'BY_CRF' ]
then
	INPUT_DOC_CRITERIA="TEXT|TEXT|${TEXT_PIPELINE_KEY}|0.1.0;CONCEPT_DRUGBANK|BIONLP|OGER|0.1.0;CONCEPT_CHEBI|BIONLP|OGER|0.1.0;CRF_CHEBI|BIONLP|CRF|0.1.0;CONCEPT_PR|BIONLP|OGER|0.1.0;CRF_PR|BIONLP|CRF|0.1.0;CONCEPT_CL|BIONLP|OGER|0.1.0;CRF_CL|BIONLP|CRF|0.1.0;CONCEPT_UBERON|BIONLP|OGER|0.1.0;CRF_UBERON|BIONLP|CRF|0.1.0;CONCEPT_GO_BP|BIONLP|OGER|0.1.0;CRF_GO_BP|BIONLP|CRF|0.1.0;CONCEPT_GO_CC|BIONLP|OGER|0.1.0;CRF_GO_CC|BIONLP|CRF|0.1.0;CONCEPT_GO_MF|BIONLP|OGER|0.1.0;CRF_GO_MF|BIONLP|CRF|0.1.0;CONCEPT_SO|BIONLP|OGER|0.1.0;CRF_SO|BIONLP|CRF|0.1.0;CONCEPT_NCBITAXON|BIONLP|OGER|0.1.0;CRF_NCBITAXON|BIONLP|CRF|0.1.0;CONCEPT_HP|BIONLP|OGER|0.1.0;CRF_HP|BIONLP|CRF|0.1.0;CONCEPT_MONDO|BIONLP|OGER|0.1.0;CRF_MONDO|BIONLP|CRF|0.1.0;CONCEPT_MOP|BIONLP|OGER|0.1.0;CRF_MOP|BIONLP|CRF|0.1.0"
	TARGET_PROCESSING_STATUS_FLAG=CONCEPT_POST_PROCESSING_DONE
else
	INPUT_DOC_CRITERIA="TEXT|TEXT|${TEXT_PIPELINE_KEY}|0.1.0;CONCEPT_DRUGBANK|BIONLP|OGER|0.1.0;CONCEPT_CHEBI|BIONLP|OGER|0.1.0;CONCEPT_PR|BIONLP|OGER|0.1.0;CONCEPT_CL|BIONLP|OGER|0.1.0;CONCEPT_UBERON|BIONLP|OGER|0.1.0;CONCEPT_GO_BP|BIONLP|OGER|0.1.0;CONCEPT_GO_CC|BIONLP|OGER|0.1.0;CONCEPT_GO_MF|BIONLP|OGER|0.1.0;CONCEPT_SO|BIONLP|OGER|0.1.0;CONCEPT_NCBITAXON|BIONLP|OGER|0.1.0;CONCEPT_HP|BIONLP|OGER|0.1.0;CONCEPT_MONDO|BIONLP|OGER|0.1.0;CONCEPT_MOP|BIONLP|OGER|0.1.0"
	TARGET_PROCESSING_STATUS_FLAG=CONCEPT_POST_PROCESSING_UNFILTERED_DONE
fi

echo "COLLECTION: $COLLECTION"
echo "PROJECT: $PROJECT"
echo "JOB_NAME: $JOB_NAME"
echo "FILTER_FLAG: $FILTER_FLAG"
echo "TARGET_PROCESSING_STATUS_FLAG: $TARGET_PROCESSING_STATUS_FLAG"
echo "INPUT_DOC_CRITERIA: $INPUT_DOC_CRITERIA"

#INPUT_DOC_CRITERIA="TEXT|TEXT|${TEXT_PIPELINE_KEY}|0.1.0;CONCEPT_CHEBI|BIONLP|OGER|0.1.0;CRF_CHEBI|BIONLP|CRF|0.1.0;CONCEPT_PR|BIONLP|OGER|0.1.0;CRF_PR|BIONLP|CRF|0.1.0;CONCEPT_CL|BIONLP|OGER|0.1.0;CRF_CL|BIONLP|CRF|0.1.0;CONCEPT_UBERON|BIONLP|OGER|0.1.0;CRF_UBERON|BIONLP|CRF|0.1.0;CONCEPT_GO_BP|BIONLP|OGER|0.1.0;CRF_GO_BP|BIONLP|CRF|0.1.0;CONCEPT_GO_CC|BIONLP|OGER|0.1.0;CRF_GO_CC|BIONLP|CRF|0.1.0;CONCEPT_GO_MF|BIONLP|OGER|0.1.0;CRF_GO_MF|BIONLP|CRF|0.1.0;CONCEPT_SO|BIONLP|OGER|0.1.0;CRF_SO|BIONLP|CRF|0.1.0;CONCEPT_NCBITAXON|BIONLP|OGER|0.1.0;CRF_NCBITAXON|BIONLP|CRF|0.1.0;CONCEPT_HP|BIONLP|OGER|0.1.0;CRF_HP|BIONLP|CRF|0.1.0;CONCEPT_MONDO|BIONLP|OGER|0.1.0;CRF_MONDO|BIONLP|CRF|0.1.0;CONCEPT_MOP|BIONLP|OGER|0.1.0;CRF_MOP|BIONLP|CRF|0.1.0"
#REQUIRED_FLAGS='TEXT_DONE|OGER_CHEBI_DONE|CRF_CHEBI_DONE|OGER_PR_DONE|CRF_PR_DONE|OGER_CL_DONE|CRF_CL_DONE|OGER_UBERON_DONE|CRF_UBERON_DONE|OGER_GO_BP_DONE|CRF_GO_BP_DONE|OGER_GO_CC_DONE|CRF_GO_CC_DONE|OGER_GO_MF_DONE|CRF_GO_MF_DONE|OGER_SO_DONE|CRF_SO_DONE|OGER_NCBITAXON_DONE|CRF_NCBITAXON_DONE'

# USE WITH TEST_SENT_EXT COLLECTION
#INPUT_DOC_CRITERIA='TEXT|TEXT|MEDLINE_XML_TO_TEXT|0.1.0;CONCEPT_CHEBI|BIONLP|OGER|0.1.0;CRF_CHEBI|BIONLP|CRF|0.1.0;CONCEPT_PR|BIONLP|OGER|0.1.0;CRF_PR|BIONLP|CRF|0.1.0'
REQUIRED_FLAGS='TEXT_DONE'

PR_PROMOTION_MAP_FILE_PATH="${BUCKET}/ontology-resources/pr-promotion-map.tsv.gz"
NCBITAXON_PROMOTION_MAP_FILE_PATH="${BUCKET}/ontology-resources/ncbitaxon-promotion-map.tsv.gz"
EXTENSION_MAP_FILE_PATH="${BUCKET}/ontology-resources/craft-mapping-files/*.txt.gz"
ANCESTOR_MAP_FILE_PATH="${BUCKET}/ontology-resources/ontology-class-ancestor-map.tsv.gz"

java -Dfile.encoding=UTF-8 -jar target/tm-pipelines-bundled-0.1.0.jar CONCEPT_POST_PROCESS \
--jobName="$JOB_NAME" \
--inputDocumentCriteria="$INPUT_DOC_CRITERIA" \
--requiredProcessingStatusFlags="$REQUIRED_FLAGS" \
--prPromotionMapFilePath="$PR_PROMOTION_MAP_FILE_PATH" \
--prPromotionMapFileDelimiter='TAB' \
--ncbiTaxonPromotionMapFilePath="$NCBITAXON_PROMOTION_MAP_FILE_PATH" \
--ncbiTaxonPromotionMapFileDelimiter='TAB' \
--ncbiTaxonPromotionMapFileSetDelimiter='PIPE' \
--extensionMapFilePath="$EXTENSION_MAP_FILE_PATH" \
--extensionMapFileDelimiter='TAB' \
--extensionMapFileSetDelimiter='TAB' \
--ancestorMapFilePath="$ANCESTOR_MAP_FILE_PATH" \
--ancestorMapFileDelimiter='TAB' \
--ancestorMapFileSetDelimiter='PIPE' \
--collection="$COLLECTION" \
--overwrite='YES' \
--filterFlag="$FILTER_FLAG" \
--targetProcessingStatusFlag="$TARGET_PROCESSING_STATUS_FLAG" \
--project="${PROJECT}" \
--stagingLocation="$STAGE_LOCATION" \
--gcpTempLocation="$TMP_LOCATION" \
--workerZone=us-central1-c \
--region=us-central1 \
--numWorkers=30 \
--maxNumWorkers=75 \
--autoscalingAlgorithm=THROUGHPUT_BASED \
--workerMachineType=n1-highmem-2 \
--runner=DataflowRunner