#!/usr/local/bin/bash

source ./rrun.env.sh

SCRIPT=./scripts/pipelines/collections/run_collection_assignment.sh

INPUT_DOCUMENT_CRITERIA="CONCEPT_CS|BIONLP|OGER|0.3.0;CONCEPT_CIMIN|BIONLP|OGER|0.3.0;CONCEPT_CIMAX|BIONLP|OGER|0.3.0"
INPUT_COLLECTION=PUBMED
TARGET_PROCESSING_STATUS_FLAG=OGER_DONE

OUTPUT_COLLECTION=REDO_OGER_20230914

$SCRIPT $INPUT_DOCUMENT_CRITERIA $INPUT_COLLECTION $OUTPUT_COLLECTION $TARGET_PROCESSING_STATUS_FLAG $PROJECT_ID ${STAGE_LOCATION} ${TEMP_LOCATION} $JAR_VERSION &> "./logs/collection-assignment-${OUTPUT_COLLECTION}.log" &