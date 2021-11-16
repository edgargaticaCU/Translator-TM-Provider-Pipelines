#!/bin/sh

SERVICE_URL=$1
ONT=$2
PROJECT=$3
COLLECTION=$4
STAGE_LOCATION=$5
TMP_LOCATION=$6
PIPELINE_KEY=$7


TPSF="OGER_${ONT}_DONE"
TDT="CONCEPT_${ONT}"
JOB_NAME=$(echo "OGER-${ONT}-${COLLECTION}-${PIPELINE_KEY}" | tr '_' '-')


echo "SERVICE URL: $SERVICE_URL"
echo "ONT: $ONT"
echo "COLLECTION: $COLLECTION"
echo "PROJECT: $PROJECT"
echo "TPSF: $TPSF"
echo "TDT: $TDT"
echo "JOB_NAME: $JOB_NAME"
echo "PIPELINE KEY: $PIPELINE_KEY"

java -Dfile.encoding=UTF-8 -jar target/tm-pipelines-bundled-0.1.0.jar OGER \
--jobName="$JOB_NAME" \
--ogerServiceUri="$SERVICE_URL" \
--ogerOutputType=TSV \
--targetProcessingStatusFlag="$TPSF" \
--targetDocumentType="$TDT" \
--targetDocumentFormat="BIONLP" \
--inputPipelineKey="$PIPELINE_KEY" \
--inputPipelineVersion='0.1.0' \
--collection="$COLLECTION" \
--overwrite="NO" \
--project="${PROJECT}" \
--stagingLocation="$STAGE_LOCATION" \
--gcpTempLocation="$TMP_LOCATION" \
--workerZone=us-central1-c \
--region=us-central1 \
--numWorkers=10 \
--maxNumWorkers=50 \
--autoscalingAlgorithm=THROUGHPUT_BASED \
--defaultWorkerLogLevel=INFO \
--runner=DataflowRunner