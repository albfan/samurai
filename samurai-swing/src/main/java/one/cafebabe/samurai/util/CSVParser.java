/*
 * Copyright 2003-2012 Yusuke Yamamoto
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package one.cafebabe.samurai.util;

import one.cafebabe.samurai.gc.LineGraph;
import one.cafebabe.samurai.gc.LineGraphRenderer;

import java.util.ArrayList;
import java.util.List;

public class CSVParser implements LineGraphDataSourceParser {
    public CSVParser() {
    }

    private String[] labels = null;
    private List<LineGraph> lineGraphs = null;

    public boolean parse(String line, LineGraphRenderer renderer) {
        if (null == lineGraphs) {
            labels = line.split(",");
            lineGraphs = new ArrayList<>(labels.length);
            for (String label : labels) {
                lineGraphs.add(renderer.addLineGraph(label, new String[]{label}));
            }
        } else {
            String[] splitted = line.split(",");
            double[] dataArray = new double[labels.length];
            for (int i = 0; i < labels.length; i++) {
                try {
                    dataArray[i] = Double.parseDouble(splitted[i]);
                } catch (NumberFormatException nfe) {
                    dataArray[i] = splitted[i].hashCode();
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    dataArray[i] = 0d;
                }
            }
            for (int i = 0; i < labels.length; i++) {
                lineGraphs.get(i).addValues(new double[]{dataArray[i]});
            }
        }
        return true;
    }
}
