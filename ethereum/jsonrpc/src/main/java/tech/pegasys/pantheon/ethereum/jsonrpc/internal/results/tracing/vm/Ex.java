/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.tracing.vm;

public class Ex {
  private Mem mem;
  private String[] push;
  private String store;
  private long used;

  public Ex(final Mem mem, final String[] push, final String store, final long used) {
    this.mem = mem;
    this.push = push;
    this.store = store;
    this.used = used;
  }

  public Mem getMem() {
    return mem;
  }

  public void setMem(final Mem mem) {
    this.mem = mem;
  }

  public String[] getPush() {
    return push;
  }

  public void setPush(final String[] push) {
    this.push = push;
  }

  public String getStore() {
    return store;
  }

  public void setStore(final String store) {
    this.store = store;
  }

  public long getUsed() {
    return used;
  }

  public void setUsed(final long used) {
    this.used = used;
  }
}
