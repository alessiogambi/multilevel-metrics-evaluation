/**
 * Copyright 2013 Technische Universitat Wien (TUW), Distributed Systems Group E184
 *
 * This work was partially supported by the European Commission in terms of the CELAR FP7 project (FP7-ICT-2011-8 \#317790)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package at.ac.tuwien.dsg.melaclient;

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import junit.framework.TestCase;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;

/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 **/
public class MELA_APITest extends TestCase {

    /**
     * Test of main method, of class MELA_API.
     */
    public void testMain() throws Exception {
        System.out.println("main");
        String[] args = null;
        
        String serviceDescription = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<MonitoredElement id=\"CloudService\" level=\"SERVICE\">"
                + "<MonitoredElement id=\"DataEndServiceTopology\" level=\"SERVICE_TOPOLOGY\">"
                + "  <MonitoredElement id=\"DataControllerServiceUnit\" level=\"SERVICE_UNIT\"/>"
                + " <MonitoredElement id=\"DataNodeServiceUnit\" level=\"SERVICE_UNIT\"/>"
                + " </MonitoredElement>"
                + " <MonitoredElement id=\"EventProcessingServiceTopology\" level=\"SERVICE_TOPOLOGY\">"
                + "      <MonitoredElement id=\"LoadBalancerServiceUnit\" level=\"SERVICE_UNIT\"/>"
                + "      <MonitoredElement id=\"EventProcessingServiceUnit\" level=\"SERVICE_UNIT\"/>"
                + "  </MonitoredElement>"
                + "</MonitoredElement>";


        String compositionRules = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<CompositionRulesConfiguration>"
                + "<MetricsCompositionRules>"
                + "<CompositionRule TargetMonitoredElementLevel=\"VM\">"
                + "<ResultingMetric type=\"RESOURCE\" measurementUnit=\"ms\" name=\"numberOfVMs\"/>"
                + "<Operation value=\"1\" type=\"SET_VALUE\"/>"
                + "</CompositionRule>"
                + "<CompositionRule TargetMonitoredElementLevel=\"SERVICE_UNIT\">"
                + "<ResultingMetric type=\"RESOURCE\" measurementUnit=\"ms\" name=\"numberOfVMs\"/>"
                + "<Operation MetricSourceMonitoredElementLevel=\"VM\" type=\"SUM\">"
                + "<ReferenceMetric type=\"RESOURCE\" name=\"numberOfVMs\"/>"
                + "</Operation>"
                + "</CompositionRule>"
                + "</MetricsCompositionRules>"
                + "</CompositionRulesConfiguration>";

//        Unmarshaller unmarshaller = JAXBContext.newInstance(CompositionRulesConfiguration.class).createUnmarshaller();
//        CompositionRulesConfiguration compositionRulesConfiguration = (CompositionRulesConfiguration) unmarshaller.unmarshal(new StringReader(compositionRules));
//
//        
//        //TODO: express in new way cloud service config
//
////        new MELA_API().submitServiceConfiguration(null);
//
//
//        new MELA_API().submitMetricCompositionConfiguration(compositionRulesConfiguration);
//        new MELA_API().refreshMonitoringData();
////        new MELA_API().notifyControlActionStarted(compositionRules, );
    }
}