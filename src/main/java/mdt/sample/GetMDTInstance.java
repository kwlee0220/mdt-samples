package mdt.sample;

import mdt.client.HttpMDTManager;
import mdt.model.instance.MDTInstance;
import mdt.model.instance.MDTInstanceManager;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class GetMDTInstance {
	public static void main(String[] args) throws Exception {
		// 기본적인 설정 정보를 이용하여 MDT Manager에 접속한다.
		HttpMDTManager mdt = HttpMDTManager.connectWithDefault();
		
		// MDT Manager에 접속한 후에는 MDTInstanceManager를 얻어올 수 있다.
		MDTInstanceManager manager = mdt.getInstanceManager();
		
		// 'test'라는 식별자를 가진 MDTInstance를 얻어온다.
		MDTInstance inst = manager.getInstance("test");
		
		System.out.printf("%s: status=%s, endpoint=%s", inst.getId(), inst.getStatus(), inst.getServiceEndpoint());
	}
}
