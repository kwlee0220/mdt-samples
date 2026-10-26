package mdt.sample;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationResult;
import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;

import utils.Instants;
import utils.func.Funcs;
import utils.stream.FStream;

import mdt.aas.DataTypes;
import mdt.client.HttpMDTManager;
import mdt.client.operation.AASOperationClient;
import mdt.model.SubmodelService;
import mdt.model.instance.MDTInstance;
import mdt.model.instance.MDTInstanceManager;
import mdt.model.sm.PropertyUtils;
import mdt.model.sm.ref.DefaultSubmodelReference;
import mdt.model.sm.ref.timeseries.TimeSeriesRecordsReference;
import mdt.model.sm.value.ElementValues;


/**
 * 시계열 Submodel의 레코드를 지정된 시각부터 고정 크기 배치로 반복 조회하는 예제.
 * <p>
 * 시작 시각 {@code T}부터 {@code N}개씩 레코드를 읽어 처리하는 것을 반복하며, 읽어온 레코드가
 * {@code N}개보다 적으면 <b>해당 배치를 처리하지 않고 버린 뒤</b> 프로그램을 종료한다. 즉 완전한
 * 배치만 처리하고, 아직 {@code N}개가 쌓이지 않은 구간은 다음 실행으로 미룬다.
 * <p>
 * 조회에 사용한 시작 시각은 {@link #CURSOR_FILE}에 저장되므로, 프로그램을 다시 실행하면 이전에
 * 중단된 지점부터 이어서 읽는다.
 *
 * <pre>
 * 사용법: SampleTimeSeriesBatchReader [&lt;start-timestamp&gt;] &lt;batch-size&gt;
 *
 *   SampleTimeSeriesBatchReader 2023-05-25T14:00:00Z 5   시작 시각을 지정하여 조회
 *   SampleTimeSeriesBatchReader 5                        저장된 시작 시각부터 이어서 조회
 * </pre>
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class SampleTimeSeriesBatchReader {
	/** 조회 대상 MDTInstance id. */
	private static final String INSTANCE_ID = "Welder";
	/** 조회 대상 시계열 Submodel의 idShort. */
	private static final String SUBMODEL_IDSHORT = "WelderAmpereLog";
	/** 다음 조회에 사용할 시작 시각을 보관하는 파일. */
	private static final Path CURSOR_FILE = Path.of("timeseries-cursor.txt");
	/** 시계열 레코드의 시각 필드 이름. */
	private static final String TIME_FIELD = "Time";

	public static void main(String[] args) throws Exception {
		Instant startTs;
		int batchSize;
		if ( args.length == 2 ) {
			// 시작 시각이 주어진 경우는 해당 시각부터 조회한다.
			startTs = Instants.fromString(args[0]);
			batchSize = Integer.parseInt(args[1]);
		}
		else if ( args.length == 1 ) {
			// 시작 시각이 없는 경우는 마지막으로 사용된 시작 시각부터 이어서 조회한다.
			batchSize = Integer.parseInt(args[0]);
			startTs = loadCursor();
			if ( startTs == null ) {
				System.err.printf("저장된 시작 시각이 없습니다. 최초 실행에서는 시작 시각을 지정하십시오: file=%s%n",
									CURSOR_FILE.toAbsolutePath());
				System.exit(1);
				return;
			}
		}
		else {
			System.err.printf("사용법: %s [<start-timestamp>] <batch-size>%n",
								SampleTimeSeriesBatchReader.class.getSimpleName());
			System.exit(1);
			return;
		}
		if ( batchSize <= 0 ) {
			System.err.println("batch-size는 1 이상이어야 합니다: " + batchSize);
			System.exit(1);
			return;
		}

		// 기본적인 설정 정보를 이용하여 MDT Manager에 접속한다.
		HttpMDTManager mdt = HttpMDTManager.connectWithDefault();
		MDTInstanceManager manager = mdt.getInstanceManager();

		DefaultSubmodelReference smRef = DefaultSubmodelReference.ofIdShort(INSTANCE_ID, SUBMODEL_IDSHORT);
		smRef.activate(manager);
		
		MDTInstance welder = manager.getInstance(INSTANCE_ID);
		SubmodelService prodEst = welder.getSubmodelServiceByIdShort("ProductionEstimation").get();

		Instant cursor = startTs;
		int batchCount = 0;
		float count = 0f;
		while ( true ) {
			// 조회에 사용할 시작 시각을 먼저 저장한다.
			// (배치가 중단되더라도 다음 실행이 같은 지점에서 재개할 수 있도록 한다)
			saveCursor(cursor);

			List<SubmodelElement> records = readRecords(manager, smRef, cursor, batchSize);
			if ( records.size() < batchSize ) {
				// 배치를 채울 만큼 레코드가 쌓이지 않았으므로 이 레코드 집합은 버리고 종료한다.
				System.out.printf("불완전한 배치를 취소합니다: records=%d < %d, start=%s%n",
									records.size(), batchSize, cursor);
				break;
			}

			String samples = getSamples(++batchCount, cursor, records);
//			System.out.printf("  %s%n", samples);
			
			AASOperationClient opClient = new AASOperationClient(prodEst, "Operation", Duration.ofSeconds(1));
			opClient.setInputVariable("Samples", PropertyUtils.STRING("Samples", samples));
			
			OperationResult result = opClient.run();
			List<OperationVariable> outputs = result.getOutputArguments();
			Double score = (Double)ElementValues.getValue(outputs.get(0).getValue()).toValueObject();
			String cluster = (String)ElementValues.getValue(outputs.get(1).getValue()).toValueObject();
			
			count += Float.parseFloat(cluster);
			
			System.out.printf("AnomalyScore=%f, Cluster=%s, Count=%.1f%n", score, cluster, count);

			// 마지막으로 읽은 레코드 다음 시각으로 커서를 옮긴다.
			cursor = nextCursor(Funcs.getLast(records));
		}

		System.out.printf("종료: 처리한 배치=%d개, 다음 시작 시각=%s (%s)%n",
							batchCount, cursor, CURSOR_FILE.toAbsolutePath());
	}

	/**
	 * {@code from} 시각(포함)부터 최대 {@code count}개의 레코드를 조회한다.
	 *
	 * @param manager	MDTInstance 관리자.
	 * @param smRef		대상 시계열 Submodel에 대한 참조.
	 * @param from		조회 시작 시각(포함).
	 * @param count		읽어올 최대 레코드 개수.
	 * @return	조회된 레코드 목록. 시각 오름차순으로 정렬되어 있다.
	 * @throws IOException	레코드 조회에 실패한 경우.
	 */
	private static List<SubmodelElement> readRecords(MDTInstanceManager manager, DefaultSubmodelReference smRef,
													Instant from, int count) throws IOException {
		TimeSeriesRecordsReference tsRef = TimeSeriesRecordsReference.builder()
																	.submodelReference(smRef)
																	.since(from, count)
																	.build();
		tsRef.activate(manager);

		return tsRef.read().getValue();
	}

	/**
	 * 주어진 레코드 다음 배치의 시작 시각을 계산한다.
	 * <p>
	 * 조회 범위가 시작 시각을 포함하므로 마지막 레코드의 시각을 그대로 사용하면 같은 레코드를 다시
	 * 읽게 된다. 조회 질의의 시각 리터럴이 밀리초 단위로 표현되기 때문에 1밀리초를 더해 다음 시각을
	 * 만든다. (따라서 마지막 레코드와 <b>같은 밀리초</b>에 기록된 레코드는 건너뛴다)
	 *
	 * @param lastRecord	배치의 마지막 레코드.
	 * @return	다음 배치의 시작 시각.
	 */
	private static Instant nextCursor(SubmodelElement lastRecord) {
		return getTimestamp(lastRecord).plusMillis(1);
	}

	/**
	 * 레코드에서 시각 필드({@link #TIME_FIELD})의 값을 읽어 반환한다.
	 *
	 * @param recSme	대상 레코드.
	 * @return	레코드의 시각.
	 */
	private static Instant getTimestamp(SubmodelElement recSme) {
		SubmodelElementCollection recSmc = (SubmodelElementCollection)recSme;

		SubmodelElement tsSme = Funcs.findFirst(recSmc.getValue(), sme -> TIME_FIELD.equals(sme.getIdShort()));
		if ( tsSme == null ) {
			// 시각 필드가 projection에서 제외된 경우는 첫번째 필드를 시각으로 간주한다.
			tsSme = recSmc.getValue().get(0);
		}

		return DataTypes.DATE_TIME.parseValueString(((Property)tsSme).getValue());
	}

	private static String getSamples(int batchNo, Instant from, List<SubmodelElement> records) {
//		System.out.printf("[배치 %d] start=%s, records=%d%n", batchNo, from, records.size());
		String valueStr = FStream.from(records)
								.castSafely(SubmodelElementCollection.class)
								.map(SampleTimeSeriesBatchReader::toAmpereValues)
								.map(v -> ""+v)
								.join(",", "[[", "]]");
		return valueStr;
	}

	private static float toAmpereValues(SubmodelElementCollection recSmc) {
		SubmodelElement ampereSme = recSmc.getValue().get(1);
		return Float.parseFloat(((Property)ampereSme).getValue());
	}

	/**
	 * 마지막으로 사용된 시작 시각을 파일에서 읽어 반환한다.
	 *
	 * @return	저장된 시작 시각. 저장된 값이 없으면 {@code null}.
	 * @throws IOException	파일 읽기에 실패한 경우.
	 */
	private static Instant loadCursor() throws IOException {
		if ( !Files.exists(CURSOR_FILE) ) {
			return null;
		}

		String cursorStr = Files.readString(CURSOR_FILE, StandardCharsets.UTF_8).trim();
		return (cursorStr.length() > 0) ? Instants.fromString(cursorStr) : null;
	}

	/**
	 * 조회에 사용할 시작 시각을 파일에 저장한다.
	 *
	 * @param cursor	저장할 시작 시각.
	 * @throws IOException	파일 쓰기에 실패한 경우.
	 */
	private static void saveCursor(Instant cursor) throws IOException {
		Files.writeString(CURSOR_FILE, cursor.toString(), StandardCharsets.UTF_8);
	}
}
