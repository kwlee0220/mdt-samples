package mdt.sample;

import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShellDescriptor;

import mdt.aas.ShellRegistry;
import mdt.client.HttpMDTManager;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class SampleShellRegistry {
	private static final String REGISTRY_AAS_ID = "http://mdt.etri.re.kr/mdt/Test";
	
	public static final void main(String... args) throws Exception {
		HttpMDTManager mdtClient = HttpMDTManager.connect("http://localhost:12985");
		
		ShellRegistry registry = mdtClient.getAssetAdministrationShellRegistry();
		for ( AssetAdministrationShellDescriptor aasDesc: registry.getAllAssetAdministrationShellDescriptors() ) {
			System.out.println(aasDesc);
		}
		
		AssetAdministrationShellDescriptor desc
			= registry.getAssetAdministrationShellDescriptorById(REGISTRY_AAS_ID);
		System.out.println("Found Shell: " + desc.getId());
		
		for ( AssetAdministrationShellDescriptor aasDesc:
						registry.getAllAssetAdministrationShellDescriptorsByIdShort("HEAT-02") ) {
			System.out.println("Found Shell: " + aasDesc.getId());
		}
		
		for ( AssetAdministrationShellDescriptor aasDesc:
			registry.getAllAssetAdministrationShellDescriptorByAssetId("QualityInspectionEquipment") ) {
			System.out.println("Found Shell: " + aasDesc.getId());
		}
	}
}
