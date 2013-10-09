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

package at.ac.tuwien.dsg.mela.analysisservice.utils.converters;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.jaxbEntities.ClusterInfo;
import at.ac.tuwien.dsg.mela.dataservice.MonDataSQLWriteAccess;
import java.io.File;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;


/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 **/
public class WriteMonDataToSQL {

    public static void main(String[] args) throws SQLException {
//        String monitoringFile = "/home/OGandaniel-tuwien/Documents/DSG_SVN/software/tmp_prototypes/MELA_MAVEN/src/main/resources/config/monitoringSat_Jun_29_19_14_46_CEST_2013";
        String monitoringFile = "/home/daniel-tuwien/Documents/CELAR_GIT/multilevel-metrics-evaluation/MELA-Core/MELA-AnalysisService/monitoringSat_Jun_29_20_01_04_CEST_2013";
         
        Map<String, ClusterInfo> temp = new LinkedHashMap<String, ClusterInfo>();

        

        try {
            Yaml yaml = new Yaml();
            Iterable<Object> iterable = yaml.loadAll(new FileInputStream(new File(monitoringFile)));

            for (Object o : iterable) {
                ClusterInfo gangliaClusterInfo = (ClusterInfo) o;
                if (!temp.containsKey(gangliaClusterInfo.getLocaltime())) {
                    temp.put(gangliaClusterInfo.getLocaltime(), gangliaClusterInfo);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        MonDataSQLWriteAccess access = new MonDataSQLWriteAccess("mela","mela");
        for (ClusterInfo gangliaClusterInfo : temp.values()) {
            access.writeMonitoringData(gangliaClusterInfo);
        }
        access.closeConnection();

    }
}
