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
package one.cafebabe.samurai.tail;

import java.io.File;
import java.io.IOException;

public interface LogMonitor {
    void onLine(File file, String line, long filePointer);

    void logStarted(File file, long filePointer);

    void logWillEnd(File file, long filePointer);

    void logEnded(File file, long filePointer);

    void logContinued(File file, long filePointer);

    void onException(File file, IOException ioe);
}
