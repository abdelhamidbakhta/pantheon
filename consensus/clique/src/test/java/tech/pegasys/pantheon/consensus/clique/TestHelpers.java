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
package tech.pegasys.pantheon.consensus.clique;

import tech.pegasys.pantheon.crypto.SECP256K1;
import tech.pegasys.pantheon.crypto.SECP256K1.KeyPair;
import tech.pegasys.pantheon.crypto.SECP256K1.Signature;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.BlockHeaderTestFixture;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.util.List;

public class TestHelpers {

  public static BlockHeader createCliqueSignedBlockHeader(
      final BlockHeaderTestFixture blockHeaderBuilder,
      final KeyPair signer,
      final List<Address> validators) {

    final BlockHeader unsealedHeader =
        blockHeaderBuilder
            .blockHeaderFunctions(new CliqueBlockHeaderFunctions())
            .extraData(CliqueExtraData.encodeUnsealed(BytesValue.wrap(new byte[32]), validators))
            .buildHeader();
    final CliqueExtraData unsignedExtraData = CliqueExtraData.decodeRaw(unsealedHeader);

    final Hash signingHash =
        CliqueBlockHashing.calculateDataHashForProposerSeal(unsealedHeader, unsignedExtraData);

    final Signature proposerSignature = SECP256K1.sign(signingHash, signer);

    final BytesValue signedExtraData =
        new CliqueExtraData(
                unsignedExtraData.getVanityData(),
                proposerSignature,
                unsignedExtraData.getValidators(),
                unsealedHeader)
            .encode();

    blockHeaderBuilder.extraData(signedExtraData);

    return blockHeaderBuilder.buildHeader();
  }
}
