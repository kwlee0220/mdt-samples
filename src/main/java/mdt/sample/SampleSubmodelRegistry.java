package mdt.sample;

import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelDescriptor;

import mdt.aas.SubmodelRegistry;
import mdt.client.HttpMDTManager;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class SampleSubmodelRegistry {
	public static final void main(String... args) throws Exception {
		JsonSerializer ser = new JsonSerializer();
		
		HttpMDTManager mdtClient = HttpMDTManager.connect("http://localhost:12985");
		
		SubmodelRegistry registry = mdtClient.getSubmodelRegistry();
		for ( SubmodelDescriptor desc: registry.getAllSubmodelDescriptors() ) {
			System.out.println(desc);
		}
		
		SubmodelDescriptor desc
			= registry.getSubmodelDescriptorById("http://mdt.etri.re.kr/mdt/Test/sm/InformationModel");
		System.out.println(ser.write(desc));
		
		for ( SubmodelDescriptor aasDesc: registry.getAllSubmodelDescriptorsByIdShort("Data") ) {
			System.out.println(aasDesc);
		}
	}
}
