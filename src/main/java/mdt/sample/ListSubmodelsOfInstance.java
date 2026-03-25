package mdt.sample;

import java.util.List;

import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;

import mdt.client.HttpMDTManager;
import mdt.model.ReferenceUtils;
import mdt.model.SubmodelService;
import mdt.model.instance.MDTInstance;
import mdt.model.instance.MDTInstanceManager;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class ListSubmodelsOfInstance {
	public static void main(String[] args) throws Exception {
		// 기본적인 설정 정보를 이용하여 MDT Manager에 접속한다.
		HttpMDTManager mdt = HttpMDTManager.connectWithDefault();
		
		// MDT Manager에 접속한 후에는 MDTInstanceManager를 얻어올 수 있다.
		MDTInstanceManager manager = mdt.getInstanceManager();
		
		// 'test'라는 식별자를 가진 MDTInstance를 얻어온다.
		MDTInstance inst = manager.getInstance("Welder");
		
		// MDTInstance에 등록된 SubmodelService 목록을 얻어온다.
		List<? extends SubmodelService> smSvcList = inst.getSubmodelServiceAll();
		for ( SubmodelService smSvc : smSvcList ) {
			Submodel sm = smSvc.getSubmodel();
			String semanticIdStr = ReferenceUtils.getSemanticIdStringOrNull(sm.getSemanticId());
			System.out.printf("id=%s, idShort=%s, semanticId=%s%n", sm.getId(), sm.getIdShort(), semanticIdStr);
		}
	}
}
