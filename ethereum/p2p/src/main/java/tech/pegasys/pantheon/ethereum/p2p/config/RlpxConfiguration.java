/*
 * Copyright 2018 ConsenSys AG.
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
package tech.pegasys.pantheon.ethereum.p2p.config;

import static com.google.common.base.Preconditions.checkState;

import tech.pegasys.pantheon.ethereum.p2p.rlpx.wire.SubProtocol;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class RlpxConfiguration {
  public static final Double DEFAULT_FRACTION_REMOTE_CONNECTIONS_ALLOWED = 0.5;
  private String clientId = "TestClient/1.0.0";
  private String bindHost = "0.0.0.0";
  private int bindPort = 30303;
  private int maxPeers = 25;
  private boolean limitRemoteWireConnectionsEnabled = false;
  private double fractionRemoteWireConnectionsAllowed = DEFAULT_FRACTION_REMOTE_CONNECTIONS_ALLOWED;
  private List<SubProtocol> supportedProtocols = Collections.emptyList();

  public static RlpxConfiguration create() {
    return new RlpxConfiguration();
  }

  public RlpxConfiguration setSupportedProtocols(final SubProtocol... supportedProtocols) {
    this.supportedProtocols = Arrays.asList(supportedProtocols);
    return this;
  }

  public RlpxConfiguration setSupportedProtocols(final List<SubProtocol> supportedProtocols) {
    this.supportedProtocols = supportedProtocols;
    return this;
  }

  public List<SubProtocol> getSupportedProtocols() {
    return supportedProtocols;
  }

  public String getBindHost() {
    return bindHost;
  }

  public RlpxConfiguration setBindHost(final String bindHost) {
    this.bindHost = bindHost;
    return this;
  }

  public int getBindPort() {
    return bindPort;
  }

  public RlpxConfiguration setBindPort(final int bindPort) {
    this.bindPort = bindPort;
    return this;
  }

  public RlpxConfiguration setMaxPeers(final int peers) {
    maxPeers = peers;
    return this;
  }

  public int getMaxPeers() {
    return maxPeers;
  }

  public String getClientId() {
    return clientId;
  }

  public RlpxConfiguration setClientId(final String clientId) {
    this.clientId = clientId;
    return this;
  }

  public boolean isLimitRemoteWireConnectionsEnabled() {
    return limitRemoteWireConnectionsEnabled;
  }

  public RlpxConfiguration setLimitRemoteWireConnectionsEnabled(
      final boolean limitRemoteWireConnectionsEnabled) {
    this.limitRemoteWireConnectionsEnabled = limitRemoteWireConnectionsEnabled;
    return this;
  }

  public double getFractionRemoteWireConnectionsAllowed() {
    return fractionRemoteWireConnectionsAllowed;
  }

  public RlpxConfiguration setFractionRemoteWireConnectionsAllowed(
      final double fractionRemoteWireConnectionsAllowed) {
    checkState(
        fractionRemoteWireConnectionsAllowed > 0.0,
        "Fraction of remote connections allowed must be positive.");
    this.fractionRemoteWireConnectionsAllowed = fractionRemoteWireConnectionsAllowed;
    return this;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final RlpxConfiguration that = (RlpxConfiguration) o;
    return bindPort == that.bindPort && Objects.equals(bindHost, that.bindHost);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bindHost, bindPort);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("RlpxConfiguration{");
    sb.append("bindHost='").append(bindHost).append('\'');
    sb.append(", bindPort=").append(bindPort);
    sb.append('}');
    return sb.toString();
  }
}
