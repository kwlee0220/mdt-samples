package mdt.sample;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import com.fasterxml.jackson.databind.JsonNode;

import okhttp3.OkHttpClient;

import utils.async.AsyncResult;
import utils.http.OkHttpClientUtils;
import utils.rpc.restful.RESTfulAsyncRpcClient;
import utils.rpc.restful.RpcRequestMessage;

import mdt.client.HttpMDTManager;
import mdt.client.instance.HttpMDTInstanceManager;
import mdt.model.MDTModelSerDe;
import mdt.model.instance.MDTInstance;
import mdt.model.sm.ref.MDTParameterReference;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class SampleRunHttpOperation {
	private static final String ENDPOINT = "http://localhost:12985";
	private static final String OPERATION_SERVER_ENDPOINT = "http://localhost:12987";
	/** 연산 서버의 operations 디렉터리 하위 연산 디렉터리 이름. */
	private static final String OP_ID = "Simulation";
	
	@SuppressWarnings("unused")
	public static final void main(String... args) throws Exception {
		HttpMDTInstanceManager manager = HttpMDTManager.connect(ENDPOINT)
																	.getInstanceManager();
		
		MDTInstance instance = manager.getInstance("test");
		MDTParameterReference data = MDTParameterReference.newInstance("test", "Data");
		data.activate(manager);
		SubmodelElement dataSme = data.read();
		
		MDTParameterReference incAmount = MDTParameterReference.newInstance("test", "IncAmount");
		incAmount.activate(manager);
		SubmodelElement incAmountSme = incAmount.read();
		
		MDTParameterReference sleepTime = MDTParameterReference.newInstance("test", "SleepTime");
		sleepTime.activate(manager);
		SubmodelElement sleepTimeSme = sleepTime.read();
//		Property sleepTimeSme = PropertyUtils.INT("SleepTime", 10);
		
		OkHttpClient http = OkHttpClientUtils.newTrustAllOkHttpClientBuilder().build();

		Map<String,JsonNode> inputs = Map.of("Data", MDTModelSerDe.toJsonNode(dataSme),
											"IncAmount", MDTModelSerDe.toJsonNode(incAmountSme),
											"SleepTime", MDTModelSerDe.toJsonNode(sleepTimeSme));
		RpcRequestMessage req = new RpcRequestMessage(inputs);
		
		RESTfulAsyncRpcClient opClient = RESTfulAsyncRpcClient.builder()
														.setHttpClient(http)
														.setBaseUrl(OPERATION_SERVER_ENDPOINT)
														.setOperationEndpoint("/api/v1/operations/" + OP_ID)
														.setRequestMessage(req)
														.setJsonMapper(MDTModelSerDe.getJsonMapper())
														.setPollInterval(Duration.ofSeconds(1))
														.setTimeout(Duration.ofSeconds(10))
														.build();
		opClient.start();
		AsyncResult<Map<String,JsonNode>> result = opClient.waitForFinished(5, TimeUnit.SECONDS);
		if ( result.isRunning() ) {
			System.out.println("Cancelling...");
			opClient.cancel(true);
			result = opClient.waitForFinished();
		}
		System.out.println(result);
	}
}
