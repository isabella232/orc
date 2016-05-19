/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.orc.mapred;

import org.apache.hadoop.io.Writable;
import org.apache.orc.TypeDescription;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * A TreeMap implementation that implements Writable.
 * @param <K> the key type, which must be Writable
 * @param <V> the value type, which must be Writable
 */
public final class OrcMap<K extends Writable, V extends Writable>
    extends TreeMap<K, V> implements Writable {
  private final TypeDescription keySchema;
  private final TypeDescription valueSchema;

  public OrcMap(TypeDescription schema) {
    keySchema = schema.getChildren().get(0);
    valueSchema = schema.getChildren().get(1);
  }

  @Override
  public void write(DataOutput output) throws IOException {
    Iterator<Map.Entry<K,V>> itr = entrySet().iterator();
    output.writeInt(size());
    for(Map.Entry<K,V> entry: entrySet()) {
      K key = entry.getKey();
      V value = entry.getValue();
      output.writeByte((key == null ? 0 : 2) | (value == null ? 0 : 1));
      if (key != null) {
        key.write(output);
      }
      if (value != null) {
        value.write(output);
      }
    }
  }

  @Override
  public void readFields(DataInput input) throws IOException {
    clear();
    int size = input.readInt();
    for(int i=0; i < size; ++i) {
      byte flag = input.readByte();
      K key;
      V value;
      if ((flag & 2) != 0) {
        key = (K) OrcStruct.createValue(keySchema);
        key.readFields(input);
      } else {
        key = null;
      }
      if ((flag & 1) != 0) {
        value = (V) OrcStruct.createValue(valueSchema);
        value.readFields(input);
      } else {
        value = null;
      }
      put(key, value);
    }
  }
}