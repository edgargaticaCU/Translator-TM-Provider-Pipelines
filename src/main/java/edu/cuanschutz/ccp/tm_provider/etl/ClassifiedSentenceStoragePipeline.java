package edu.cuanschutz.ccp.tm_provider.etl;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.jdbc.JdbcIO;
import org.apache.beam.sdk.io.jdbc.JdbcIO.DataSourceConfiguration;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.join.CoGbkResult;
import org.apache.beam.sdk.transforms.join.CoGroupByKey;
import org.apache.beam.sdk.transforms.join.KeyedPCollectionTuple;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.commons.codec.digest.DigestUtils;

import edu.cuanschutz.ccp.tm_provider.etl.fn.ExtractedSentence;
import edu.cuanschutz.ccp.tm_provider.etl.util.BiolinkConstants.BiolinkAssociation;
import edu.cuanschutz.ccp.tm_provider.etl.util.BiolinkConstants.BiolinkPredicate;
import edu.cuanschutz.ccp.tm_provider.etl.util.BiolinkConstants.SPO;
import edu.ucdenver.ccp.common.collections.CollectionsUtil;
import lombok.Data;

/**
 * Combines BERT output (classified sentences) with sentence metadata and stores
 * results in a Cloud SQL instance
 */
public class ClassifiedSentenceStoragePipeline {

	public interface Options extends DataflowPipelineOptions {
		@Description("GCS path to the BERT output file")
		String getBertOutputFilePath();

		void setBertOutputFilePath(String path);

		@Description("GCS path to the sentence metadata file")
		String getSentenceMetadataFilePath();

		void setSentenceMetadataFilePath(String path);

		@Description("The Biolink Association that is being processed.")
		BiolinkAssociation getBiolinkAssociation();

		void setBiolinkAssociation(BiolinkAssociation assoc);

		@Description("The name of the database")
		String getDatabaseName();

		void setDatabaseName(String value);

		@Description("The database username")
		String getDbUsername();

		void setDbUsername(String value);

		@Description("The password for the corresponding database user")
		String getDbPassword();

		void setDbPassword(String value);

		@Description("Cloud SQL MySQL instance name")
		String getMySqlInstanceName();

		void setMySqlInstanceName(String value);

		@Description("GCP region for the Cloud SQL instance (see the connection name in the GCP console)")
		String getCloudSqlRegion();

		void setCloudSqlRegion(String value);

		@Description("The minimum BERT classification score required for a sentence to be kept as evidence for an assertion.")
		double getBertScoreInclusionMinimumThreshold();

		void setBertScoreInclusionMinimumThreshold(double minThreshold);

	}

	public static void main(String[] args) {
		Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);
		Pipeline p = Pipeline.create(options);
		
		
		System.out.println("bert output file: " + options.getBertOutputFilePath());
		System.out.println("sentence metadata file: " + options.getSentenceMetadataFilePath());
		
		final double bertScoreInclusionMinimumThreshold = options.getBertScoreInclusionMinimumThreshold();
		
		final String bertOutputFilePath = options.getBertOutputFilePath();
		PCollection<String> bertOutputLines = p.apply(TextIO.read().from(bertOutputFilePath));
		PCollection<KV<String, String>> idToBertOutputLines = getKV(bertOutputLines, 0);

		final String sentenceMetadataFilePath = options.getSentenceMetadataFilePath();
		PCollection<String> metadataLines = p.apply(TextIO.read().from(sentenceMetadataFilePath));
		PCollection<KV<String, String>> idToMetadataLines = getKV(metadataLines, 0);

		/* group the lines by their ids */
		final TupleTag<String> bertOutputTag = new TupleTag<>();
		final TupleTag<String> metadataTag = new TupleTag<>();
		PCollection<KV<String, CoGbkResult>> result = KeyedPCollectionTuple.of(bertOutputTag, idToBertOutputLines)
				.and(metadataTag, idToMetadataLines).apply(CoGroupByKey.create());

		final String dbUsername = options.getDbUsername();
		final String dbPassword = options.getDbPassword();
		final String databaseName = options.getDatabaseName();
		final String cloudSqlInstanceName = options.getMySqlInstanceName();
		final String projectId = options.getProject();
		final String cloudSqlRegion = options.getCloudSqlRegion();
		final BiolinkAssociation biolinkAssoc = options.getBiolinkAssociation();
		
		
		

