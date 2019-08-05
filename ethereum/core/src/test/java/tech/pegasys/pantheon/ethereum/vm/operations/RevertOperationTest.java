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
package tech.pegasys.pantheon.ethereum.vm.operations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import tech.pegasys.pantheon.ethereum.mainnet.ConstantinopleGasCalculator;
import tech.pegasys.pantheon.ethereum.vm.MessageFrame;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;
import tech.pegasys.pantheon.util.uint.UInt256;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RevertOperationTest {

  @Mock private MessageFrame messageFrame;
  private final RevertOperation operation = new RevertOperation(new ConstantinopleGasCalculator());

  private final BytesValue revertReasonBytes =
      BytesValue.fromHexString("726576657274206d657373616765");

  @Before
  public void setUp() {
    when(messageFrame.popStackItem())
        .thenReturn(Bytes32.fromHexString("0x00"))
        .thenReturn(Bytes32.fromHexString("0x0e"));
    when(messageFrame.readMemory(UInt256.ZERO, UInt256.of(0x0e))).thenReturn(revertReasonBytes);
  }

  @Test
  public void shouldReturnReason() {
    ArgumentCaptor<BytesValue> arg = ArgumentCaptor.forClass(BytesValue.class);
    operation.execute(messageFrame);
    Mockito.verify(messageFrame).setRevertReason(arg.capture());
    assertEquals(revertReasonBytes, arg.getValue());
  }
}
