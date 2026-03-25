package mdt.sample;

import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import mdt.client.HttpMDTManager;
import mdt.model.ReferenceUtils;
import mdt.model.SubmodelService;
import mdt.model.instance.MDTInstance;
import mdt.model.instance.MDTInstanceManager;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class GetSubmodelOfInstance {
	public static void main(String[] args) throws Exception {
		// 기본적인 설정 정보를 이용하여 MDT Manager에 접속한다.
		HttpMDTManager mdt = HttpMDTManager.connectWithDefault();
		
		// MDT Manager에 접속한 후에는 MDTInstanceManager를 얻어올 수 있다.
		MDTInstanceManager manager = mdt.getInstanceManager();
		
		// 'test'라는 식별자를 가진 MDTInstance를 얻어온다.
		MDTInstance inst = manager.getInstance("test");
		
		// MDTInstance에 등록된 SubmodelService 중에서 idShort이 'Data'인 SubmodelService를 얻어온다.
		SubmodelService smSvc = inst.getSubmodelServiceByIdShort("Data");

		// SubmodelService에서 Submodel을 얻어온다.
		Submodel sm = smSvc.getSubmodel();
		String semanticIdStr = ReferenceUtils.getSemanticIdStringOrNull(sm.getSemanticId());
		System.out.printf("id=%s, idShort=%s, semanticId=%s%n", sm.getId(), sm.getIdShort(), semanticIdStr);
		
		for ( SubmodelElement topSme: sm.getSubmodelElements() ) {
			String topSmeSemanticIdStr = ReferenceUtils.getSemanticIdStringOrNull(topSme.getSemanticId());
			System.out.printf("  id=%s, semanticId=%s%n", topSme.getIdShort(), 
					topSmeSemanticIdStr);
		}
	}
}