		PCollection<SqlValues> sqlValues = result.apply("compile sql values",
				ParDo.of(new DoFn<KV<String, CoGbkResult>, SqlValues>() {
					private static final long serialVersionUID = 1L;

					@ProcessElement
					public void processElement(ProcessContext c) {
						KV<String, CoGbkResult> element = c.element();
						CoGbkResult result = element.getValue();

						/*
						 * the result should have one line from each file except for the header in the
						 * bert output file which won't have a corresponding match in the metadata file.
						 * Below we get one line for each tag, check to make sure there aren't other
						 * lines just in case, and handle the case of the bert output file header.
						 */
						String bertOutputLine = null;
						Iterator<String> bertOutputLineIter = result.getAll(bertOutputTag).iterator();
						if (bertOutputLineIter.hasNext()) {
							bertOutputLine = bertOutputLineIter.next();
							if (bertOutputLineIter.hasNext()) {
								throw new IllegalArgumentException(
										"Did not expect another line to match from the BERT output file: "
												+ bertOutputLineIter.next());
							}
						}

						String metadataLine = null;
						Iterator<String> metadataLineIter = result.getAll(metadataTag).iterator();
						if (metadataLineIter.hasNext()) {
							metadataLine = metadataLineIter.next();
							if (metadataLineIter.hasNext()) {
								throw new IllegalArgumentException(
										"Did not expect another line to match from the BERT output file: "
												+ metadataLineIter.next());
							}
						}

						/* both lines must have data in order to continue */
						if (metadataLine != null && bertOutputLine != null) {

							int index = 0;
							String[] bertOutputCols = bertOutputLine.split("\\t");
							@SuppressWarnings("unused")
							String sentenceId1 = bertOutputCols[index++];
							@SuppressWarnings("unused")
							String sentenceWithPlaceholders1 = bertOutputCols[index++];

							// one of the scores for the predicates that is not
							// BiolinkPredicate.NO_RELATION_PRESENT must be greater than the BERT minimum
							// inclusion score threshold in order for this sentence to be considered
							// evidence of an assertion. This is checked while populating the
							// predicateCurietoScoreMap.
							boolean hasScoreThatMeetsMinimumInclusionThreshold = false;
							Map<String, Double> predicateCurieToScore = new HashMap<String, Double>();
							for (String predicateCurie : getPredicateCuries(biolinkAssoc)) {
								double score = Double.parseDouble(bertOutputCols[index++]);
								predicateCurieToScore.put(predicateCurie, score);
								if (!predicateCurie.equals("false")
										&& score > bertScoreInclusionMinimumThreshold) {
									hasScoreThatMeetsMinimumInclusionThreshold = true;
								}
							}

							if (hasScoreThatMeetsMinimumInclusionThreshold) {
								ExtractedSentence es = ExtractedSentence.fromTsv(metadataLine, true);

								String subjectCoveredText;
								String subjectCurie;
								String subjectSpanStr;
								String objectCoveredText;
								String objectCurie;
								String objectSpanStr;
								if (es.getEntityPlaceholder1().equals(biolinkAssoc.getSubjectPlaceholder())) {
									subjectCurie = es.getEntityId1();
									subjectSpanStr = ExtractedSentence.getSpanStr(es.getEntitySpan1());
									subjectCoveredText = es.getEntityCoveredText1();
									objectCurie = es.getEntityId2();
									objectSpanStr = ExtractedSentence.getSpanStr(es.getEntitySpan2());
									objectCoveredText = es.getEntityCoveredText2();
								} else {
									subjectCurie = es.getEntityId2();
									subjectSpanStr = ExtractedSentence.getSpanStr(es.getEntitySpan2());
									subjectCoveredText = es.getEntityCoveredText2();
									objectCurie = es.getEntityId1();
									objectSpanStr = ExtractedSentence.getSpanStr(es.getEntitySpan1());
									objectCoveredText = es.getEntityCoveredText1();
								}

								String documentId = es.getDocumentId();
								String sentence = es.getSentenceText();

								String assertionId = DigestUtils
										.sha256Hex(subjectCurie + objectCurie + biolinkAssoc.getAssociationId());
								String evidenceId = DigestUtils.sha256Hex(documentId + sentence + subjectCurie
										+ subjectSpanStr + objectCurie + objectSpanStr);
								String subjectEntityId = DigestUtils
										.sha256Hex(documentId + sentence + subjectCurie + subjectSpanStr);
								String objectEntityId = DigestUtils
										.sha256Hex(documentId + sentence + objectCurie + objectSpanStr);

								int documentYearPublished = es.getDocumentYearPublished();
								if (documentYearPublished > 2155) {
									// 2155 is the max year value in MySQL
									documentYearPublished = 2155;
								}
								SqlValues sqlValues = new SqlValues(assertionId, subjectCurie, objectCurie,
										biolinkAssoc.getAssociationId(), evidenceId, documentId, sentence,
										subjectEntityId, objectEntityId, es.getDocumentZone(),
										CollectionsUtil.createDelimitedString(es.getDocumentPublicationTypes(), "|"),
										documentYearPublished, subjectSpanStr, objectSpanStr,
										subjectCoveredText, objectCoveredText);

								for (Entry<String, Double> entry : predicateCurieToScore.entrySet()) {
									sqlValues.addScore(entry.getKey(), entry.getValue());
								}

								c.output(sqlValues);
							}

						}
					}

				}));

