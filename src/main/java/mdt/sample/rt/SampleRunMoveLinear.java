 package mdt.sample.rt;

import java.time.Duration;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationResult;
import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import mdt.client.HttpMDTManager;
import mdt.client.instance.HttpMDTInstanceManager;
import mdt.client.operation.AASOperationClient;
import mdt.model.sm.value.ElementCollectionValue;
import mdt.model.sm.value.ElementValues;
import mdt.model.sm.value.PropertyValue.DoublePropertyValue;
import mdt.rt.Ros2SerDes.PoseSerDe;
import mdt.rt.Ros2Types.Orientation;
import mdt.rt.Ros2Types.Pose;
import mdt.rt.Ros2Types.Position;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class SampleRunMoveLinear {
	private static final PoseSerDe s_poseSerDe = new PoseSerDe();
	
	public static final void main(String... args) throws Exception {
		HttpMDTInstanceManager manager = HttpMDTManager.connectWithDefault().getInstanceManager();
		
		var panda01 = manager.getInstance("panda01");
		var data = panda01.getSubmodelServiceByIdShort("Data").get();
		AASOperationClient client = new AASOperationClient(data, "Operations.MoveLinear",
															Duration.ofSeconds(1));
		
		Pose targetPose = new Pose(new Position(0.3f, 0.0f, 0.5f),
									new Orientation(1.0f, 0.0f, 0.0f, 0.0f));
		var poseValue = s_poseSerDe.serializeToElementValue(targetPose);
		
		OperationVariable poseOpv = client.getInputVariables().get("TargetPose");
		ElementValues.update(poseOpv.getValue(), poseValue);
		
		DoublePropertyValue velScaleValue = new DoublePropertyValue(0.2);
		OperationVariable velOpv = client.getInputVariables().get("VelocityScaling");
		ElementValues.update(velOpv.getValue(), velScaleValue);
		
		OperationResult result = client.run();
		var outputs = result.getOutputArguments();
		SubmodelElement finalPose = outputs.getFirst().getValue();
		ElementCollectionValue finalPoseValue = (ElementCollectionValue)ElementValues.getValue(finalPose);
		System.out.println("FinalPose: " + finalPoseValue.getField("Pose"));
	}
}