package mdt.sample.workflow;

import org.apache.commons.text.StringSubstitutor;

import mdt.client.HttpMDTManager;
import mdt.client.instance.HttpMDTInstanceManager;
import mdt.model.NameValue;
import mdt.model.instance.MDTInstanceManager;
import mdt.task.RESTfulAsyncRpcTask;
import mdt.task.TaskUtils;
import mdt.workflow.WorkflowManager;
import mdt.workflow.WorkflowModel;
import mdt.workflow.model.ArgumentSpec;
import mdt.workflow.model.TaskDescriptor;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class SampleWorkflowDescriptor3 {
	private static final String WORKFLOW_ID = "sample-workflow-3";
	private static final String HTTP_OP_SERVER_ENDPOINT = "http://${MDT_HOST}:12987";
	
	public static final void main(String... args) throws Exception {
		HttpMDTManager mdt = HttpMDTManager.connectWithDefault();
		HttpMDTInstanceManager manager = mdt.getInstanceManager();
		
		WorkflowModel wfModel;
		
		wfModel = new WorkflowModel();
		wfModel.setId(WORKFLOW_ID);
		wfModel.setName("테스트 시뮬레이션");
		wfModel.setDescription("본 워크플로우는 시뮬레이션 연동을 확인하기 위한 테스트 목적으로 작성됨.");

		TaskDescriptor taskDesc;

		taskDesc = newHttpTask(manager, "sleep-and-add");
		wfModel.getTaskDescriptors().add(taskDesc);

		WorkflowManager wfManager = mdt.getWorkflowManager();
		wfModel = wfManager.addOrReplaceWorkflowModel(wfModel);

		System.out.println(wfModel.toJsonString());
	}
	
	private static TaskDescriptor newHttpTask(MDTInstanceManager manager, String taskId) {
		TaskDescriptor task = new TaskDescriptor(taskId, "", RESTfulAsyncRpcTask.class.getName());
		
		StringSubstitutor subst = new StringSubstitutor(System.getenv());
		String resolvedEndpoint = subst.replace(HTTP_OP_SERVER_ENDPOINT);

		task.addOption(RESTfulAsyncRpcTask.OPTION_BASE_URL, resolvedEndpoint);
		task.addOption(RESTfulAsyncRpcTask.OPTION_OPERATION_ENDPOINT, "/operations/AddAndSleep");
		task.addOption(RESTfulAsyncRpcTask.OPTION_POLL_INTERVAL, "1.0");
		task.addOption(RESTfulAsyncRpcTask.OPTION_TIMEOUT, "60");
		task.addOption(RESTfulAsyncRpcTask.OPTION_LOG_LEVEL, "info");
		task.getLabels().add(NameValue.of(TaskUtils.LABEL_MDT_OPERATION, "test:AddAndSleep"));

		task.addInputArgument(ArgumentSpec.reference("Data", "param:test:Data"));
		task.addInputArgument(ArgumentSpec.literal("IncAmount", 11));
		task.addInputArgument(ArgumentSpec.reference("SleepTime",
								"test:Data:DataInfo.Equipment.EquipmentParameterValues[2].ParameterValue"));
		task.addOutputArgument(ArgumentSpec.reference("Output", "param:test:Data"));
		
		return task;
	}
}
