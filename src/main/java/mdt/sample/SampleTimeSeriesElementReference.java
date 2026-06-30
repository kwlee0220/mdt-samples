package mdt.sample;

import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;

import utils.stream.KeyValueFStream;

import mdt.client.HttpMDTManager;
import mdt.model.instance.MDTInstanceManager;
import mdt.model.sm.ref.ElementReferences;
import mdt.model.sm.ref.MDTElementReference;
import mdt.model.sm.value.ElementCollectionValue;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class SampleTimeSeriesElementReference {
	public static void main(String[] args) throws Exception {
		// 기본적인 설정 정보를 이용하여 MDT Manager에 접속한다.
		HttpMDTManager mdt = HttpMDTManager.connectWithDefault();
		
		// MDT Manager에 접속한 후에는 MDTInstanceManager를 얻어올 수 있다.
		MDTInstanceManager manager = mdt.getInstanceManager();
		
		// TimeSeries SubmodelElementReference 관련
		readRecords2(manager);
	}
	
	private static void readRecords1(MDTInstanceManager manager) throws Exception {
		String refStr = "timeseries:Welder:NozzleProductionLog";
		printSegment(manager, refStr);
	}
	
	private static void readRecords2(MDTInstanceManager manager) throws Exception {
		String refStr = "timeseries:Welder:NozzleProductionLog#last=5";
		printSegment(manager, refStr);
	}
	
	private static void readRecords3(MDTInstanceManager manager) throws Exception {
		String refStr = "timeseries:Welder:NozzleProductionLog#last=30s|QuantityProduced,DefectVolume";
		printSegment(manager, refStr);
	}
	
	private static void readRecords4(MDTInstanceManager manager) throws Exception {
		String refStr = "timeseries:Welder:NozzleProductionLog#last=1m@latest|Time";
		printSegment(manager, refStr);
	}
	
	private static void readRecords5(MDTInstanceManager manager) throws Exception {
		String refStr = "timeseries:Welder:NozzleProductionLog#2023-05-25T04:20:42~2023-05-25T04:21:14|Time";
		printSegment(manager, refStr);
	}
	
	private static void printSegment(MDTInstanceManager manager, String refExpr) throws Exception {
		MDTElementReference ref = ElementReferences.parseExpr(refExpr);
		ref.activate(manager);
		
		String jsonStr = ref.toJsonString();
		System.out.println(jsonStr);
		
		MDTElementReference ref2 = (MDTElementReference)ElementReferences.parseJsonString(jsonStr);
		ref2.activate(manager);
		System.out.println(ref2);
		
		SubmodelElementCollection ts1 = ref2.readCollection();
		for ( SubmodelElement rec: ts1.getValue() ) {
			System.out.println(rec);
		}
		
		ElementCollectionValue values = (ElementCollectionValue)ref2.readValue();
		KeyValueFStream.from(values.getFieldMap())
						.forEach((k, v) -> System.out.println(k + ": " + v));
	}
}
