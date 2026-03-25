package mdt.sample;

import java.util.List;
import java.util.Map;

import utils.KeyValue;
import utils.func.Funcs;

import mdt.client.HttpMDTManager;
import mdt.model.instance.MDTInstance;
import mdt.model.instance.MDTInstanceManager;
import mdt.model.instance.MDTParameterDescriptor;
import mdt.model.instance.MDTParameterService;
import mdt.model.sm.value.ElementValue;
import mdt.model.sm.value.PropertyValue.StringPropertyValue;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class ListParametersOfInstance {
	public static void main(String[] args) throws Exception {
		// 기본적인 설정 정보를 이용하여 MDT Manager에 접속한다.
		HttpMDTManager mdt = HttpMDTManager.connectWithDefault();
		
		// MDT Manager에 접속한 후에는 MDTInstanceManager를 얻어올 수 있다.
		MDTInstanceManager manager = mdt.getInstanceManager();
		
		// 'test'라는 식별자를 가진 MDTInstance를 얻어온다.
		MDTInstance inst = manager.getInstance("Welder");
		
		// MDTInstance에 등록된 파라미터 목록을 얻어온다.
		List<MDTParameterService> paramColl = inst.getParameterServiceAll();
		System.out.printf("length=%d%n", paramColl.size());
		System.out.println(Funcs.map(paramColl, svc -> svc.getDescriptor().getName()));
		for ( MDTParameterService paramSvc : paramColl ) {
			MDTParameterDescriptor paramDesc = paramSvc.getDescriptor();
			System.out.printf("%s: name=%s type=%s value=%s%n",
							paramDesc.getId(), paramDesc.getName(), paramDesc.getValueType(), paramSvc.read());
		}
		
		Map<String,MDTParameterService> paramMap = Funcs.toMap(paramColl, svc -> KeyValue.of(svc.getId(), svc));
		
		MDTParameterService status = paramMap.get("Status");
		ElementValue old = status.read();
		status.update(StringPropertyValue.STRING("XXX"));
		System.out.printf("%s: old=%s new=%s%n",
							status.getDescriptor().getName(),
							old.toDisplayString(), status.read().toDisplayString());
		status.update(old);
	}
}
