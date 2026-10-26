package mdt.sample.workflow;

import java.io.File;

import utils.io.IOUtils;

import mdt.client.HttpMDTManager;
import mdt.model.MDTModelSerDe;
import mdt.task.RESTfulAsyncRpcTask;
import mdt.workflow.WorkflowManager;
import mdt.workflow.WorkflowModel;
import mdt.workflow.model.ArgumentSpec;
import mdt.workflow.model.TaskDescriptor;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class SampleTimeSeriesTask {
	public static final void main(String... args) throws Exception {
		HttpMDTManager mdt = HttpMDTManager.connectWithDefault();
		
		WorkflowModel wfDesc;
		TaskDescriptor taskDesc;
		
		wfDesc = new WorkflowModel();
		wfDesc.setId("test_timeseries_arg");
		wfDesc.setName("테스트 시뮬레이션");
		wfDesc.setDescription("본 워크플로우는 시계열 데이터 처리를 확인하기 위한 워크플로우.");
		
		taskDesc = createRESTfulAsyncRpcTask("count_records", "http://192.168.0.7:12987/api/v1",
												"/operations/CountRecords");
		taskDesc.addInputArgument(ArgumentSpec.reference("TimeSeriesData", "timeseries:Welder:NozzleProductionLog#last=50s|Time,QuantityProduced"));
		taskDesc.addOutputArgument(ArgumentSpec.reference("Count", "param:test:Data"));
		wfDesc.getTaskDescriptors().add(taskDesc);
		
		String wfJsonStr = MDTModelSerDe.toJsonString(wfDesc);
		IOUtils.toFile(wfJsonStr, new File("/home/kwlee/tmp/wf_test.json"));

		WorkflowManager wfManager = mdt.getWorkflowManager();
		wfDesc = wfManager.addOrReplaceWorkflowModel(wfDesc);
		
		System.out.println("Workflow id: " + wfDesc.getId());
	}
	
	private static TaskDescriptor createRESTfulAsyncRpcTask(String tasId, String baseUrl, String endpoint) {
		TaskDescriptor task = new TaskDescriptor();
		
		task.setId(tasId);
		task.setType(RESTfulAsyncRpcTask.class.getName());

		task.addOption("id", tasId);
		task.addOption("baseUrl", baseUrl);
		task.addOption("endpoint", endpoint);
		task.addOption("timeout", "1m");
		task.addOption("loglevel", "info");
		
		return task;
	}
}
