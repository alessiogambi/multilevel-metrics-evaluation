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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import at.ac.tuwien.dsg.csdg.Node;
import at.ac.tuwien.dsg.csdg.Node.NodeType;
import at.ac.tuwien.dsg.csdg.Relationship.RelationshipType;
import at.ac.tuwien.dsg.csdg.elasticityInformation.ElasticityRequirement;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;

public class MELA_API {
    private boolean existsStructureData = false;
    private boolean serviceSet = false;
    private static final String REST_API_URL = "http://localhost:8080/MELA-AnalysisService-0.1-SNAPSHOT/REST_WS";
    private static final int MONITORING_DATA_REFRESH_INTERVAL = 5; //in seconds
    private MonitoredElementMonitoringSnapshot latestMonitoringData;
    private AtomicBoolean monitoringDataUsed;

    {
        latestMonitoringData = new MonitoredElementMonitoringSnapshot();
        monitoringDataUsed = new AtomicBoolean(false);
    }
    public MELA_API(){
    	
    }
//todo: Continuous refresh data with a semaphore like mechanism to synchronzie resource access
    {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
            	if (serviceSet)
                   refreshMonitoringData();
            }
        };

        Timer monitoringDataRefreshTimer = new Timer();
        monitoringDataRefreshTimer.schedule(task, 0, MONITORING_DATA_REFRESH_INTERVAL * 1000);
    }

