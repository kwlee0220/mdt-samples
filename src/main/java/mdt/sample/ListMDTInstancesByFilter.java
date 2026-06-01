package mdt.sample;

import java.util.List;

import mdt.client.HttpMDTManager;
import mdt.model.instance.MDTInstance;
import mdt.model.instance.MDTInstanceManager;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class ListMDTInstancesByFilter {
	public static void main(String[] args) throws Exception {
		// 기본적인 설정 정보를 이용하여 MDT Manager에 접속한다.
		HttpMDTManager mdt = HttpMDTManager.connectWithDefault();
		
		// MDT Manager에 접속한 후에는 MDTInstanceManager를 얻어올 수 있다.
		MDTInstanceManager manager = mdt.getInstanceManager();
		
		var instanceList = manager.getInstanceAllByFilter("instance.instanceId like 'HEAT-%'");
		// 조회된 MDTInstance들의 목록을 출력한다.
		for ( MDTInstance instance : instanceList ) {
			System.out.println(instance.getId());
		}
	}
}
