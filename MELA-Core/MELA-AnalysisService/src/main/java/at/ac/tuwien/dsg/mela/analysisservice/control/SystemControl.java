/**
 * Copyright 2013 Technische Universitat Wien (TUW), Distributed Systems Group
 * E184
 *
 * This work was partially supported by the European Commission in terms of the
 * CELAR FP7 project (FP7-ICT-2011-8 \#317790)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package at.ac.tuwien.dsg.mela.analysisservice.control;

import at.ac.tuwien.dsg.mela.common.requirements.MetricFilter;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.mela.analysisservice.concepts.ElasticitySpace;
import at.ac.tuwien.dsg.mela.analysisservice.concepts.ElasticitySpaceFunction;
import at.ac.tuwien.dsg.mela.analysisservice.concepts.impl.ElSpaceDefaultFunction;
import at.ac.tuwien.dsg.mela.analysisservice.concepts.impl.defaultElPthwFunction.EncounterRateElasticityPathway;
import at.ac.tuwien.dsg.mela.analysisservice.concepts.impl.defaultElSgnFunction.som.entities.Neuron;
import at.ac.tuwien.dsg.mela.analysisservice.engines.InstantMonitoringDataAnalysisEngine;
import at.ac.tuwien.dsg.mela.analysisservice.engines.DataAggregationEngine;
import at.ac.tuwien.dsg.mela.analysisservice.report.AnalysisReport;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.AbstractDataAccess;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.DataAccess;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionOperation;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRule;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.analysisservice.utils.Configuration;
import at.ac.tuwien.dsg.mela.analysisservice.utils.exceptions.ConfigurationException;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.GangliaFileDataSource;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.RemoteGangliaLiveDataSource;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.GangliaSQLDataSource;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 * Delegates the functionality of configuring MELA for instant monitoring and
 * analysis
 */
public class SystemControl {

