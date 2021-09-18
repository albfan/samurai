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
package one.cafebabe.samurai.swing;

import java.awt.Color;

public interface GraphCanvas {
    void drawLine(int x1, int y1, int x2, int y2);

    void fillRect(int x1, int y1, int x2, int y2);

    void setColor(Color color);

    void drawString(String str, int x, int y);

    int getFontHeight();

    int getStringWidth(String str);

}
