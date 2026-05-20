package mdt.sample;

import java.time.Duration;

import mdt.client.HttpMDTManager;
import mdt.model.instance.MDTInstance;
import mdt.model.instance.MDTInstanceManager;
import mdt.model.instance.MDTInstanceStatus;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class StartStopMDTInstance {
	public static void main(String[] args) throws Exception {
		// 기본적인 설정 정보를 이용하여 MDT Manager에 접속한다.
		HttpMDTManager mdt = HttpMDTManager.connectWithDefault();
		
		// MDT Manager에 접속한 후에는 MDTInstanceManager를 얻어올 수 있다.
		MDTInstanceManager manager = mdt.getInstanceManager();
		
		// 'test'라는 식별자를 가진 MDTInstance를 얻어온다.
		MDTInstance inst = manager.getInstance("test");
		
		if ( inst.isRunning() ) {
			inst.stop(Duration.ofSeconds(1), Duration.ofMinutes(1));
			System.out.println("Stop MDTInstance");
		}
		
		MDTInstanceStatus status = inst.start(Duration.ofSeconds(1), Duration.ofMinutes(1));
		System.out.println("Start MDTInstance: " + status);
		
		inst.stop(Duration.ofSeconds(1), Duration.ofMinutes(1));
		System.out.println("Stop MDTInstance");
		
		// MDTInstance의 속성 정보를 출력한다.
		Utils.printMDTInstance(inst);
	}
}
