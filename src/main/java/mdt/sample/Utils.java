package mdt.sample;

import mdt.model.instance.MDTInstance;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public final class Utils {
	private Utils() {
		throw new AssertionError("Should not be called: class=" + getClass().getName());
	}
	
	public static void printMDTInstance(MDTInstance inst) {
		System.out.printf("id: %s%n", inst.getId());
		System.out.printf("AAS-id: %s%n", inst.getAasId());
		System.out.printf("AAS-idShort: %s%n", inst.getAasIdShort());
		System.out.printf("GlobalAssetId: %s%n", inst.getGlobalAssetId());
		System.out.printf("AssetType: %s%n", inst.getAssetType());
		System.out.printf("status: %s%n", inst.getStatus());
		System.out.printf("isRunning: %s%n", inst.isRunning());
		System.out.printf("endpoint: %s%n", inst.getServiceEndpoint());
	}
}
