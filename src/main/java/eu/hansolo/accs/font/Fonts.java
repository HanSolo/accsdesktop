/*
 * Copyright (c) 2016 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.accs.font;

import javafx.scene.text.Font;


/**
 * User: hansolo
 * Date: 13.03.14
 * Time: 15:01
 */
public final class Fonts {
    private static final String LATO_LIGHT_NAME;

    private static String latoLightName;

    static {
        try {
            latoLightName = Font.loadFont(Fonts.class.getResourceAsStream("Lato-Lig.ttf"), 10).getName();
        } catch (Exception exception) { }
        LATO_LIGHT_NAME = latoLightName;
    }


    // ******************** Methods *******************************************
    public static Font latoLight(final double SIZE) {
        return new Font(LATO_LIGHT_NAME, SIZE);
    }
}