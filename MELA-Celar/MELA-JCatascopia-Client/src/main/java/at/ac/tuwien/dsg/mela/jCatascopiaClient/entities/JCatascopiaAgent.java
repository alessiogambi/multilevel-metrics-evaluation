/**
 * Copyright 2013 Technische Universitaet Wien (TUW), Distributed Systems Group
 * E184
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
package at.ac.tuwien.dsg.mela.jCatascopiaClient.entities;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @Author Daniel Moldovan
 * @E-mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
public class JCatascopiaAgent {

    private String id;
    private String ip;
    private String status;
    
    private List<JCatascopiaMetric> agentMetrics;

    {
        agentMetrics = new ArrayList<JCatascopiaMetric>();
    }
    
    public JCatascopiaAgent() {
        agentMetrics = new ArrayList<JCatascopiaMetric>();
    }

    public JCatascopiaAgent(String id, String ip, String status) {
        this.id = id;
        this.ip = ip;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