//     private List<MetricFilter> getMetricFilters() {
//        List<MetricFilter> filters = new ArrayList<MetricFilter>();
//        MetricFilter metricFilter = new MetricFilter();
//        metricFilter.setId("VMLevelCassandra");
//        Metric m;
//        metricFilter.setLevel(ServiceElement.ServiceElementLevel.VM);
//        Collection<Metric> metrics = new ArrayList<Metric>();
//        metrics.add(new Metric("cpu_usage"));
//        metrics.add(new Metric("mem_used"));
//        metrics.add(new Metric("pkts_total"));
//
//        metricFilter.setMetrics(metrics);
//        filters.add(metricFilter);
//
//
//        metricFilter = new MetricFilter();
//
//        metricFilter.setId("SERVICE_UNITLevel");
//        metricFilter.setLevel(ServiceElement.ServiceElementLevel.SERVICE_UNIT);
//
//        metrics = new ArrayList<Metric>();
//        m = new Metric("cpu_usage");
//        metrics.add(m);
//        m = new Metric("read_latency");
//        metrics.add(m);
//        m = new Metric("costPerHour");
//        metrics.add(m);
//        m = new Metric("write_latency");
//        metrics.add(m);
//        m = new Metric("responseTime");
//        metrics.add(m);
//        m = new Metric("throughput");
//        metrics.add(m);
//        m = new Metric("throughput_average");
//        metrics.add(m);
//        m = new Metric("clientsNb");
//        metrics.add(m);
//        metricFilter.setMetrics(metrics);
//
//
//        filters.add(metricFilter);
//        metricFilter = new MetricFilter();
//        metricFilter.setId("SERVICE_TOPOLOGYLevel");
//        metricFilter.setLevel(ServiceElement.ServiceElementLevel.SERVICE_TOPOLOGY);
//
//        metrics = new ArrayList<Metric>();
//        m = new Metric("costPerHour");
//        metrics.add(m);
//        m = new Metric("responseTime");
//        metrics.add(m);
//        m = new Metric("clientsNb");
//        metrics.add(m);
//        metricFilter.setMetrics(metrics);
//
//        filters.add(metricFilter);
//        metricFilter = new MetricFilter();
//        metricFilter.setId("SERVICELevel");
//        metricFilter.setLevel(ServiceElement.ServiceElementLevel.SERVICE);
//        metrics = new ArrayList<Metric>();
//        metrics.add(new Metric("cpu_usage"));
//        m = new Metric("clientsNb");
//        metrics.add(m);
//        m = new Metric("costPerHour");
//        
//        metrics.add(m);
//
//        
//        m = new Metric("costPerClientPerHour");
//        metrics.add(m);
//        metricFilter.setMetrics(metrics);
//
//        filters.add(metricFilter);
//        return filters;
//
//    }
    /**
     * pulls new monitoring data
     */
    public void refreshMonitoringData() {
        URL url = null;
        HttpURLConnection connection = null;
        try {
            url = new URL(REST_API_URL + "/monitoringdataXML");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setRequestProperty("Accept", "application/xml");

            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    Logger.getLogger(MELA_API.class.getName()).log(Level.SEVERE, line);
                }
            }

            InputStream inputStream = connection.getInputStream();
            JAXBContext jAXBContext = JAXBContext.newInstance(MonitoredElementMonitoringSnapshot.class);
            MonitoredElementMonitoringSnapshot retrievedData = (MonitoredElementMonitoringSnapshot) jAXBContext.createUnmarshaller().unmarshal(inputStream);

            if (retrievedData != null) {
                //P
                getLatestMonitoringDataLock();
                latestMonitoringData = retrievedData;
                //V
                releaseLatestMonitoringDataLock();
            }

        } catch (Exception e) {
            Logger.getLogger(MELA_API.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    //uses a semaphore like mechanism to synchronzie access to the latestMonitoringData to avoid data refresh when data is queried
    private void getLatestMonitoringDataLock() {
        while (monitoringDataUsed.get()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(MELA_API.class.getName()).log(Level.SEVERE, null, ex);
            }
        };

        monitoringDataUsed.set(true);
    }

    private void releaseLatestMonitoringDataLock() {
        monitoringDataUsed.set(false);
    }

    public void submitServiceConfiguration(Node cloudService) {
        MonitoredElement element = new MonitoredElement();
        element.setId(cloudService.getId());
        element.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE);

        MELA_ClientUtils.convertServiceTopology(element, cloudService);

        URL url = null;
        HttpURLConnection connection = null;
        try {
            url = new URL(REST_API_URL + "/servicedescription");
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setRequestProperty("Accept", "application/json");

            //write message body
            OutputStream os = connection.getOutputStream();
            JAXBContext jaxbContext = JAXBContext.newInstance(MonitoredElement.class);
            jaxbContext.createMarshaller().marshal(element, os);
            os.flush();
            os.close();

            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    Logger.getLogger(MELA_API.class.getName()).log(Level.SEVERE, line);
                }
            }

            InputStream inputStream = connection.getInputStream();
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    Logger.getLogger(MELA_API.class.getName()).log(Level.SEVERE, line);
                }
            }

        } catch (Exception e) {
            Logger.getLogger(MELA_API.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        serviceSet = true;
     
    }

    public void submitMetricCompositionConfiguration(CompositionRulesConfiguration compositionRulesConfiguration) {

        URL url = null;
        HttpURLConnection connection = null;
        try {
            url = new URL(REST_API_URL + "/metricscompositionrules");
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setRequestProperty("Accept", "application/json");

            //write message body
            OutputStream os = connection.getOutputStream();
            JAXBContext jaxbContext = JAXBContext.newInstance(CompositionRulesConfiguration.class);
            jaxbContext.createMarshaller().marshal(compositionRulesConfiguration, os);
            os.flush();
            os.close();

            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    Logger.getLogger(MELA_API.class.getName()).log(Level.SEVERE, line);
                }
            }

            InputStream inputStream = connection.getInputStream();
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    Logger.getLogger(MELA_API.class.getName()).log(Level.SEVERE, line);
                }
            }

        } catch (Exception e) {
            Logger.getLogger(MELA_API.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

    }

//    public float getCpuUsage(Node entity) {
//        Metric metric = new Metric("cpu_usage");
//        RuntimeLogger.logger.info("For entity " + entity.getId() + " Cpu usage is " + getMetricValue(metric, entity) + "cpu idle is" + getMetricValue(new Metric("cpu_idle"), entity));
//        return getMetricValue(metric, entity);
//    }
//
//    public float getMemoryAvailable(Node entity) {
//        Metric metric = new Metric("mem_free_in_GB");
//        return getMetricValue(metric, entity);
//    }
//
//    public float getMemorySize(Node entity) {
//        Metric metric = new Metric("mem_total_in_GB");
//        return getMetricValue(metric, entity);
//    }
//
//    public float getMemoryUsage(Node entity) {
//        Metric metric = new Metric("mem_used");
//        return getMetricValue(metric, entity);
//    }
//
//    public float getDiskSize(Node entity) {
//        Metric metric = new Metric("disk_total");
//        return getMetricValue(metric, entity);
//    }
//
//    public float getDiskAvailable(Node entity) {
//        Metric metric = new Metric("disk_free");
//        return getMetricValue(metric, entity);
//    }
//
//    //TODO: define agg rule in procentaj
//    public float getDiskUsage(Node entity) {
//
//        return (getDiskSize(entity) - getDiskAvailable(entity)) / getDiskSize(entity) * 100;
//    }
//
//    public float getCPUSpeed(Node entity) {
//
//        Metric metric = new Metric("cpu_speed");
//        return getMetricValue(metric, entity);
//    }
//
//    //TODO: define agg rule in TOTAL
//    public float getPkts(Node entity) {
//        Metric metric = new Metric("pkts_total");
//        return getMetricValue(metric, entity);
//    }
//
//    public float getPktsIn(Node entity) {
//        Metric metric = new Metric("pkts_in");
//        return getMetricValue(metric, entity);
//    }
//
//    public float getPktsOut(Node entity) {
//        Metric metric = new Metric("pkts_out");
//        return getMetricValue(metric, entity);
//    }
//
//    public float getReadLatency(Node entity) {
//        Metric metric = new Metric("read_latency");
//        return getMetricValue(metric, entity);
//    }
//
//    public float getWriteLatency(Node entity) {
//        Metric metric = new Metric("write_latency");
//        return getMetricValue(metric, entity);
//    }
//
//    public float getReadCount(Node entity) {
//        Metric metric = new Metric("read_count");
//        return getMetricValue(metric, entity);
//    }
//
//    public float getCostPerHour(Node entity) {
//        Metric metric = new Metric("costPerHour");
//        return getMetricValue(metric, entity);
//    }
//
//    public float getWriteCount(Node entity) {
//        Metric metric = new Metric("write_count");
//        return getMetricValue(metric, entity);
//    }
//
//    //TODO: can;t be done currentlu
//    public float getTotalCostSoFar(Node entity) {
//        Metric metric = new Metric("costPerHour");
//        return getMetricValue(metric, entity);
//    }
//
//    /**
//     * @return currently, all metrics for the first VM that is monitored. While
//     * MELA can return metrics for different VMs belonging to different service
//     * units, it would require a Service_unit_id to be added as parameter to
//     * this call
//     */
//    public List<String> getAvailableMetrics() {
//        ServiceMonitoringSnapshot monitoringSnapshot = systemControl.getRawMonitoringData();
//        Map<ServiceElement, ServiceElementMonitoringSnapshot> monitoringData = monitoringSnapshot.getMonitoredData(ServiceElement.ServiceElementLevel.VM);
//
//        Collection<Metric> metrics = monitoringData.values().iterator().next().getMonitoredData().keySet();
//        List<String> strings = new ArrayList<String>();
//        for (Metric metric : metrics) {
//            strings.add(metric.getName());
//        }
//        return strings;
//    }
//
//    //should at least have an action name in name
    public void notifyControlActionStarted(String actionName, Node actionTargetEntity) {
        URL url = null;
        HttpURLConnection connection = null;
        try {
            url = new URL(REST_API_URL + "/executingaction");
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setRequestProperty("Accept", "application/xml");

            //write message body
            OutputStream os = connection.getOutputStream();

            String compositionRules = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<Action targetEntityID=\"" + actionTargetEntity.getId() + "\" action=\"" + actionName + "\" />";
            os.write(compositionRules.getBytes());
            os.flush();
            os.close();

            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    Logger.getLogger(MELA_API.class.getName()).log(Level.SEVERE, line);
                }
            }

            InputStream inputStream = connection.getInputStream();
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    Logger.getLogger(MELA_API.class.getName()).log(Level.SEVERE, line);
                }
            }

        } catch (Exception e) {
            Logger.getLogger(MELA_API.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

    }

    public void notifyControlActionEnded(String actionName, Node actionTargetEntity) {
        URL url = null;
        HttpURLConnection connection = null;
        try {
            url = new URL(REST_API_URL + "/executingaction");
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setRequestProperty("Accept", "application/xml");

            //write message body
            OutputStream os = connection.getOutputStream();

            String compositionRules = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<Action targetEntityID=\"" + actionTargetEntity.getId() + "\" action=\"" + actionName + "\" />";
            os.write(compositionRules.getBytes());
            os.flush();
            os.close();

            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    Logger.getLogger(MELA_API.class.getName()).log(Level.SEVERE, line);
                }
            }

            InputStream inputStream = connection.getInputStream();
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    Logger.getLogger(MELA_API.class.getName()).log(Level.SEVERE, line);
                }
            }

        } catch (Exception e) {
            Logger.getLogger(MELA_API.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public float getMetricValue(Metric metric, Node entity) {

        if (entity.getId() == null) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Supplied entity has no ID. Can't get metric value");
            return -1;
        }

        //get the Entity level so I can search it in the monitored snapshot easily (only in entity and its children)
        MonitoredElement.MonitoredElementLevel level = MELA_ClientUtils.getElementLevelFromEntity(entity);
        MonitoredElement element = new MonitoredElement();
        element.setId(entity.getId());
        element.setLevel(level);

        //search in the aggregated data over time for the target entity
        MonitoredElement elementSearchingFor = new MonitoredElement(entity.getId());
        List<MonitoredElementMonitoringSnapshot> processing = new ArrayList<MonitoredElementMonitoringSnapshot>();

        processing.add(latestMonitoringData);

        while (!processing.isEmpty()) {
            MonitoredElementMonitoringSnapshot currentlyUnderInspection = processing.remove(0);
            if (currentlyUnderInspection.getMonitoredElement().equals(elementSearchingFor)) {
                if (currentlyUnderInspection.containsMetric(metric)) {
                    MetricValue value = currentlyUnderInspection.getMetricValue(metric);
                    switch (value.getValueType()) {
                        case NUMERIC:
                            return Float.parseFloat(value.getValueRepresentation());
                        default:
                            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Value ''{0}''for metric {1} for Node {2} is not Numeric", new Object[]{value.getValueRepresentation(), metric.toString(), entity.getId()});
                            return -1;
                    }
                }
            } else {
                processing.addAll(currentlyUnderInspection.getChildren());
            }
        }

        //if we have reached this point, either the monitored element was not found, either the metric
        Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Metric ''{0}'' OR   Node {1} not found", new Object[]{metric.toString(), entity.getId()});
        return -1;
    }
//

    private static class MELA_ClientUtils {

        //works as side effect
        public static void convertServiceTopology(MonitoredElement serviceElement, Node cloudService) {
            //RuntimeLogger.logger.info("Related nodes for node "+ cloudService +" are "+ cloudService.getAllRelatedNodesOfType(RelationshipType.COMPOSITION_RELATIONSHIP));
            List<Node> serviceTopologies = new ArrayList<Node>();
            		
            		serviceTopologies.addAll(cloudService.getAllRelatedNodesOfType(RelationshipType.COMPOSITION_RELATIONSHIP));
            while (!serviceTopologies.isEmpty()){
                MonitoredElement serviceTopologyElement = new MonitoredElement();

            Node serviceTopology = serviceTopologies.get(0);
            serviceTopologyElement.setId(serviceTopology.getId());
            serviceTopologyElement.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);

            if (serviceTopology.getAllRelatedNodesOfType(RelationshipType.COMPOSITION_RELATIONSHIP) != null) {

                for (Node serviceUnit : serviceTopology.getAllRelatedNodesOfType(RelationshipType.COMPOSITION_RELATIONSHIP)) {
                    if (serviceUnit.getNodeType() == NodeType.SERVICE_UNIT) {
                        MonitoredElement serviceUnitElement = new MonitoredElement();
                        serviceUnitElement.setId(serviceUnit.getId());
                        serviceUnitElement.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
                        for (Node vm: serviceUnit.getAllRelatedNodesOfType(RelationshipType.HOSTED_ON_RELATIONSHIP,NodeType.VIRTUAL_MACHINE)){
                        	 MonitoredElement virtualMachine = new MonitoredElement();
                        	 virtualMachine.setId(vm.getId());
                        	 virtualMachine.setLevel(MonitoredElement.MonitoredElementLevel.VM);
                        	 serviceUnitElement.addElement(virtualMachine);
                        }
                        for (Node vm: serviceUnit.getAllRelatedNodesOfType(RelationshipType.ASSOCIATED_AT_RUNTIME_RELATIONSHIP,NodeType.VIRTUAL_MACHINE)){
                        	MonitoredElement virtualMachine = new MonitoredElement();
	                       	 virtualMachine.setId(vm.getId());
	                       	 boolean alreadyContained=false;
	                       	 virtualMachine.setLevel(MonitoredElement.MonitoredElementLevel.VM);
	                       	 for (MonitoredElement el:serviceUnitElement.getContainedElements())
	                       	 { 
	                       		 if (el.getId().equalsIgnoreCase(vm.getId())){
	                       			alreadyContained=true;
	                       		 }
	                       	 }
	                       	 if (!alreadyContained)
	                       		 serviceUnitElement.addElement(virtualMachine);
                        }
                        serviceTopologyElement.addElement(serviceUnitElement);

                    }
                }
            }

            serviceElement.addElement(serviceTopologyElement);

            if (serviceTopology.getAllRelatedNodesOfType(RelationshipType.COMPOSITION_RELATIONSHIP) != null) {
                for (Node subTopology : serviceTopology.getAllRelatedNodesOfType(RelationshipType.COMPOSITION_RELATIONSHIP)) {
                    if (subTopology.getNodeType() == NodeType.SERVICE_TOPOLOGY) {
                    	serviceTopologies.add(subTopology);
                    }
                }
            }
            serviceTopologies.remove(0);
            }
            
        }

        public static MonitoredElement.MonitoredElementLevel getElementLevelFromEntity(Node entity) {
            if (entity.getNodeType() == NodeType.CLOUD_SERVICE) {
                return MonitoredElement.MonitoredElementLevel.SERVICE;
            } else if (entity.getNodeType() == NodeType.SERVICE_TOPOLOGY) {
                return MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY;
            } else if (entity.getNodeType() == NodeType.SERVICE_UNIT) {
                return MonitoredElement.MonitoredElementLevel.SERVICE_UNIT;
            } else {
                Logger.getLogger(MELA_API.class.getName()).log(Level.SEVERE, "Error. Cannot determine the source class of entity " + entity);
                return null;
            }
        }
    }

    public static void main(String[] args) throws Exception {

       

    }
    
    public void submitElasticityRequirements(
            ArrayList<ElasticityRequirement> description) {
        //Requirements requirements = new Requirements();
      //  requirements.setRequirements(new ArrayList<Requirement>());


    }

    
    public float getNumberInstances(Node entity) {
        Metric metric = new Metric("vmCount");
        return getMetricValue(metric, entity);
    }
    public float getCpuUsage(Node entity) {
        Metric metric = new Metric("cpu_usage");
        return getMetricValue(metric, entity);
    }

    public float getMemoryAvailable(Node entity) {
        Metric metric = new Metric("mem_free_in_GB");
        return getMetricValue(metric, entity);
    }

    public float getMemorySize(Node entity) {
        Metric metric = new Metric("mem_total_in_GB");
        return getMetricValue(metric, entity);
    }

    public float getMemoryUsage(Node entity) {
        Metric metric = new Metric("mem_used");
        return getMetricValue(metric, entity);
    }

    public float getDiskSize(Node entity) {
        Metric metric = new Metric("disk_total");
        return getMetricValue(metric, entity);
    }

    public float getDiskAvailable(Node entity) {
        Metric metric = new Metric("disk_free");
        return getMetricValue(metric, entity);
    }

    //TODO: define agg rule in procentaj
    public float getDiskUsage(Node entity) {

        return (getDiskSize(entity) - getDiskAvailable(entity)) / getDiskSize(entity) * 100;
    }

    public float getCPUSpeed(Node entity) {

        Metric metric = new Metric("cpu_speed");
        return getMetricValue(metric, entity);
    }

    //TODO: define agg rule in TOTAL
    public float getPkts(Node entity) {
        Metric metric = new Metric("pkts_total");
        return getMetricValue(metric, entity);
    }

    public float getPktsIn(Node entity) {
        Metric metric = new Metric("pkts_in");
        return getMetricValue(metric, entity);
    }

    public float getPktsOut(Node entity) {
        Metric metric = new Metric("pkts_out");
        return getMetricValue(metric, entity);
    }

    public float getReadLatency(Node entity) {
        Metric metric = new Metric("read_latency");
        return getMetricValue(metric, entity);
    }

    public float getWriteLatency(Node entity) {
        Metric metric = new Metric("write_latency");
        return getMetricValue(metric, entity);
    }

    public float getReadCount(Node entity) {
        Metric metric = new Metric("read_count");
        return getMetricValue(metric, entity);
    }

    public float getCostPerHour(Node entity) {
        Metric metric = new Metric("costPerHour");
        return getMetricValue(metric, entity);
    }

    public float getWriteCount(Node entity) {
        Metric metric = new Metric("write_count");
        return getMetricValue(metric, entity);
    }

    //TODO: can;t be done currentlu
    public float getTotalCostSoFar(Node entity) {
        Metric metric = new Metric("costPerHour");
        return getMetricValue(metric, entity);
    }

    /**
     * @return currently, all metrics for the first VM that is monitored. While MELA can return metrics for different VMs belonging to different service units,
     *         it would require a Service_unit_id to be added as parameter to this call
     */
    public List<String> getAvailableMetrics() {
//        ServiceMonitoringSnapshot monitoringSnapshot = systemControl.getRawMonitoringData();
//        Map<ServiceElement, ServiceElementMonitoringSnapshot> monitoringData = monitoringSnapshot.getMonitoredData(ServiceElement.ServiceElementLevel.VM);
//
//        Collection<Metric> metrics = monitoringData.values().iterator().next().getMonitoredData().keySet();
        List<String> strings = new ArrayList<String>();
//        for (Metric metric : metrics) {
//            strings.add(metric.getName());
//        }
        return strings;
    }

  
  
    public float getMetricValue(String metricName, Node entity) {
        Metric metric = new Metric(metricName);
        
        return getMetricValue(metric, entity);
    }



   

	
}