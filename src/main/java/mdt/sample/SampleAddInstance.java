package mdt.sample;

import java.io.File;

import mdt.client.HttpMDTManager;
import mdt.client.instance.HttpMDTInstanceManager;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class SampleAddInstance {
	private static final String ENDPOINT = "http://61.98.138.54:10133";
	
	public static final void main(String... args) throws Exception {
		// MDTInstanceManager에 접속함.
		HttpMDTInstanceManager manager = HttpMDTManager.connect(ENDPOINT)
																	.getInstanceManager();
		
		// 인스턴스 디렉터리(model.json, config.json 등)를 bundle로 묶어 올린다. port가 -1이면 매니저가 정한다.
		manager.addInstance("Test", -1, new File("/home/kwlee/mdt/models/test"));
		manager.addInstance("surface", -1, new File("/home/kwlee/mdt/models/surface"));
	}
}
