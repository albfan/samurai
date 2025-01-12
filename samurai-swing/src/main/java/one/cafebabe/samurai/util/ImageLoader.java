/*
 * Copyright 2003-2021 Yusuke Yamamoto
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

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ImageLoader {
    private static final Map<String, ImageIcon> iconCache = new HashMap<>();

    public static ImageIcon get(String resourcePath) {
        return get(resourcePath, 16, 16);
    }

    public static ImageIcon get(String resourcePath, int width, int height) {
        //noinspection ConstantConditions
        return iconCache.computeIfAbsent(resourcePath, e -> new ImageIcon(new ImageIcon(
                ImageLoader.class.getResource(e))
                .getImage()
                .getScaledInstance(width, height, Image.SCALE_SMOOTH)));
    }
}
