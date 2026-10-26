 package mdt.sample.rt;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationResult;
import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import mdt.client.HttpMDTManager;
import mdt.client.instance.HttpMDTInstanceManager;
import mdt.client.operation.AASOperationClient;
import mdt.model.SubmodelService;
import mdt.model.sm.value.ElementCollectionValue;
import mdt.model.sm.value.ElementValue;
import mdt.model.sm.value.ElementValues;
import mdt.model.sm.value.PropertyValue.DoublePropertyValue;
import mdt.rt.Ros2SerDes.PoseSerDe;
import mdt.rt.Ros2SerDes.PoseStampedSerDe;
import mdt.rt.Ros2Types.Orientation;
import mdt.rt.Ros2Types.Pose;
import mdt.rt.Ros2Types.PoseStamped;
import mdt.rt.Ros2Types.Position;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class SamplePickNPlace {
	private static final PoseSerDe s_poseSerDe = new PoseSerDe();
    private static final PoseStampedSerDe s_poseStampedSerDe = new PoseStampedSerDe();

	private static final Position CUBE_POSITION = new Position(0.5189f, 0.0774f, 0.025f);
	private static final Position ABOVE_POSITION = new Position(0.5189f, 0.0774f, 0.125f);
	private static final Position NEW_ABOVE_POSITION = new Position(0.411f, 0.326f, 0.125f);
	private static final Position NEW_CUBE_POSITION = new Position(0.411f, 0.326f, 0.025f);
	private static final Orientation GRIPPER_ORIENTATION = new Orientation(1.0f, 0.0f, 0.0f, 0.0f);

	
	public static final void main(String... args) throws Exception {
		HttpMDTInstanceManager manager = HttpMDTManager.connectWithDefault().getInstanceManager();
		
		var panda01 = manager.getInstance("panda01");
		var data = panda01.getSubmodelServiceByIdShort("Data").get();
		AASOperationClient grasp = new AASOperationClient(data, "Operations.MoveGripperToTarget", Duration.ofMillis(100));

		// 큐브 위로 이동
		moveLinear(data, ABOVE_POSITION, 0.2);
		
		// 그리퍼 열기
		moveGripperToTarget(data, "open");
		
		// 큐브를 잡기 위해 내려가기
		moveLinear(data, CUBE_POSITION, 0.2);
		
		// 그리퍼 쥐기
		moveGripperToTarget(data, "grasp");
		
		// 큐브를 들어올리기 위해 위로 이동
		moveLinear(data, ABOVE_POSITION, 0.2);
		
		// 새로운 위치로 이동
		moveLinear(data, NEW_ABOVE_POSITION, 0.2);
		
		// 큐브를 놓기 위해 내려가기
		moveLinear(data, NEW_CUBE_POSITION, 0.2);
		
		// 그리퍼 열기
		moveGripperToTarget(data, "open");
		
		// 그리퍼를 들어올리기 위해 위로 이동
		moveLinear(data, NEW_ABOVE_POSITION, 0.2);
		
		// 'Home' 위치로 이동
		moveToNamedTarget(data, "ready", 0.2);
	}
	
	private static final PoseStamped moveLinear(SubmodelService robotSvc, Position targetPos, double velScale)
		throws IOException, CancellationException, InterruptedException, ExecutionException {
		ElementValue poseValue = s_poseSerDe.serializeToElementValue(new Pose(targetPos, GRIPPER_ORIENTATION));
		
		AASOperationClient moveLinear = new AASOperationClient(robotSvc, "Operations.MoveLinear",
																Duration.ofMillis(100));
		OperationVariable poseOpv = moveLinear.getInputVariables().get("TargetPose");
		ElementValues.update(poseOpv.getValue(), poseValue);
		
		DoublePropertyValue velScaleValue = new DoublePropertyValue(velScale);
		OperationVariable velOpv = moveLinear.getInputVariables().get("VelocityScaling");
		ElementValues.update(velOpv.getValue(), velScaleValue);
		
		OperationResult result = moveLinear.run();
		var outputs = result.getOutputArguments();
		SubmodelElement finalPoseSme = outputs.getFirst().getValue();
		ElementCollectionValue finalPoseValue = (ElementCollectionValue)ElementValues.getValue(finalPoseSme);
		return s_poseStampedSerDe.deserializeFromElementValue(finalPoseValue);
	}
	
	private static final void moveGripperToTarget(SubmodelService robotSvc, String target)
		throws IOException, CancellationException, InterruptedException, ExecutionException {
		AASOperationClient moveGripperToTarget = new AASOperationClient(robotSvc, "Operations.MoveGripperToTarget",
																Duration.ofMillis(100));
		OperationVariable poseOpv = moveGripperToTarget.getInputVariables().get("Target");
		ElementValues.update(poseOpv.getValue(), target);
		
		OperationResult result = moveGripperToTarget.run();
		var outputs = result.getOutputArguments();
//		SubmodelElement finalPoseSme = outputs.getFirst().getValue();
//		ElementCollectionValue finalPoseValue = (ElementCollectionValue)ElementValues.getValue(finalPoseSme);
//		return s_poseStampedSerDe.deserializeFromElementValue(finalPoseValue);
	}
	
	private static final PoseStamped moveToNamedTarget(SubmodelService robotSvc, String target, double velScale)
		throws IOException, CancellationException, InterruptedException, ExecutionException {
		AASOperationClient moveLinear = new AASOperationClient(robotSvc, "Operations.MoveToNamedTarget",
																Duration.ofMillis(100));
		OperationVariable poseOpv = moveLinear.getInputVariables().get("TargetName");
		ElementValues.update(poseOpv.getValue(), target);
		
		DoublePropertyValue velScaleValue = new DoublePropertyValue(velScale);
		OperationVariable velOpv = moveLinear.getInputVariables().get("VelocityScaling");
		ElementValues.update(velOpv.getValue(), velScaleValue);
		
		OperationResult result = moveLinear.run();
		var outputs = result.getOutputArguments();
		SubmodelElement finalPoseSme = outputs.getFirst().getValue();
		ElementCollectionValue finalPoseValue = (ElementCollectionValue)ElementValues.getValue(finalPoseSme);
		return s_poseStampedSerDe.deserializeFromElementValue(finalPoseValue);
	}
	
}
//1	파지 접근	(0.5189, 0.0774, 0.125)	(1, 0, 0, 0)
//2	파지	(0.5189, 0.0774, 0.025)	(1, 0, 0, 0)
//3	상승	(0.5189, 0.0774, 0.125)	(1, 0, 0, 0)
//4	놓기 접근	(0.4107, 0.3265, 0.125)	(0.9659, 0.2588, 0, 0)
//5	놓기	(0.4107, 0.3265, 0.025)	(0.9659, 0.2588, 0, 0)
//6	상승	(0.4107, 0.3265, 0.125)	(0.9659, 0.2588, 0, 0)
//7	ready 복귀	— (관절 공간)	—