package mdt.sample.workflow;

import mdt.client.HttpMDTManager;
import mdt.client.instance.HttpMDTInstanceManager;
import mdt.model.sm.ref.DefaultElementReference;
import mdt.model.sm.ref.DefaultSubmodelReference;
import mdt.model.sm.ref.DefaultSubmodelReference.ByIdShortSubmodelReference;
import mdt.task.AASOperationTask;
import mdt.task.MDTTask;
import mdt.task.RESTfulAsyncRpcTask;
import mdt.task.SetTask;
import mdt.task.TaskUtils;
import mdt.workflow.WorkflowManager;
import mdt.workflow.WorkflowModel;
import mdt.workflow.model.ArgumentSpec;
import mdt.workflow.model.TaskDescriptor;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class SampleWorkflowDescriptor4 {
	private static final String WORKFLOW_ID = "sample-workflow-4";
	private static final String HTTP_OP_SERVER_ENDPOINT = "http://218.158.72.211:12987";

	public static final void main(String... args) throws Exception {
		HttpMDTManager mdt = HttpMDTManager.connectWithDefault();
		HttpMDTInstanceManager manager = mdt.getInstanceManager();
		
		WorkflowModel wfModel;
		
		wfModel = new WorkflowModel();
		wfModel.setId(WORKFLOW_ID);
		wfModel.setName("테스트 시뮬레이션");
		wfModel.setDescription("본 워크플로우는 시뮬레이션 연동을 확인하기 위한 테스트 목적으로 작성됨.");
		
		TaskDescriptor descriptor;
		
		descriptor = new TaskDescriptor();
		descriptor.setType(SetTask.class.getName());
		descriptor.setId("copy-data");
		descriptor.addInputArgument(ArgumentSpec.reference("source", "param:test:Data"));
		descriptor.addOutputArgument(ArgumentSpec.reference("target", "oparg:test:AddAndSleep:in:Data"));
		wfModel.getTaskDescriptors().add(descriptor);

		descriptor = new TaskDescriptor();
		descriptor.setType(SetTask.class.getName());
		descriptor.setId("copy-inc-amount");
		descriptor.addInputArgument(ArgumentSpec.reference("source", "param:test:IncAmount"));
		descriptor.addOutputArgument(ArgumentSpec.reference("target", "oparg:test:AddAndSleep:in:IncAmount"));
		wfModel.getTaskDescriptors().add(descriptor);
		
		descriptor = new TaskDescriptor();
		descriptor.setType(SetTask.class.getName());
		descriptor.setId("set-sleeptime");
		descriptor.addInputArgument(ArgumentSpec.literal("source", 3));
		descriptor.addOutputArgument(ArgumentSpec.reference("target", "oparg:test:AddAndSleep:in:SleepTime"));
		wfModel.getTaskDescriptors().add(descriptor);
		
		ByIdShortSubmodelReference smRef = DefaultSubmodelReference.ofIdShort("test", "AddAndSleep");
		smRef.activate(manager);
//		taskDesc = newHttpTask("simulation", smRef);
		descriptor = newAASOperationTask("sleep-and-add", smRef);
		wfModel.getTaskDescriptors().add(descriptor);
		
		descriptor = new TaskDescriptor();
		descriptor.setType(SetTask.class.getName());
		descriptor.setId("copy-result");
		descriptor.addInputArgument(ArgumentSpec.reference("source", "oparg:test:AddAndSleep:out:Output"));
		descriptor.addOutputArgument(ArgumentSpec.reference("target", "param:test:Data"));
		descriptor.addDependency("sleep-and-add");
		wfModel.getTaskDescriptors().add(descriptor);

		WorkflowManager wfManager = mdt.getWorkflowManager();
		wfModel = wfManager.addOrReplaceWorkflowModel(wfModel);

		System.out.println(wfModel.toJsonString());
	}
	
	private static TaskDescriptor newHttpTask(String taskId, ByIdShortSubmodelReference smRef) {
		TaskDescriptor descriptor = new TaskDescriptor();
		descriptor.setId(taskId);
		descriptor.setType(RESTfulAsyncRpcTask.class.getName());
		descriptor.addOption(RESTfulAsyncRpcTask.OPTION_BASE_URL, HTTP_OP_SERVER_ENDPOINT);
		descriptor.addOption(RESTfulAsyncRpcTask.OPTION_OPERATION_ENDPOINT, smRef.getInstanceId() + "/" + smRef.getSubmodelIdShort());
		descriptor.addOption(MDTTask.OPTION_TIMEOUT, "im");
		descriptor.addOption(MDTTask.OPTION_POLL_INTERVAL, "3s");
		descriptor.addLabel(TaskUtils.LABEL_MDT_OPERATION, smRef.toStringExpr());
		descriptor.addDependency("copy-data", "copy-inc-amount", "set-sleeptime");
		
		return descriptor;
	}
	
	private static TaskDescriptor newAASOperationTask(String taskId, ByIdShortSubmodelReference smRef) {
		String opElmExpr = DefaultElementReference.newInstance(smRef, "Operation").toStringExpr();
		
		TaskDescriptor descriptor = new TaskDescriptor();
		descriptor.setId(taskId);
		descriptor.setType(AASOperationTask.class.getName());
		descriptor.addOption(TaskDescriptor.OPTION_OPERATION, opElmExpr);
		descriptor.addOption(TaskDescriptor.OPTION_TIMEOUT, "im");
		descriptor.addOption(TaskDescriptor.OPTION_POLL_INTERVAL, "3s");
		descriptor.addOption(TaskDescriptor.OPTION_LOG_LEVEL, "info");
		descriptor.addLabel(TaskUtils.LABEL_MDT_OPERATION, smRef.toStringExpr());
		descriptor.addDependency("copy-data", "copy-inc-amount", "set-sleeptime");
		
		return descriptor;
	}
}