		// https://stackoverflow.com/questions/44699643/connecting-to-cloud-sql-from-dataflow-job
		// MYSQLINSTANCE format is project:zone:instancename.

		String instanceName = String.format("%s:%s:%s", projectId, cloudSqlRegion, cloudSqlInstanceName);
		String jdbcUrl = String.format(
				"jdbc:mysql://google/%s?cloudSqlInstance=%s&socketFactory=com.google.cloud.sql.mysql.SocketFactory&user=%s&password=%s&useUnicode=true&characterEncoding=UTF-8",
				databaseName, instanceName, dbUsername, dbPassword);

		DataSourceConfiguration dbConfig = JdbcIO.DataSourceConfiguration.create("com.mysql.cj.jdbc.Driver", jdbcUrl);

		/* Insert into assertions table */
		sqlValues.apply("insert assertions", JdbcIO.<SqlValues>write().withDataSourceConfiguration(dbConfig)
				.withStatement("INSERT INTO assertion (assertion_id,subject_curie,object_curie,association_curie) \n"
						+ "values(?,?,?,?) ON DUPLICATE KEY UPDATE\n" + "    assertion_id = VALUES(assertion_id),\n"
						+ "    subject_curie = VALUES(subject_curie),\n" + "    object_curie = VALUES(object_curie),\n"
						+ "    association_curie = VALUES(association_curie)")
				.withPreparedStatementSetter(new JdbcIO.PreparedStatementSetter<SqlValues>() {
					private static final long serialVersionUID = 1L;

					public void setParameters(SqlValues sqlValues, PreparedStatement query) throws SQLException {
						query.setString(1, sqlValues.getAssertionId()); // assertion_id
						query.setString(2, sqlValues.getSubjectCurie()); // subject_curie
						query.setString(3, sqlValues.getObjectCurie()); // object_curie
						query.setString(4, sqlValues.getAssociationCurie()); // association_curie
					}
				}));

		/* Insert into evidence table */
		sqlValues.apply("insert evidence",
				JdbcIO.<SqlValues>write().withDataSourceConfiguration(dbConfig).withStatement(
						"INSERT INTO evidence (evidence_id, assertion_id, document_id, sentence, subject_entity_id, object_entity_id, document_zone, document_publication_type, document_year_published) \n"
								+ "values(?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE\n"
								+ "  evidence_id = VALUES(evidence_id),\n" + "  assertion_id = VALUES(assertion_id),\n"
								+ "  document_id = VALUES(document_id),\n" + "  sentence = VALUES(sentence),\n"
								+ "  subject_entity_id = VALUES(subject_entity_id),\n"
								+ "  object_entity_id = VALUES(object_entity_id),\n"
								+ "  document_zone = VALUES(document_zone),\n"
								+ "  document_publication_type = VALUES(document_publication_type),\n"
								+ "  document_year_published = VALUES(document_year_published)")
						.withPreparedStatementSetter(new JdbcIO.PreparedStatementSetter<SqlValues>() {
							private static final long serialVersionUID = 1L;

							public void setParameters(SqlValues sqlValues, PreparedStatement query)
									throws SQLException {
								query.setString(1, sqlValues.getEvidenceId()); // evidence_id
								query.setString(2, sqlValues.getAssertionId());
								query.setString(3, sqlValues.getDocumentId()); // document_id
								query.setString(4, sqlValues.getSentence()); // sentence
								query.setString(5, sqlValues.getSubjectEntityId()); // subject_entity_id
								query.setString(6, sqlValues.getObjectEntityId()); // object_entity_id
								query.setString(7, sqlValues.getDocumentZone()); // document_zone
								query.setString(8, sqlValues.getDocumentPublicationTypesStr()); // document_publication_type
								query.setInt(9, sqlValues.getDocumentYearPublished()); // document_year_published
							}
						}));

