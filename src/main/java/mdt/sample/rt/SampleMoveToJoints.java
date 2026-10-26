package mdt.sample.rt;

import java.time.Duration;
import java.util.Map;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationResult;
import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import mdt.client.HttpMDTManager;
import mdt.client.instance.HttpMDTInstanceManager;
import mdt.client.operation.AASOperationClient;
import mdt.model.sm.value.ElementCollectionValue;
import mdt.model.sm.value.ElementValues;
import mdt.rt.Ros2SerDes.JointsSerDe;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class SampleMoveToJoints {
	private static final JointsSerDe s_serde = new JointsSerDe();
	
	public static final void main(String... args) throws Exception {
		HttpMDTInstanceManager manager = HttpMDTManager.connectWithDefault().getInstanceManager();
		
		var panda01 = manager.getInstance("panda01");
		var data = panda01.getSubmodelServiceByIdShort("Data").get();
		
		AASOperationClient client = new AASOperationClient(data, "Operations.MoveToJoints",
															Duration.ofSeconds(1));
		
		var joints1 = Map.of(
			"panda_joint1", 0.0f,
			"panda_joint2", -1.0f,
			"panda_joint3", 0.0f,
			"panda_joint4", -1.5f,
			"panda_joint5", 0.0f,
			"panda_joint6", 1.5f,
			"panda_joint7", 0.0f
		);
		OperationVariable jointsOpv = client.getInputVariables().get("Joints");
		ElementValues.update(jointsOpv.getValue(), s_serde.serializeToElementValue(joints1));
		
//		DoublePropertyValue velScaleValue = new DoublePropertyValue(0.2);
//		OperationVariable velOpv = client.getInputVariables().get("VelocityScaling");
//		ElementValues.update(velOpv.getValue(), velScaleValue);
		
		OperationResult result = client.run();
		var outputs = result.getOutputArguments();
		SubmodelElement finalJoints = outputs.getFirst().getValue();
		ElementCollectionValue finalJointsValue = (ElementCollectionValue)ElementValues.getValue(finalJoints);
		System.out.println("FinalJoints: " + finalJointsValue);
		
		var joints2 = Map.of(
			"panda_joint1", 0.5f,
			"panda_joint2", -0.5f,
			"panda_joint3", -0.1f,
			"panda_joint4", -2.0f,
			"panda_joint5", 0.3f,
			"panda_joint6", 1.0f,
			"panda_joint7", -0.1f
		);
		
		client = new AASOperationClient(data, "Operations.MoveToJoints", Duration.ofSeconds(1));
		jointsOpv = client.getInputVariables().get("Joints");
		ElementValues.update(jointsOpv.getValue(), s_serde.serializeToElementValue(joints2));
		
		result = client.run();
		outputs = result.getOutputArguments();
		finalJoints = outputs.getFirst().getValue();
		finalJointsValue = (ElementCollectionValue)ElementValues.getValue(finalJoints);
		System.out.println("FinalJoints: " + finalJointsValue);
		
		var joints3 = Map.of(
			"panda_joint1", 0.0f,
			"panda_joint2", -0.785f,
			"panda_joint3", 0.0f,
			"panda_joint4", -2.356f,
			"panda_joint5", 0.0f,
			"panda_joint6", 1.571f,
			"panda_joint7", 0.785f
		);
		
		client = new AASOperationClient(data, "Operations.MoveToJoints", Duration.ofSeconds(1));
		jointsOpv = client.getInputVariables().get("Joints");
		ElementValues.update(jointsOpv.getValue(), s_serde.serializeToElementValue(joints3));
		
		result = client.run();
		outputs = result.getOutputArguments();
		finalJoints = outputs.getFirst().getValue();
		finalJointsValue = (ElementCollectionValue)ElementValues.getValue(finalJoints);
		System.out.println("FinalJoints: " + finalJointsValue);
	}
}
//panda_joint1	-2.8973	2.8973	
//panda_joint2	-1.7628	1.7628	범위가 가장 좁다
//panda_joint3	-2.8973	2.8973	
//panda_joint4	-3.0718	0.0175	거의 항상 음수여야 한다
//panda_joint5	-2.8973	2.8973	
//panda_joint6	-0.0175	3.7525	거의 항상 양수여야 한다
//panda_joint7	-2.8973	2.8973	