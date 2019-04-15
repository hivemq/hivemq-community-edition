/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.bootstrap;

import java.io.Serializable;
import java.util.Comparator;

public class HiveMQVersionComparator implements Comparator<String>, Serializable {
    @Override
    public int compare(final String o1, final String o2) {

        final String[] split1 = o1.split("\\.");
        final String[] split2 = o2.split("\\.");

        if (split1.length != 3) {
            throw new IllegalArgumentException("Not able to parse HiveMQ version number [" + o1 + "]");
        }

        if (split2.length != 3) {
            throw new IllegalArgumentException("Not able to parse HiveMQ version number [" + o2 + "]");
        }

        final int compare0 = Integer.compare(Integer.parseInt(split1[0]), Integer.parseInt(split2[0]));
        if (compare0 != 0) {
            return compare0;
        }

        final int compare1 = Integer.compare(Integer.parseInt(split1[1]), Integer.parseInt(split2[1]));
        if (compare1 != 0) {
            return compare1;
        }

        return Integer.compare(Integer.parseInt(split1[2]), Integer.parseInt(split2[2]));
    }
}