		/* Insert subject into entity table */
		sqlValues.apply("insert subject entity",
				JdbcIO.<SqlValues>write().withDataSourceConfiguration(dbConfig)
						.withStatement("INSERT INTO entity (entity_id, span, covered_text) \n"
								+ "values(?, ?, ?) ON DUPLICATE KEY UPDATE\n" + "  entity_id = VALUES(entity_id),\n"
								+ "  span = VALUES(span),\n" + "  covered_text = VALUES(covered_text)")
						.withPreparedStatementSetter(new JdbcIO.PreparedStatementSetter<SqlValues>() {
							private static final long serialVersionUID = 1L;

							public void setParameters(SqlValues sqlValues, PreparedStatement query)
									throws SQLException {
								query.setString(1, sqlValues.getSubjectEntityId()); // subject_entity_id
								query.setString(2, sqlValues.getSubjectSpanStr()); // subject_span
								query.setString(3, sqlValues.getSubjectCoveredText()); // subject_covered_text
							}
						}));

		/* Insert object into entity table */
		sqlValues.apply("insert object entity",
				JdbcIO.<SqlValues>write().withDataSourceConfiguration(dbConfig)
						.withStatement("INSERT INTO entity (entity_id, span, covered_text) \n"
								+ "values(?, ?, ?) ON DUPLICATE KEY UPDATE\n" + "  entity_id = VALUES(entity_id),\n"
								+ "  span = VALUES(span),\n" + "  covered_text = VALUES(covered_text)")
						.withPreparedStatementSetter(new JdbcIO.PreparedStatementSetter<SqlValues>() {
							private static final long serialVersionUID = 1L;

							public void setParameters(SqlValues sqlValues, PreparedStatement query)
									throws SQLException {
								query.setString(1, sqlValues.getObjectEntityId()); // object_entity_id
								query.setString(2, sqlValues.getObjectSpanStr()); // object_span
								query.setString(3, sqlValues.getObjectCoveredText()); // object_covered_text
							}
						}));

		for (final String predicateCurie : getPredicateCuries(biolinkAssoc)) {
			/* Insert into evidence score table */
			sqlValues.apply("insert predicate=" + predicateCurie, JdbcIO.<SqlValues>write()
					.withDataSourceConfiguration(dbConfig)
					.withStatement("INSERT INTO evidence_score (evidence_id, predicate_curie, score) \n"
							+ "values(?, ?, ?) ON DUPLICATE KEY UPDATE\n" + "  evidence_id = VALUES(evidence_id),\n"
							+ "  predicate_curie = VALUES(predicate_curie),\n" + "  score = VALUES(score)")
					.withPreparedStatementSetter(new JdbcIO.PreparedStatementSetter<SqlValues>() {
						private static final long serialVersionUID = 1L;

						public void setParameters(SqlValues sqlValues, PreparedStatement query) throws SQLException {

							query.setString(1, sqlValues.getEvidenceId()); // evidence_id
							query.setString(2, predicateCurie); // predicate_curie
							query.setDouble(3, sqlValues.getScore(predicateCurie)); // score
						}
					}));
		}

		p.run().waitUntilFinish();
	}

	private static List<String> getPredicateCuries(BiolinkAssociation biolinkAssociation) {
		List<String> curies = new ArrayList<String>();
		for (SPO spo : biolinkAssociation.getSpoTriples()) {
			BiolinkPredicate predicate = spo.getPredicate();
			if (predicate == BiolinkPredicate.NO_RELATION_PRESENT) {
				curies.add("false");
			} else {
				curies.add(predicate.getCurie());
			}
		}

		return curies;
	}

	/**
	 * @param lines
	 * @param keyColumn
	 * @return Given a PCollection of lines, return a PCollection<KV<String,
	 *         String>> where the key is the value of a specified column in the line
	 *         and the value is the line itself.
	 */
	private static PCollection<KV<String, String>> getKV(PCollection<String> lines, int keyColumn) {
		return lines.apply(ParDo.of(new DoFn<String, KV<String, String>>() {
			private static final long serialVersionUID = 1L;

			@ProcessElement
			public void processElement(ProcessContext c) {
				String line = c.element();
				String id = line.split("\\t")[keyColumn];
				c.output(KV.of(id, line));
			}
		}));
	}

	@Data
	private static class SqlValues implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		/* assertion table */
		private final String assertionId;
		private final String subjectCurie;
		private final String objectCurie;
		private final String associationCurie;

		/* evidence table */
		private final String evidenceId;
		private final String documentId;
		private final String sentence;
		private final String subjectEntityId;
		private final String objectEntityId;
		private final String documentZone;
		private final String documentPublicationTypesStr;
		private final int documentYearPublished;

		/* entity table */
		private final String subjectSpanStr;
		private final String objectSpanStr;
		private final String subjectCoveredText;
		private final String objectCoveredText;

		private final Map<String, Double> predicateCurieToScoreMap = new HashMap<String, Double>();

		public void addScore(String predicateCurie, double score) {
			predicateCurieToScoreMap.put(predicateCurie, score);
		}

		public double getScore(String predicateCurie) {
			return predicateCurieToScoreMap.get(predicateCurie);
		}
	}

}
