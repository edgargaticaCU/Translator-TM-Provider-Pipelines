package edu.cuanschutz.ccp.tm_provider.etl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.datastore.DatastoreIO;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Keys;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionTuple;

import edu.cuanschutz.ccp.tm_provider.etl.fn.DocumentToEntityFn;
import edu.cuanschutz.ccp.tm_provider.etl.fn.EtlFailureToEntityFn;
import edu.cuanschutz.ccp.tm_provider.etl.fn.TurkuDepParserFn;
import edu.cuanschutz.ccp.tm_provider.etl.util.DatastoreProcessingStatusUtil;
import edu.cuanschutz.ccp.tm_provider.etl.util.DocumentFormat;
import edu.cuanschutz.ccp.tm_provider.etl.util.DocumentType;
import edu.cuanschutz.ccp.tm_provider.etl.util.PipelineKey;
import edu.cuanschutz.ccp.tm_provider.etl.util.ProcessingStatusFlag;
import edu.cuanschutz.ccp.tm_provider.etl.util.Version;

/**
 * This Apache Beam pipeline processes documents with a dependency parser
 * reached via HTTP POST. Input is plain text; Output is CoNLL-U format.
 */
public class DependencyParsePipeline {

	private final static Logger LOGGER = Logger.getLogger(DependencyParsePipeline.class.getName());

	private static final PipelineKey PIPELINE_KEY = PipelineKey.DEPENDENCY_PARSE;

	public interface Options extends DataflowPipelineOptions {
		@Description("URI for the dependency parser service")
		String getDependencyParserServiceUri();

		void setDependencyParserServiceUri(String value);
	}

	public static void main(String[] args) {
		String pipelineVersion = Version.getProjectVersion();
		com.google.cloud.Timestamp timestamp = com.google.cloud.Timestamp.now();

		Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);

		Pipeline p = Pipeline.create(options);

		// we want to find documents that need dependency parsing
		ProcessingStatusFlag targetProcessStatusFlag = ProcessingStatusFlag.DP_DONE;
		// we require that the documents have a plain text version
		Set<ProcessingStatusFlag> requiredProcessStatusFlags = EnumSet.of(ProcessingStatusFlag.TEXT_DONE);

		PCollection<KV<String, String>> docId2Content = PipelineMain.getDocId2Content(pipelineVersion, options.getProject(), p,
				targetProcessStatusFlag, requiredProcessStatusFlags);

		PCollectionTuple output = TurkuDepParserFn.process(docId2Content, options.getDependencyParserServiceUri(),
				PIPELINE_KEY, pipelineVersion, DocumentType.DEPENDENCY_PARSE, timestamp);

		/*
		 * Processing of the plain text by the dependency parser results in 1) a
		 * PCollection mapping document ID to the CoNLL-U version of the document. 2) a
		 * PCollection logging any errors encountered during the dependency parse
		 * processing.
		 */

		PCollection<KV<String, String>> docIdToConllu = output.get(TurkuDepParserFn.CONLLU_TAG);
		PCollection<EtlFailureData> failures = output.get(TurkuDepParserFn.ETL_FAILURE_TAG);

		/* store the serialized CoNLL-U document content in Cloud Datastore */
		docIdToConllu
				.apply("conllu->document_entity",
						ParDo.of(new DocumentToEntityFn(DocumentType.DEPENDENCY_PARSE, DocumentFormat.CONLLU,
								PIPELINE_KEY, pipelineVersion)))
				.apply("document_entity->datastore", DatastoreIO.v1().write().withProjectId(options.getProject()));

		/* store the failures for this pipeline in Cloud Datastore */
		failures.apply("failures->datastore", ParDo.of(new EtlFailureToEntityFn())).apply("failure_entity->datastore",
				DatastoreIO.v1().write().withProjectId(options.getProject()));

		// update the status for documents that were successfully processed
		PCollection<KV<String, String>> successStatus = DatastoreProcessingStatusUtil
				.getSuccessStatus(docId2Content.apply(Keys.<String>create()), failures, ProcessingStatusFlag.DP_DONE);
		List<PCollection<KV<String, String>>> statusList = new ArrayList<PCollection<KV<String, String>>>();
		statusList.add(successStatus);
		DatastoreProcessingStatusUtil.performStatusUpdatesInBatch(statusList);

		p.run().waitUntilFinish();
	}



	

	
	
}
