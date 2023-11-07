#!/usr/local/bin/bash

source ./rrun.env.sh

SCRIPT=./scripts/pipelines/collections/run_collection_assignment.sh

INPUT_DOCUMENT_CRITERIA=ABBREVIATIONS|BIONLP|ABBREVIATION|0.3.0
INPUT_COLLECTION=PUBMED
TARGET_PROCESSING_STATUS_FLAG=ABBREVIATIONS_DONE

OUTPUT_COLLECTION=REDO_ABBREV_20230915

$SCRIPT $INPUT_DOCUMENT_CRITERIA $INPUT_COLLECTION $OUTPUT_COLLECTION $TARGET_PROCESSING_STATUS_FLAG $PROJECT_ID ${STAGE_LOCATION} ${TEMP_LOCATION} $JAR_VERSION &> "./logs/collection-assignment-${INPUT_PIPELINE_KEY}-${OUTPUT_COLLECTION}.log" &
