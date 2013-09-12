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

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;

/**
 *
 * @Author Daniel Moldovan
 * @E-mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
public class JCatascopiaMetric {

    private String id;
    private String name;
    private String unit;
    private String group;
    private String value;
    private String type;

    public JCatascopiaMetric() {
    }

    public JCatascopiaMetric(String id, String name, String unit, String group, String value, String type) {
        this.id = id;
        this.name = name;
        this.unit = unit;
        this.group = group;
        this.value = value;
        this.type = type;
    }
        

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    

    public Object getConvertedValue() {
        if (type.contains("float") || type.contains("double")) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException e) {
                return new Float(Float.NaN);
            }
        } else if (type.contains("int")) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return new Float(Float.NaN);
            }
        } else {
            return value;
        }
    }
    
}
