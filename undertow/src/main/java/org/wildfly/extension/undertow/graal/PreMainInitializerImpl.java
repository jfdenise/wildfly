 /*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow.graal;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.as.controller.graal.GraalRecorder;
import org.jboss.as.controller.graal.PreMainInitializer;

/**
 *
 * @author jdenise
 */
public class PreMainInitializerImpl implements PreMainInitializer {

    public static final String KEY = "undertow-ext";

    @Override
    public void init(Map<String, List<GraalRecorder.UnresolvedRecord>> map) throws Exception {
        //ServiceLoaderInitializer.init(resolve(map));
    }

    @Override
    public String getRecordingKey() {
        return KEY;
    }

    // Deserialization is done with the classloader that loaded the PremainInitializerImpl class.
    // So can't be done in the controller.
    private Map<String, List<GraalRecorder.Record>> resolve(Map<String, List<GraalRecorder.UnresolvedRecord>> records) throws Exception {
        Map<String, List<GraalRecorder.Record>> ret = new HashMap<>();
        for (String k : records.keySet()) {
            List<GraalRecorder.Record> lst = new ArrayList<>();
            ret.put(k, lst);
            List<GraalRecorder.UnresolvedRecord> recs = records.get(k);
            for (GraalRecorder.UnresolvedRecord rec : recs) {
                Path file = rec.file;
                try (FileInputStream streamIn = new FileInputStream(file.toFile())) {
                    try (ObjectInputStream objectIS = new ObjectInputStream(streamIn)) {
                        GraalRecorder.Record r = new GraalRecorder.Record();
                        r.id = rec.id;
                        r.content = objectIS.readObject();
                        lst.add(r);
                    }
                }
            }
        }

        return ret;
    }
}