    private AbstractDataAccess dataAccess;
    private Requirements requirements;
    private CompositionRulesConfiguration compositionRulesConfiguration;
    private MonitoredElement serviceConfiguration;
    private DataAggregationEngine instantMonitoringDataEnrichmentEngine;
    private InstantMonitoringDataAnalysisEngine instantMonitoringDataAnalysisEngine;
    //used for data Aggregation over time
    private List<ServiceMonitoringSnapshot> historicalMonitoringData;
    //used if somewhone wants freshest data
    private ServiceMonitoringSnapshot latestMonitoringData;
    //interval at which RAW monitoring data is collected
    private int monitoringIntervalInSeconds = Configuration.getMonitoringIntervalInSeconds();
    //interval over which raw monitoring data is aggregated.
    //example: for monitoringIntervalInSeconds at 5 seconds, and aggregation at 30, 
    //means 6 monitoring snapshots are aggregated into 1
    private int aggregationWindowsCount = Configuration.getMonitoringAggregationIntervalInSeconds();
    private Timer monitoringTimer;
    //used in determining the service elasticity space
    private ElasticitySpaceFunction elasticitySpaceFunction;
    //temproary. Should support more functions in the future
    private EncounterRateElasticityPathway elasticityPathway;
    //holding MonitoredElement name, and Actions Name
    private Map<MonitoredElement, String> actionsInExecution;
    private Boolean isElasticityEnabled = Configuration.isElasticityAnalysisEnabled();
    //used in monitoring 
    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
        }
    };
    private SystemControl selfReference;

    protected SystemControl() {
        instantMonitoringDataEnrichmentEngine = new DataAggregationEngine();
        instantMonitoringDataAnalysisEngine = new InstantMonitoringDataAnalysisEngine();
        monitoringTimer = new Timer();

        dataAccess = DataAccess.createInstance();

        latestMonitoringData = new ServiceMonitoringSnapshot();
        historicalMonitoringData = new ArrayList<ServiceMonitoringSnapshot>();
        elasticityPathway = new EncounterRateElasticityPathway();
        selfReference = this;
        actionsInExecution = new ConcurrentHashMap<MonitoredElement, String>();
        startMonitoring();

        if ((int) (monitoringIntervalInSeconds / aggregationWindowsCount) == 0) {
            aggregationWindowsCount = 1 * monitoringIntervalInSeconds;
        }
    }

    public MonitoredElement getServiceConfiguration() {
        return serviceConfiguration;
    }

    public synchronized void addExecutingAction(String targetEntityID, String actionName) {
        MonitoredElement element = new MonitoredElement(targetEntityID);
        actionsInExecution.put(element, actionName);
    }

    public synchronized void removeExecutingAction(String targetEntityID, String actionName) {
        MonitoredElement element = new MonitoredElement(targetEntityID);
        if (actionsInExecution.containsKey(element)) {
            actionsInExecution.remove(element);
        } else {
            Configuration.getLogger().log(Level.INFO, "Action " + actionName + " on monitored element " + targetEntityID + " not found.");
        }
    }

    public Map<MonitoredElement, String> getActionsInExecution() {
        return actionsInExecution;
    }

    public synchronized void setServiceConfiguration(MonitoredElement serviceConfiguration) {
        this.serviceConfiguration = serviceConfiguration;
        elasticitySpaceFunction = new ElSpaceDefaultFunction(serviceConfiguration);
    }

    //actually removes all VMs and Virtual Clusters from the ServiceUnit and adds new ones.
    public synchronized void updateServiceConfiguration(MonitoredElement serviceConfiguration) {
        //extract all ServiceUnit level monitored elements from both services, and replace their children  
        Map<MonitoredElement, MonitoredElement> serviceUnits = new HashMap<MonitoredElement, MonitoredElement>();
        for (MonitoredElement element : this.serviceConfiguration) {
            if (element.getLevel().equals(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)) {
                //remove element's children
                element.getContainedElements().clear();
                serviceUnits.put(element, element);
            }
        }

        //go trough the new service, and for each Service Unit, add its children (containing both Virtual Machines and Virtual Clusters) to the original service
        for (MonitoredElement element : serviceConfiguration) {
            if (serviceUnits.containsKey(element)) {
                //bad practice. breaks incapsulation
                serviceUnits.get(element).getContainedElements().addAll(element.getContainedElements());
            }
        }

    }

    public List<Neuron> getElPathwayGroups(Map<Metric, List<MetricValue>> map) {
        if (elasticitySpaceFunction != null && map != null) {
            return elasticityPathway.getSituationGroups(map);
        } else {
            return new ArrayList<Neuron>();
        }
    }

    public synchronized Requirements getRequirements() {
        return requirements;
    }

    public synchronized void setRequirements(Requirements requirements) {
        this.requirements = requirements;
        elasticitySpaceFunction.setRequirements(requirements);
    }

    public synchronized CompositionRulesConfiguration getCompositionRulesConfiguration() {
        return compositionRulesConfiguration;
    }

    public synchronized void setCompositionRulesConfiguration(CompositionRulesConfiguration compositionRulesConfiguration) {
        if (dataAccess != null) {
            dataAccess.getMetricFilters().clear();
            //add data access metric filters for the source of each composition rule
            for (CompositionRule compositionRule : compositionRulesConfiguration.getMetricCompositionRules().getCompositionRules()) {
                //go trough each CompositionOperation and extract the source metrics

                List<CompositionOperation> queue = new ArrayList<CompositionOperation>();
                queue.add(compositionRule.getOperation());

                while (!queue.isEmpty()) {
                    CompositionOperation operation = queue.remove(0);
                    queue.addAll(operation.getSubOperations());

                    Metric targetMetric = operation.getTargetMetric();
                    //metric can be null if a composition rule artificially creates a metric using SET_VALUE
                    if (targetMetric != null) {
                        MetricFilter metricFilter = new MetricFilter();
                        metricFilter.setId(targetMetric.getName() + "_Filter");
                        metricFilter.setLevel(operation.getMetricSourceMonitoredElementLevel());
                        Collection<Metric> metrics = new ArrayList<Metric>();
                        metrics.add(new Metric(targetMetric.getName()));
                        metricFilter.setMetrics(metrics);
                        dataAccess.addMetricFilter(metricFilter);
                    }
                }
            }
            this.compositionRulesConfiguration = compositionRulesConfiguration;
        } else {
            Configuration.getLogger().log(Level.WARN, "Data Access source not set yet on SystemControl."
                    + "Metric filters to get metrics targeted by composition rules will not be added");
            this.compositionRulesConfiguration = compositionRulesConfiguration;
        }

    }

    public synchronized AbstractDataAccess getDataAccess() {
        return dataAccess;
    }

    public synchronized void setDataAccess(AbstractDataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public synchronized ServiceMonitoringSnapshot getRawMonitoringData() {
        if (dataAccess != null) {
            return dataAccess.getMonitoredData(serviceConfiguration);
        } else {
            Configuration.getLogger().log(Level.WARN, "Data Access source not set yet on SystemControl");
            return new ServiceMonitoringSnapshot();
        }
    }

    public synchronized ServiceMonitoringSnapshot getAggregatedMonitoringDataOverTime(List<ServiceMonitoringSnapshot> serviceMonitoringSnapshots) {
        return instantMonitoringDataEnrichmentEngine.aggregateMonitoringDataOverTime(compositionRulesConfiguration, serviceMonitoringSnapshots);
    }

    private synchronized ServiceMonitoringSnapshot getAggregatedMonitoringData(ServiceMonitoringSnapshot rawMonitoringData) {
        if (dataAccess != null) {
            return instantMonitoringDataEnrichmentEngine.enrichMonitoringData(compositionRulesConfiguration, rawMonitoringData);
        } else {
            Configuration.getLogger().log(Level.WARN, "Data Access source not set yet on SystemControl");
            return new ServiceMonitoringSnapshot();
        }
    }

    private synchronized AnalysisReport analyzeAggregatedMonitoringData(ServiceMonitoringSnapshot rawMonitoringData) {
        if (dataAccess != null) {
            return instantMonitoringDataAnalysisEngine.analyzeRequirements(instantMonitoringDataEnrichmentEngine.enrichMonitoringData(compositionRulesConfiguration, rawMonitoringData), requirements);
        } else {
            Configuration.getLogger().log(Level.WARN, "Data Access source not set yet on SystemControl");
            return new AnalysisReport(new ServiceMonitoringSnapshot(), new Requirements());
        }
    }

    public synchronized AnalysisReport analyzeLatestMonitoringData() {
        if (dataAccess != null) {
            return instantMonitoringDataAnalysisEngine.analyzeRequirements(instantMonitoringDataEnrichmentEngine.enrichMonitoringData(compositionRulesConfiguration, latestMonitoringData), requirements);
        } else {
            Configuration.getLogger().log(Level.WARN, "Data Access source not set yet on SystemControl");
            return new AnalysisReport(new ServiceMonitoringSnapshot(), new Requirements());
        }
    }

    public synchronized Collection<Metric> getAvailableMetricsForMonitoredElement(MonitoredElement MonitoredElement) throws ConfigurationException {
        if (dataAccess != null) {
            return dataAccess.getAvailableMetricsForMonitoredElement(MonitoredElement);
        } else {
            Configuration.getLogger().log(Level.WARN, "Data Access source not set yet on SystemControl");
            return new ArrayList<Metric>();
        }
    }

    public synchronized void addMetricFilter(MetricFilter metricFilter) {
        if (dataAccess != null) {
            dataAccess.addMetricFilter(metricFilter);
        } else {
            Configuration.getLogger().log(Level.WARN, "Data Access source not set yet on SystemControl");
        }
    }

    public synchronized void addMetricFilters(Collection<MetricFilter> newFilters) {
        if (dataAccess != null) {
            dataAccess.addMetricFilters(newFilters);
        } else {
            Configuration.getLogger().log(Level.WARN, "Data Access source not set yet on SystemControl");
        }
    }

    public synchronized void removeMetricFilter(MetricFilter metricFilter) {
        if (dataAccess != null) {
            dataAccess.removeMetricFilter(metricFilter);
        } else {
            Configuration.getLogger().log(Level.WARN, "Data Access source not set yet on SystemControl");
        }
    }

    public synchronized void removeMetricFilters(Collection<MetricFilter> filtersToRemove) {
        if (dataAccess != null) {
            dataAccess.removeMetricFilters(filtersToRemove);
        } else {
            Configuration.getLogger().log(Level.WARN, "Data Access source not set yet on SystemControl");
        }
    }

    public synchronized void setMonitoringIntervalInSeconds(int monitoringIntervalInSeconds) {
        this.monitoringIntervalInSeconds = monitoringIntervalInSeconds;
    }

    public synchronized void setNrOfMonitoringWindowsToAggregate(int aggregationIntervalInSeconds) {
        this.aggregationWindowsCount = aggregationIntervalInSeconds;
    }

    public ServiceMonitoringSnapshot getLatestMonitoringData() {
        return latestMonitoringData;
    }

    public synchronized void startMonitoring() {

        task = new TimerTask() {
            @Override
            public void run() {
                if (serviceConfiguration != null) {
                    ServiceMonitoringSnapshot monitoringData = selfReference.getRawMonitoringData();
                    if (monitoringData != null) {
                        historicalMonitoringData.add(monitoringData);
                        //remove the oldest and add the new value always
                        if (historicalMonitoringData.size() > aggregationWindowsCount / monitoringIntervalInSeconds) {
                            historicalMonitoringData.remove(0);
                        }

                        latestMonitoringData = selfReference.getAggregatedMonitoringDataOverTime(historicalMonitoringData);

                        //if we have no composition function, we have no metrics, so it does not make sense to train the elasticity space
                        if (isElasticityEnabled && elasticitySpaceFunction != null && compositionRulesConfiguration != null) {
                            elasticitySpaceFunction.trainElasticitySpace(latestMonitoringData);
                        }
                    } else {
                        //stop the monitoring if the data replay is done
//                        this.cancel();
                        Configuration.getLogger().log(Level.ERROR, "Monitoring data is NULL");
                    }
                } else {
                    Configuration.getLogger().log(Level.WARN, "No service configuration");
                }
            }
        };
        //repeat the monitoring every monitoringIntervalInSeconds seconds 
        monitoringTimer.schedule(task, 0, monitoringIntervalInSeconds * 1000);
//        monitoringTimer.schedule(task, 0,1);
    }

    public ElasticitySpace getElasticitySpace() {
        if (elasticitySpaceFunction != null) {
            return elasticitySpaceFunction.getElasticitySpace();
        } else {
            Configuration.getLogger().log(Level.WARN, "No elasticity space");
            return new ElasticitySpace(new MonitoredElement());
        }
    }

    public synchronized void stopMonitoring() {
        task.cancel();
    }
}
