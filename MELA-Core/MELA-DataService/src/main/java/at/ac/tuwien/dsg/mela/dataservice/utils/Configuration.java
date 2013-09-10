/**
 * Copyright 2013 Technische Universitat Wien (TUW), Distributed Systems Group
 * E184
 *
 * This work was partially supported by the European Commission in terms of the
 * CELAR FP7 project (FP7-ICT-2011-8 \#317790)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. 
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
package at.ac.tuwien.dsg.mela.dataservice.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 * Class which acts as entry point towards all configuration options that need
 * to be added to the MELA-DataService
 */
public class Configuration {

    private static Properties configuration;
    static Logger logger;

    static {
        configuration = new Properties();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
//            ClassLoader classLoader = Configuration.class.getClassLoader();
            InputStream propertiesStream = classLoader.getResourceAsStream("/config/Config.properties");
            
            //try with file
            if(propertiesStream == null){
               propertiesStream = new FileInputStream(new File("./config/Config.properties")); 
            }

            configuration.load(propertiesStream);
            
            InputStream log4jStream = classLoader.getResourceAsStream("/config/Log4j.properties");
             if(log4jStream == null){
               log4jStream = new FileInputStream(new File("./config/Log4j.properties")); 
            }

            
            if (log4jStream != null) {
                PropertyConfigurator.configure(log4jStream);
                String date = new Date().toString();
                date = date.replace(" ", "_");
                date = date.replace(":", "_");
                System.getProperties().put("recording_date", date);

                logger = Logger.getLogger("rootLogger");
            } else {
               logger = Logger.getLogger("rootLogger");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            logger = Logger.getLogger("rootLogger");
        }

    }

    public static String getSecurityCertificatePath() {
        return configuration.getProperty("PEM_CERT_PATH");
    }

    public static String getGangliaPort() {
        return configuration.getProperty("GANGLIA_PORT");
    }

    public static String getAccessMachineIP() {
        return configuration.getProperty("ACCESS_MACHINE_IP");
    }

    public static String getMonitoredElementIDMetricName() {
        return configuration.getProperty("SERVICE_ELEMENT_ID_METRIC_NAME");
    }

    public static Logger getLogger() {
        return logger;
    }

    public static String getAccessUserName() {
        return configuration.getProperty("ACCESS_MACHINE_USER_NAME");
    }

    public static String getMonitoringDataAccessMethod() {
        return configuration.getProperty("MONITORING_DATA_ACCESS");
    }

    public static String getStoredMonitoringSequenceID() {
        return configuration.getProperty("MONITORING_SEQ_ID");
    }
}
