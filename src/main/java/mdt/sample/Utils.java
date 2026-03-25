package mdt.sample;

import lombok.experimental.UtilityClass;

import mdt.model.instance.MDTInstance;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@UtilityClass
public class Utils {
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
