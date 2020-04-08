package edu.cuanschutz.ccp.tm_provider.etl.fn;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.sdk.values.TupleTagList;

import edu.cuanschutz.ccp.tm_provider.etl.EtlFailureData;
import edu.cuanschutz.ccp.tm_provider.etl.PipelineMain;
import edu.cuanschutz.ccp.tm_provider.etl.ProcessingStatus;
import edu.cuanschutz.ccp.tm_provider.etl.util.DocumentCriteria;
import edu.cuanschutz.ccp.tm_provider.etl.util.ProcessingStatusFlag;
import edu.ucdenver.ccp.common.string.StringUtil;

/**
 * Utility for loading documents that are already plain text
 * 
 * Outputs four {@link PCollection} objects
 * <ul>
 * <li>mapping document ID to plain text</li>
 * <li>a log of any failures</li>
 * <li>a status object that indicates which jobs still need processing, e.g.
 * dependency parse, etc.</li>
 * </ul>
 *
 */
public class ExtractTextFn extends DoFn<KV<String, String>, KV<String, String>> {

	private static final long serialVersionUID = 1L;

	private final static Logger LOGGER = Logger.getLogger(ExtractTextFn.class.getName());

	@SuppressWarnings("serial")
	public static TupleTag<KV<String, List<String>>> plainTextTag = new TupleTag<KV<String, List<String>>>() {
	};
	@SuppressWarnings("serial")
	public static TupleTag<EtlFailureData> etlFailureTag = new TupleTag<EtlFailureData>() {
	};
	@SuppressWarnings("serial")
	public static TupleTag<ProcessingStatus> processingStatusTag = new TupleTag<ProcessingStatus>() {
	};

	public static PCollectionTuple process(PCollection<KV<String, String>> docIdToText,
			DocumentCriteria outputDocCriteria, com.google.cloud.Timestamp timestamp, String fileSuffix,
			String collection) {

		return docIdToText.apply("Extract plain text; create status entity",
				ParDo.of(new DoFn<KV<String, String>, KV<String, List<String>>>() {
					private static final long serialVersionUID = 1L;

					@ProcessElement
					public void processElement(@Element KV<String, String> docIdToContent, MultiOutputReceiver out) {
						String fileId = docIdToContent.getKey();
						String plainText = docIdToContent.getValue();

						try {
							// the document id is parsed from the file name
							String docId = (fileId.contains("/")) ? fileId.substring(fileId.lastIndexOf("/") + 1)
									: fileId;
							docId = (docId.endsWith(fileSuffix)) ? StringUtil.removeSuffix(docId, fileSuffix) : docId;

							/*
							 * divide the document content into chunks if necessary so that each chunk is
							 * under the DataStore byte length threshold
							 */
							List<String> chunkedPlainText = PipelineMain.chunkContent(plainText);
//							LOGGER.log(Level.SEVERE, String.format("Chunks: %s" , chunkedPlainText.toString()));
							out.get(plainTextTag).output(KV.of(docId, chunkedPlainText));

							/*
							 * output a {@link ProcessingStatus} for the document
							 */
							ProcessingStatus status = new ProcessingStatus(docId);
							status.enableFlag(ProcessingStatusFlag.TEXT_DONE, outputDocCriteria,
									chunkedPlainText.size());
							if (collection != null) {
								status.addCollection(collection);
							}
							out.get(processingStatusTag).output(status);

						} catch (Throwable t) {
							LOGGER.log(Level.SEVERE, "Error while extracting text.", t);
							EtlFailureData failure = new EtlFailureData(outputDocCriteria,
									"Failure saving text file. Likely encoding issue with input document.", fileId, t,
									timestamp);
							out.get(etlFailureTag).output(failure);
						}

					}
				}).withOutputTags(plainTextTag, TupleTagList.of(etlFailureTag).and(processingStatusTag)));
	}

}
