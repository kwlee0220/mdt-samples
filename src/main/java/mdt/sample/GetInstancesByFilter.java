package mdt.sample;

import mdt.client.HttpMDTManager;
import mdt.model.instance.MDTInstanceManager;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class GetInstancesByFilter {
	public static void main(String[] args) throws Exception {
		// 기본적인 설정 정보를 이용하여 MDT Manager에 접속한다.
		HttpMDTManager mdt = HttpMDTManager.connectWithDefault();
		
		// MDT Manager에 접속한 후에는 MDTInstanceManager를 얻어올 수 있다.
		MDTInstanceManager manager = mdt.getInstanceManager();
		
		var instances = manager.getInstanceAllByFilter("parameter.id='CurrentLotNo'");
		for ( var inst : instances ) {
			System.out.printf("id=%s, status=%s%n", inst.getId(), inst.getStatus());
		}
		
		instances = manager.getInstanceAllByFilter(
										"submodel.semanticId = 'https://etri.re.kr/mdt/Submodel/Data'");
		for ( var inst : instances ) {
			System.out.printf("id=%s, status=%s%n", inst.getId(), inst.getStatus());
		}
	}
}
